package com.umes.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ProductosClientConfig {

	public static final String CABECERA_CLAVE = "X-Service-Key";

	@Bean
	RestClient productosRestClient(
			@Value("${productos.url}") String url,
			@Value("${productos.timeout-ms}") long tiempoMaximoMs,
			@Value("${productos.service-key}") String claveServicio) {
		if (claveServicio == null || claveServicio.isBlank()) {
			throw new IllegalStateException("Falta la variable de entorno PRODUCTOS_SERVICE_KEY");
		}

		return configurar(RestClient.builder().requestFactory(fabricaConTiempoMaximo(tiempoMaximoMs)), url,
				claveServicio).build();
	}

	/**
	 * Cliente HTTP del JDK (java.net.http). Admite PATCH, que usa el descuento de existencia.
	 * Si productos no conecta o no responde en este tiempo, el pedido falla con 504.
	 */
	public static JdkClientHttpRequestFactory fabricaConTiempoMaximo(long tiempoMaximoMs) {
		Duration tiempoMaximo = Duration.ofMillis(tiempoMaximoMs);
		HttpClient cliente = HttpClient.newBuilder().connectTimeout(tiempoMaximo).build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
		fabrica.setReadTimeout(tiempoMaximo);
		return fabrica;
	}

	/** Dirección base y clave de servicio. Se reutiliza en las pruebas. */
	public static RestClient.Builder configurar(RestClient.Builder builder, String url, String claveServicio) {
		return builder.baseUrl(url).defaultHeader(CABECERA_CLAVE, claveServicio);
	}
}
