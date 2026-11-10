package com.umes.pedidos.service;

import com.umes.pedidos.client.ProductosClient;
import com.umes.pedidos.dto.CrearPedidoRequest;
import com.umes.pedidos.dto.PedidoResponse;
import com.umes.pedidos.dto.ProductoDto;
import com.umes.pedidos.exception.PedidoNoEncontradoException;
import com.umes.pedidos.exception.ProductoNoEncontradoException;
import com.umes.pedidos.exception.ProductosAutenticacionException;
import com.umes.pedidos.exception.ProductosTiempoAgotadoException;
import com.umes.pedidos.exception.StockInsuficienteException;
import com.umes.pedidos.model.Pedido;
import com.umes.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de la capa de negocio con el repositorio y el cliente HTTP simulados (mocks). */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

	@Mock
	private PedidoRepository pedidoRepository;

	@Mock
	private ProductosClient productosClient;

	@InjectMocks
	private PedidoService pedidoService;

	private static ProductoDto producto(int existencia) {
		return new ProductoDto(1, "Teclado", new BigDecimal("150.00"), existencia);
	}

	@Test
	void creaElPedidoYDescuentaLaExistencia() {
		when(productosClient.obtenerProducto(1)).thenReturn(producto(5));
		when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> {
			Pedido pedido = invocacion.getArgument(0);
			ReflectionTestUtils.setField(pedido, "id", 10L);
			return pedido;
		});

		PedidoResponse respuesta = pedidoService.crearPedido(new CrearPedidoRequest(1, 2));

		assertThat(respuesta.id()).isEqualTo(10L);
		assertThat(respuesta.nombreProducto()).isEqualTo("Teclado");
		assertThat(respuesta.cantidad()).isEqualTo(2);
		assertThat(respuesta.total()).isEqualByComparingTo("300.00");

		// Primero consulta, luego descuenta y al final guarda.
		InOrder orden = inOrder(productosClient, pedidoRepository);
		orden.verify(productosClient).obtenerProducto(1);
		orden.verify(productosClient).descontarExistencia(1, 2);
		orden.verify(pedidoRepository).save(any(Pedido.class));
	}

	@Test
	void guardaElPrecioVigenteDelProducto() {
		when(productosClient.obtenerProducto(1)).thenReturn(producto(5));
		when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

		pedidoService.crearPedido(new CrearPedidoRequest(1, 3));

		ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
		verify(pedidoRepository).save(captor.capture());
		assertThat(captor.getValue().getPrecioUnitario()).isEqualByComparingTo("150.00");
		assertThat(captor.getValue().getTotal()).isEqualByComparingTo("450.00");
	}

	@Test
	void rechazaElPedidoSiNoHayExistenciaSuficiente() {
		when(productosClient.obtenerProducto(1)).thenReturn(producto(1));

		assertThatThrownBy(() -> pedidoService.crearPedido(new CrearPedidoRequest(1, 2)))
				.isInstanceOf(StockInsuficienteException.class)
				.hasMessageContaining("disponible 1, solicitado 2");

		verify(productosClient, never()).descontarExistencia(anyInt(), anyInt());
		verify(pedidoRepository, never()).save(any());
	}

	@Test
	void rechazaElPedidoSiElProductoNoExiste() {
		when(productosClient.obtenerProducto(99)).thenThrow(new ProductoNoEncontradoException(99));

		assertThatThrownBy(() -> pedidoService.crearPedido(new CrearPedidoRequest(99, 1)))
				.isInstanceOf(ProductoNoEncontradoException.class);

		verify(pedidoRepository, never()).save(any());
	}

	@Test
	void noRegistraElPedidoSiProductosNoRespondeATiempo() {
		when(productosClient.obtenerProducto(1)).thenThrow(new ProductosTiempoAgotadoException());

		assertThatThrownBy(() -> pedidoService.crearPedido(new CrearPedidoRequest(1, 1)))
				.isInstanceOf(ProductosTiempoAgotadoException.class);

		verify(productosClient, never()).descontarExistencia(anyInt(), anyInt());
		verify(pedidoRepository, never()).save(any());
	}

	@Test
	void noRegistraElPedidoSiProductosRechazaLaAutenticacion() {
		when(productosClient.obtenerProducto(1)).thenThrow(new ProductosAutenticacionException());

		assertThatThrownBy(() -> pedidoService.crearPedido(new CrearPedidoRequest(1, 1)))
				.isInstanceOf(ProductosAutenticacionException.class);

		verify(pedidoRepository, never()).save(any());
	}

	@Test
	void noRegistraElPedidoSiOtroPedidoGanoLaExistencia() {
		// La consulta decía que había stock, pero al descontar otro pedido ya lo había tomado.
		when(productosClient.obtenerProducto(1)).thenReturn(producto(2));
		when(productosClient.descontarExistencia(1, 2))
				.thenThrow(new StockInsuficienteException("La existencia ya no alcanza"));

		assertThatThrownBy(() -> pedidoService.crearPedido(new CrearPedidoRequest(1, 2)))
				.isInstanceOf(StockInsuficienteException.class);

		verify(pedidoRepository, never()).save(any());
	}

	@Test
	void listaLosPedidosOrdenadosPorId() {
		Pedido pedido = new Pedido(1, "Teclado", 2, new BigDecimal("150.00"), LocalDateTime.now());
		when(pedidoRepository.findAll(Sort.by("id"))).thenReturn(List.of(pedido));

		List<PedidoResponse> pedidos = pedidoService.listar();

		assertThat(pedidos).hasSize(1);
		assertThat(pedidos.get(0).total()).isEqualByComparingTo("300.00");
	}

	@Test
	void fallaSiElPedidoNoExiste() {
		when(pedidoRepository.findById(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> pedidoService.obtener(5L))
				.isInstanceOf(PedidoNoEncontradoException.class)
				.hasMessage("No existe el pedido con id 5");
	}
}
