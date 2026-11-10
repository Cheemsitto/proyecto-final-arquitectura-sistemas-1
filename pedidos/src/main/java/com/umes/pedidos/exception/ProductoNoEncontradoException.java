package com.umes.pedidos.exception;

/** El servicio de productos respondió 404. */
public class ProductoNoEncontradoException extends RuntimeException {

	public ProductoNoEncontradoException(Integer productoId) {
		super("No existe el producto con id " + productoId);
	}
}
