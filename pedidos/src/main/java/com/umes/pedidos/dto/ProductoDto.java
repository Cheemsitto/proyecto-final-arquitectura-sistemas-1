package com.umes.pedidos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Producto tal como lo devuelve el servicio de productos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoDto(Integer id, String nombre, BigDecimal precio, Integer existencia) {
}
