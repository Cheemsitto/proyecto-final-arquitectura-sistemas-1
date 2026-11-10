package com.umes.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Pedido registrado. Guarda una copia del nombre y del precio del producto
 * al momento de la venta, porque los productos viven en otra base de datos.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "producto_id", nullable = false)
	private Integer productoId;

	@Column(name = "nombre_producto", nullable = false, length = 120)
	private String nombreProducto;

	@Column(nullable = false)
	private Integer cantidad;

	@Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
	private BigDecimal precioUnitario;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	protected Pedido() {
		// Requerido por JPA
	}

	public Pedido(Integer productoId, String nombreProducto, Integer cantidad,
			BigDecimal precioUnitario, LocalDateTime fechaCreacion) {
		this.productoId = productoId;
		this.nombreProducto = nombreProducto;
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario.setScale(2, RoundingMode.HALF_UP);
		this.total = this.precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
		this.fechaCreacion = fechaCreacion;
	}

	public Long getId() {
		return id;
	}

	public Integer getProductoId() {
		return productoId;
	}

	public String getNombreProducto() {
		return nombreProducto;
	}

	public Integer getCantidad() {
		return cantidad;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}
}
