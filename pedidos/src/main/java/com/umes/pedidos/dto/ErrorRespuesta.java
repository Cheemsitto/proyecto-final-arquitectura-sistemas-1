package com.umes.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Error devuelto por el servicio")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorRespuesta(
		@Schema(example = "Existencia insuficiente para el producto 1: disponible 1, solicitado 2") String error,
		List<String> detalles) {

	public ErrorRespuesta(String error) {
		this(error, List.of());
	}
}
