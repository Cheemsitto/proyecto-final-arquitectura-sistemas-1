-- Base de datos del servicio de Productos (MVC + Sequelize)
CREATE DATABASE IF NOT EXISTS productos_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE productos_db;

CREATE TABLE IF NOT EXISTS productos (
  id             INT            NOT NULL AUTO_INCREMENT,
  nombre         VARCHAR(120)   NOT NULL,
  descripcion    VARCHAR(255)   NULL,
  precio         DECIMAL(10, 2) NOT NULL,
  existencia     INT            NOT NULL,
  creado_en      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_en DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  -- El precio siempre debe ser mayor a 0.
  CONSTRAINT chk_productos_precio CHECK (precio > 0),
  -- Las ventas pueden dejar la existencia en 0, pero nunca en negativo.
  -- La regla "existencia mayor a 0" se valida en el modelo al crear o editar.
  CONSTRAINT chk_productos_existencia CHECK (existencia >= 0)
);

INSERT INTO productos (nombre, descripcion, precio, existencia) VALUES
  ('Teclado mecánico', 'Teclado mecánico con switches rojos', 450.00, 15),
  ('Mouse inalámbrico', 'Mouse óptico de 1600 DPI', 125.50, 30),
  ('Monitor 24 pulgadas', 'Monitor Full HD IPS', 1350.00, 8),
  ('Audífonos', 'Audífonos con micrófono', 210.75, 20);
