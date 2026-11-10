package com.umes.pedidos.exception;

/** El servicio de productos no respondió dentro del tiempo configurado. */
public class ProductosTiempoAgotadoException extends RuntimeException {

	public ProductosTiempoAgotadoException() {
		super("El servicio de productos no respondió a tiempo");
	}
}
