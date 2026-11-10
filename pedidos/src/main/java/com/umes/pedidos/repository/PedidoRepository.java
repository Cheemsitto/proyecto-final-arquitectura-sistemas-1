package com.umes.pedidos.repository;

import com.umes.pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Capa de datos: acceso a la tabla pedidos mediante Spring Data JPA (Hibernate). */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
