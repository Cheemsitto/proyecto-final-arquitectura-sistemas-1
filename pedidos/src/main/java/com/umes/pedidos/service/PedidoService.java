package com.umes.pedidos.service;

import com.umes.pedidos.client.ProductosClient;
import com.umes.pedidos.dto.CrearPedidoRequest;
import com.umes.pedidos.dto.PedidoResponse;
import com.umes.pedidos.dto.ProductoDto;
import com.umes.pedidos.exception.PedidoNoEncontradoException;
import com.umes.pedidos.exception.StockInsuficienteException;
import com.umes.pedidos.model.Pedido;
import com.umes.pedidos.repository.PedidoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Capa de negocio: reglas para registrar pedidos. */
@Service
public class PedidoService {

	private final PedidoRepository pedidoRepository;
	private final ProductosClient productosClient;

	public PedidoService(PedidoRepository pedidoRepository, ProductosClient productosClient) {
		this.pedidoRepository = pedidoRepository;
		this.productosClient = productosClient;
	}

	/**
	 * 1. Consulta el producto (falla si no existe, si productos no responde a tiempo o si rechaza la clave).
	 * 2. Verifica que la existencia alcance.
	 * 3. Pide a productos que descuente la existencia.
	 * 4. Guarda el pedido con el precio vigente.
	 */
	public PedidoResponse crearPedido(CrearPedidoRequest solicitud) {
		Integer productoId = solicitud.productoId();
		int cantidad = solicitud.cantidad();

		ProductoDto producto = productosClient.obtenerProducto(productoId);

		int disponible = producto.existencia() == null ? 0 : producto.existencia();
		if (disponible < cantidad) {
			throw new StockInsuficienteException(String.format(
					"Existencia insuficiente para el producto %d: disponible %d, solicitado %d",
					productoId, disponible, cantidad));
		}

		productosClient.descontarExistencia(productoId, cantidad);

		Pedido pedido = new Pedido(productoId, producto.nombre(), cantidad, producto.precio(), LocalDateTime.now());
		return PedidoResponse.desde(pedidoRepository.save(pedido));
	}

	@Transactional(readOnly = true)
	public List<PedidoResponse> listar() {
		return pedidoRepository.findAll(Sort.by("id")).stream()
				.map(PedidoResponse::desde)
				.toList();
	}

	@Transactional(readOnly = true)
	public PedidoResponse obtener(Long id) {
		return pedidoRepository.findById(id)
				.map(PedidoResponse::desde)
				.orElseThrow(() -> new PedidoNoEncontradoException(id));
	}
}
