package com.umes.pedidos.client;

import com.umes.pedidos.dto.ProductoDto;
import com.umes.pedidos.exception.ProductoNoEncontradoException;
import com.umes.pedidos.exception.ProductosAutenticacionException;
import com.umes.pedidos.exception.ProductosNoDisponibleException;
import com.umes.pedidos.exception.ProductosTiempoAgotadoException;
import com.umes.pedidos.exception.StockInsuficienteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Map;

/**
 * Capa de integración: única clase que conoce cómo hablar por HTTP con el servicio de productos.
 * Convierte cada respuesta de error en una excepción del dominio de pedidos.
 */
@Component
public class ProductosClient {

	private final RestClient restClient;

	public ProductosClient(RestClient productosRestClient) {
		this.restClient = productosRestClient;
	}

	public ProductoDto obtenerProducto(Integer productoId) {
		try {
			ProductoDto producto = restClient.get()
					.uri("/api/productos/{id}", productoId)
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.body(ProductoDto.class);
			if (producto == null) {
				throw new ProductosNoDisponibleException("El servicio de productos devolvió una respuesta vacía");
			}
			return producto;
		} catch (RestClientException ex) {
			throw traducir(ex, productoId);
		}
	}

	public ProductoDto descontarExistencia(Integer productoId, int cantidad) {
		try {
			return restClient.patch()
					.uri("/api/productos/{id}/descontar", productoId)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(Map.of("cantidad", cantidad))
					.retrieve()
					.body(ProductoDto.class);
		} catch (RestClientException ex) {
			throw traducir(ex, productoId);
		}
	}

	public ProductoDto reponerExistencia(Integer productoId, int cantidad) {
		try {
			return restClient.patch()
					.uri("/api/productos/{id}/reponer", productoId)
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(Map.of("cantidad", cantidad))
					.retrieve()
					.body(ProductoDto.class);
		} catch (RestClientException ex) {
			throw traducir(ex, productoId);
		}
	}

	private RuntimeException traducir(RestClientException ex, Integer productoId) {
		if (ex instanceof RestClientResponseException respuesta) {
			HttpStatusCode estado = respuesta.getStatusCode();
			if (estado.isSameCodeAs(HttpStatus.NOT_FOUND)) {
				return new ProductoNoEncontradoException(productoId);
			}
			if (estado.isSameCodeAs(HttpStatus.UNAUTHORIZED) || estado.isSameCodeAs(HttpStatus.FORBIDDEN)) {
				return new ProductosAutenticacionException();
			}
			if (estado.isSameCodeAs(HttpStatus.CONFLICT)) {
				return new StockInsuficienteException(
						"La existencia del producto " + productoId + " ya no alcanza para la cantidad solicitada");
			}
			return new ProductosNoDisponibleException(
					"El servicio de productos respondió con el estado " + estado.value());
		}
		if (ex instanceof ResourceAccessException) {
			return esTiempoAgotado(ex)
					? new ProductosTiempoAgotadoException()
					: new ProductosNoDisponibleException("No se pudo conectar con el servicio de productos");
		}
		return new ProductosNoDisponibleException("El servicio de productos devolvió una respuesta inválida");
	}

	private static boolean esTiempoAgotado(Throwable ex) {
		for (Throwable causa = ex; causa != null; causa = causa.getCause()) {
			if (causa instanceof SocketTimeoutException || causa instanceof HttpTimeoutException) {
				return true;
			}
		}
		return false;
	}
}
