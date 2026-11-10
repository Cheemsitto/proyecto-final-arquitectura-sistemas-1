package com.umes.pedidos.exception;

/** La existencia del producto no alcanza para la cantidad pedida. */
public class StockInsuficienteException extends RuntimeException {

	public StockInsuficienteException(String mensaje) {
		super(mensaje);
	}
}
