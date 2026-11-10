package com.umes.pedidos.exception;

/** No existe un pedido con el id solicitado. */
public class PedidoNoEncontradoException extends RuntimeException {

	public PedidoNoEncontradoException(Long id) {
		super("No existe el pedido con id " + id);
	}
}
