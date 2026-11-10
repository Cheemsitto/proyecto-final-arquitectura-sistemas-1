package com.umes.pedidos.dto;

import com.umes.pedidos.model.Pedido;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Pedido registrado")
public record PedidoResponse(
		@Schema(example = "1") Long id,
		@Schema(example = "1") Integer productoId,
		@Schema(example = "Teclado mecánico") String nombreProducto,
		@Schema(example = "2") Integer cantidad,
		@Schema(example = "450.00") BigDecimal precioUnitario,
		@Schema(example = "900.00") BigDecimal total,
		LocalDateTime fechaCreacion) {

	public static PedidoResponse desde(Pedido pedido) {
		return new PedidoResponse(
				pedido.getId(),
				pedido.getProductoId(),
				pedido.getNombreProducto(),
				pedido.getCantidad(),
				pedido.getPrecioUnitario(),
				pedido.getTotal(),
				pedido.getFechaCreacion());
	}
}
