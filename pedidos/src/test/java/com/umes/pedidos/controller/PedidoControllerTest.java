package com.umes.pedidos.controller;

import com.umes.pedidos.dto.ActualizarPedidoRequest;
import com.umes.pedidos.dto.CrearPedidoRequest;
import com.umes.pedidos.dto.PedidoResponse;
import com.umes.pedidos.exception.GlobalExceptionHandler;
import com.umes.pedidos.exception.PedidoNoEncontradoException;
import com.umes.pedidos.exception.ProductoNoEncontradoException;
import com.umes.pedidos.exception.ProductosAutenticacionException;
import com.umes.pedidos.exception.ProductosNoDisponibleException;
import com.umes.pedidos.exception.ProductosTiempoAgotadoException;
import com.umes.pedidos.exception.StockInsuficienteException;
import com.umes.pedidos.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de la capa de presentación con el servicio simulado (mock). */
@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

	@Mock
	private PedidoService pedidoService;

	private MockMvc mvc;

	@BeforeEach
	void preparar() {
		mvc = MockMvcBuilders.standaloneSetup(new PedidoController(pedidoService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	private void crearPedidoEsperando(int estado) throws Exception {
		mvc.perform(post("/api/pedidos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productoId\":1,\"cantidad\":2}"))
				.andExpect(status().is(estado))
				.andExpect(jsonPath("$.error").exists());
	}

	@Test
	void creaElPedidoYResponde201() throws Exception {
		when(pedidoService.crearPedido(new CrearPedidoRequest(1, 2))).thenReturn(new PedidoResponse(
				7L, 1, "Teclado", 2, new BigDecimal("150.00"), new BigDecimal("300.00"), LocalDateTime.now()));

		mvc.perform(post("/api/pedidos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productoId\":1,\"cantidad\":2}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/pedidos/7"))
				.andExpect(jsonPath("$.total").value(300.00));
	}

	@Test
	void rechazaCantidadCeroSinLlamarAlServicio() throws Exception {
		mvc.perform(post("/api/pedidos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productoId\":1,\"cantidad\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detalles[0]").value("La cantidad debe ser mayor a 0"));

		verifyNoInteractions(pedidoService);
	}

	@Test
	void rechazaCuerpoSinProducto() throws Exception {
		mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":1}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(pedidoService);
	}

	@Test
	void productoInexistenteResponde404() throws Exception {
		when(pedidoService.crearPedido(any())).thenThrow(new ProductoNoEncontradoException(1));
		crearPedidoEsperando(404);
	}

	@Test
	void stockInsuficienteResponde409() throws Exception {
		when(pedidoService.crearPedido(any())).thenThrow(new StockInsuficienteException("sin stock"));
		crearPedidoEsperando(409);
	}

	@Test
	void autenticacionRechazadaResponde502() throws Exception {
		when(pedidoService.crearPedido(any())).thenThrow(new ProductosAutenticacionException());
		crearPedidoEsperando(502);
	}

	@Test
	void productosCaidoResponde503() throws Exception {
		when(pedidoService.crearPedido(any())).thenThrow(new ProductosNoDisponibleException("caído"));
		crearPedidoEsperando(503);
	}

	@Test
	void tiempoAgotadoResponde504() throws Exception {
		when(pedidoService.crearPedido(any())).thenThrow(new ProductosTiempoAgotadoException());
		crearPedidoEsperando(504);
	}

	@Test
	void pedidoInexistenteResponde404() throws Exception {
		when(pedidoService.obtener(3L)).thenThrow(new PedidoNoEncontradoException(3L));

		mvc.perform(get("/api/pedidos/3"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("No existe el pedido con id 3"));
	}

	@Test
	void actualizaElPedido() throws Exception {
		when(pedidoService.actualizar(7L, new ActualizarPedidoRequest(3))).thenReturn(new PedidoResponse(
				7L, 1, "Teclado", 3, new BigDecimal("150.00"), new BigDecimal("450.00"), LocalDateTime.now()));

		mvc.perform(put("/api/pedidos/7").contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cantidad").value(3));
	}

	@Test
	void actualizarConCantidadCeroResponde400() throws Exception {
		mvc.perform(put("/api/pedidos/7").contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":0}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(pedidoService);
	}
}
