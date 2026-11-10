-- Base de datos del servicio de Pedidos (capas + Spring Data JPA)
CREATE DATABASE IF NOT EXISTS pedidos_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE pedidos_db;

-- Pedidos no tiene llave foránea hacia productos porque cada servicio
-- tiene su propia base. El nombre y el precio se copian al momento de vender.
CREATE TABLE IF NOT EXISTS pedidos (
  id               BIGINT         NOT NULL AUTO_INCREMENT,
  producto_id      INT            NOT NULL,
  nombre_producto  VARCHAR(120)   NOT NULL,
  cantidad         INT            NOT NULL,
  precio_unitario  DECIMAL(10, 2) NOT NULL,
  total            DECIMAL(12, 2) NOT NULL,
  fecha_creacion   DATETIME(6)    NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT chk_pedidos_cantidad CHECK (cantidad > 0),
  INDEX idx_pedidos_producto (producto_id)
);
