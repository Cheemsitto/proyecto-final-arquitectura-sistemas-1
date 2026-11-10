package com.umes.pedidos.exception;

/** El servicio de productos rechazó la clave de servicio (401 o 403). */
public class ProductosAutenticacionException extends RuntimeException {

	public ProductosAutenticacionException() {
		super("El servicio de productos rechazó la autenticación del servicio de pedidos");
	}
}
