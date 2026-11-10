package com.umes.pedidos.client;

import com.sun.net.httpserver.HttpServer;
import com.umes.pedidos.config.ProductosClientConfig;
import com.umes.pedidos.dto.ProductoDto;
import com.umes.pedidos.exception.ProductosTiempoAgotadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Usa la fábrica HTTP real del servicio contra un servidor local de prueba,
 * para comprobar el método PATCH y el tiempo máximo de espera.
 */
class ProductosClientRealTest {

	private static final long TIEMPO_MAXIMO_MS = 300;

	private HttpServer servidor;
	private ProductosClient cliente;
	private final AtomicReference<String> metodoRecibido = new AtomicReference<>();
	private final AtomicReference<String> claveRecibida = new AtomicReference<>();

	@BeforeEach
	void iniciar() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/api/productos/1/descontar", intercambio -> {
			metodoRecibido.set(intercambio.getRequestMethod());
			claveRecibida.set(intercambio.getRequestHeaders().getFirst("X-Service-Key"));
			responder(intercambio, "{\"id\":1,\"nombre\":\"Teclado\",\"precio\":150,\"existencia\":3}");
		});
		servidor.createContext("/api/productos/2", intercambio -> {
			try {
				Thread.sleep(TIEMPO_MAXIMO_MS * 4);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			responder(intercambio, "{}");
		});
		servidor.start();

		String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
		RestClient.Builder builder = RestClient.builder()
				.requestFactory(ProductosClientConfig.fabricaConTiempoMaximo(TIEMPO_MAXIMO_MS));
		cliente = new ProductosClient(ProductosClientConfig.configurar(builder, url, "clave-test").build());
	}

	@AfterEach
	void detener() {
		servidor.stop(0);
	}

	private static void responder(com.sun.net.httpserver.HttpExchange intercambio, String json) throws IOException {
		byte[] cuerpo = json.getBytes(StandardCharsets.UTF_8);
		intercambio.getResponseHeaders().add("Content-Type", "application/json");
		intercambio.sendResponseHeaders(200, cuerpo.length);
		try (OutputStream salida = intercambio.getResponseBody()) {
			salida.write(cuerpo);
		}
	}

	@Test
	void enviaElDescuentoConPatch() {
		ProductoDto producto = cliente.descontarExistencia(1, 2);

		assertThat(metodoRecibido.get()).isEqualTo("PATCH");
		assertThat(claveRecibida.get()).isEqualTo("clave-test");
		assertThat(producto.existencia()).isEqualTo(3);
	}

	@Test
	void cortaLaEsperaCuandoProductosTardaDemasiado() {
		long inicio = System.nanoTime();

		assertThatThrownBy(() -> cliente.obtenerProducto(2)).isInstanceOf(ProductosTiempoAgotadoException.class);

		long transcurridoMs = (System.nanoTime() - inicio) / 1_000_000;
		assertThat(transcurridoMs).isLessThan(TIEMPO_MAXIMO_MS * 3);
	}
}
