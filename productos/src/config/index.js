require('dotenv').config({ quiet: true });

function entero(valor, porDefecto) {
  const numero = Number.parseInt(valor, 10);
  return Number.isNaN(numero) ? porDefecto : numero;
}

// Opciones SSL para MySQL en la nube. Con DB_SSL_CA se verifica el certificado del servidor.
function ssl() {
  if (process.env.DB_SSL_CA) {
    return { ca: process.env.DB_SSL_CA };
  }
  if (process.env.DB_SSL === 'true') {
    return { rejectUnauthorized: false };
  }
  return undefined;
}

module.exports = {
  puerto: entero(process.env.PORT, 3001),
  // Clave que deben enviar el broker y el servicio de pedidos en X-Service-Key.
  claveServicio: process.env.SERVICE_KEY || '',
  db: {
    host: process.env.DB_HOST || 'localhost',
    puerto: entero(process.env.DB_PORT, 3306),
    nombre: process.env.DB_NAME || 'productos_db',
    usuario: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    ssl: ssl(),
    // Crea la tabla si no existe. El script SQL hace lo mismo.
    sincronizar: process.env.DB_SYNC !== 'false',
  },
};
