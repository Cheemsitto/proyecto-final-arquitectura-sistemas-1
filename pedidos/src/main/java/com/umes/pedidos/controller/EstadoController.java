package com.umes.pedidos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Estado")
public class EstadoController {

	@Operation(summary = "Estado del servicio")
	@SecurityRequirements
	@GetMapping("/health")
	public Map<String, String> estado() {
		return Map.of("servicio", "pedidos", "estado", "ok");
	}
}
