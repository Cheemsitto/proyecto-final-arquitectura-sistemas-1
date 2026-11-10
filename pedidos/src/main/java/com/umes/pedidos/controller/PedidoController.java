package com.umes.pedidos.controller;

import com.umes.pedidos.dto.CrearPedidoRequest;
import com.umes.pedidos.dto.ErrorRespuesta;
import com.umes.pedidos.dto.PedidoResponse;
import com.umes.pedidos.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Capa de presentación: recibe las peticiones HTTP y delega en el servicio. */
@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "Registro y consulta de pedidos")
public class PedidoController {

	private final PedidoService pedidoService;

	public PedidoController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	@Operation(summary = "Crear pedido",
			description = "Consulta el producto en el servicio de productos, valida la existencia, "
					+ "la descuenta y registra el pedido.")
	@ApiResponse(responseCode = "201", description = "Pedido creado")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "401", description = "Falta la clave de servicio", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "404", description = "El producto no existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "409", description = "Existencia insuficiente", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "502", description = "Productos rechazó la autenticación", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "503", description = "Productos no está disponible", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "504", description = "Productos no respondió a tiempo", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@PostMapping
	public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody CrearPedidoRequest solicitud) {
		PedidoResponse creado = pedidoService.crearPedido(solicitud);
		return ResponseEntity.created(URI.create("/api/pedidos/" + creado.id())).body(creado);
	}

	@Operation(summary = "Listar pedidos")
	@ApiResponse(responseCode = "200", description = "Lista de pedidos")
	@GetMapping
	public List<PedidoResponse> listar() {
		return pedidoService.listar();
	}

	@Operation(summary = "Consultar pedido por id")
	@ApiResponse(responseCode = "200", description = "Pedido encontrado")
	@ApiResponse(responseCode = "404", description = "El pedido no existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@GetMapping("/{id}")
	public PedidoResponse obtener(@PathVariable Long id) {
		return pedidoService.obtener(id);
	}
}
