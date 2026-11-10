-- Base de datos del Broker (FastAPI + SQLAlchemy)
-- Solo guarda los usuarios que pueden pedir un token.
CREATE DATABASE IF NOT EXISTS broker_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE broker_db;

CREATE TABLE IF NOT EXISTS usuarios (
  id             INT          NOT NULL AUTO_INCREMENT,
  usuario        VARCHAR(50)  NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  activo         TINYINT(1)   NOT NULL DEFAULT 1,
  creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_usuarios_usuario (usuario)
);

-- El usuario administrador se crea al iniciar el broker con las variables
-- ADMIN_USUARIO y ADMIN_PASSWORD, para no guardar contraseñas en este script.
