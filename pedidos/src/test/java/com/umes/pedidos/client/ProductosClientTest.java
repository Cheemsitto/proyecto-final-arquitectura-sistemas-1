package com.umes.pedidos.client;

import com.umes.pedidos.config.ProductosClientConfig;
import com.umes.pedidos.dto.ProductoDto;
import com.umes.pedidos.exception.ProductoNoEncontradoException;
import com.umes.pedidos.exception.ProductosAutenticacionException;
import com.umes.pedidos.exception.ProductosNoDisponibleException;
import com.umes.pedidos.exception.ProductosTiempoAgotadoException;
import com.umes.pedidos.exception.StockInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Pruebas del cliente HTTP con un servidor de productos simulado (MockRestServiceServer). */
class ProductosClientTest {

	private static final String URL = "http://productos.test";
	private static final String PRODUCTO_JSON = """
			{"id":1,"nombre":"Teclado","descripcion":null,"precio":150,"existencia":5,
			 "creadoEn":"2026-11-10T00:00:00.000Z","actualizadoEn":"2026-11-10T00:00:00.000Z"}
			""";

	private MockRestServiceServer servidor;
	private ProductosClient cliente;

	@BeforeEach
	void preparar() {
		RestClient.Builder builder = ProductosClientConfig.configurar(RestClient.builder(), URL, "clave-test");
		servidor = MockRestServiceServer.bindTo(builder).build();
		cliente = new ProductosClient(builder.build());
	}

	@Test
	void consultaElProductoEnviandoLaClaveDeServicio() {
		servidor.expect(requestTo(URL + "/api/productos/1"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Service-Key", "clave-test"))
				.andRespond(withSuccess(PRODUCTO_JSON, MediaType.APPLICATION_JSON));

		ProductoDto producto = cliente.obtenerProducto(1);

		assertThat(producto.nombre()).isEqualTo("Teclado");
		assertThat(producto.existencia()).isEqualTo(5);
		assertThat(producto.precio()).isEqualByComparingTo("150");
		servidor.verify();
	}

	@Test
	void descuentaEnviandoLaCantidad() {
		servidor.expect(requestTo(URL + "/api/productos/1/descontar"))
				.andExpect(method(HttpMethod.PATCH))
				.andExpect(content().json("{\"cantidad\":2}"))
				.andRespond(withSuccess(PRODUCTO_JSON.replace("\"existencia\":5", "\"existencia\":3"),
						MediaType.APPLICATION_JSON));

		ProductoDto producto = cliente.descontarExistencia(1, 2);

		assertThat(producto.existencia()).isEqualTo(3);
		servidor.verify();
	}

	@Test
	void traduce404AProductoNoEncontrado() {
		servidor.expect(requestTo(URL + "/api/productos/9")).andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> cliente.obtenerProducto(9))
				.isInstanceOf(ProductoNoEncontradoException.class)
				.hasMessage("No existe el producto con id 9");
	}

	@Test
	void traduce401AErrorDeAutenticacion() {
		servidor.expect(requestTo(URL + "/api/productos/1")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

		assertThatThrownBy(() -> cliente.obtenerProducto(1)).isInstanceOf(ProductosAutenticacionException.class);
	}

	@Test
	void traduce409AlDescontarAStockInsuficiente() {
		servidor.expect(requestTo(URL + "/api/productos/1/descontar")).andRespond(withStatus(HttpStatus.CONFLICT));

		assertThatThrownBy(() -> cliente.descontarExistencia(1, 10)).isInstanceOf(StockInsuficienteException.class);
	}

	@Test
	void traduceElTiempoAgotado() {
		servidor.expect(requestTo(URL + "/api/productos/1")).andRespond(solicitud -> {
			throw new SocketTimeoutException("Read timed out");
		});

		assertThatThrownBy(() -> cliente.obtenerProducto(1)).isInstanceOf(ProductosTiempoAgotadoException.class);
	}

	@Test
	void traduceLaConexionRechazada() {
		servidor.expect(requestTo(URL + "/api/productos/1")).andRespond(solicitud -> {
			throw new ConnectException("Connection refused");
		});

		assertThatThrownBy(() -> cliente.obtenerProducto(1))
				.isInstanceOf(ProductosNoDisponibleException.class)
				.hasMessage("No se pudo conectar con el servicio de productos");
	}

	@Test
	void traduceUnError500() {
		servidor.expect(requestTo(URL + "/api/productos/1")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThatThrownBy(() -> cliente.obtenerProducto(1))
				.isInstanceOf(ProductosNoDisponibleException.class)
				.hasMessageContaining("500");
	}
}
