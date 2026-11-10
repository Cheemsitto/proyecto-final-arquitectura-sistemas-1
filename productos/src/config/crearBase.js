const mysql = require('mysql2/promise');
const config = require('./index');

/**
 * Crea la base del servicio si todavía no existe, para que el servicio arranque
 * aunque no se haya ejecutado el script SQL. Si el usuario no tiene permiso, solo avisa.
 */
async function crearBaseSiNoExiste() {
  if (!/^[A-Za-z0-9_]+$/.test(config.db.nombre)) {
    throw new Error('DB_NAME solo admite letras, números y guion bajo');
  }

  let conexion;
  try {
    conexion = await mysql.createConnection({
      host: config.db.host,
      port: config.db.puerto,
      user: config.db.usuario,
      password: config.db.password,
      ssl: config.db.ssl,
    });
    await conexion.query(`CREATE DATABASE IF NOT EXISTS \`${config.db.nombre}\``);
  } catch (err) {
    console.warn(`No se pudo crear la base ${config.db.nombre}: ${err.message}`);
  } finally {
    if (conexion) {
      await conexion.end();
    }
  }
}

module.exports = crearBaseSiNoExiste;
