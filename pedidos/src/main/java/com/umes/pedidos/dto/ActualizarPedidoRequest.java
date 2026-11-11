package com.umes.pedidos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Nueva cantidad del pedido")
public record ActualizarPedidoRequest(
		@Schema(description = "Unidades del pedido", example = "3")
		@NotNull(message = "La cantidad es obligatoria")
		@Positive(message = "La cantidad debe ser mayor a 0")
		Integer cantidad) {
}
