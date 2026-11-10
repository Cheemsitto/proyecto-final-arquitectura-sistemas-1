package com.umes.pedidos.exception;

/** No se pudo conectar con el servicio de productos o respondió con un error inesperado. */
public class ProductosNoDisponibleException extends RuntimeException {

	public ProductosNoDisponibleException(String mensaje) {
		super(mensaje);
	}
}
