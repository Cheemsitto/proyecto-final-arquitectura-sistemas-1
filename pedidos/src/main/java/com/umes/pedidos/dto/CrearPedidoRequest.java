package com.umes.pedidos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Datos para crear un pedido")
public record CrearPedidoRequest(
		@Schema(description = "Id del producto en el servicio de productos", example = "1")
		@NotNull(message = "El productoId es obligatorio")
		@Positive(message = "El productoId debe ser mayor a 0")
		Integer productoId,

		@Schema(description = "Unidades a comprar", example = "2")
		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor a 0")
		Integer cantidad) {
}
