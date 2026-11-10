const crypto = require('node:crypto');
const vista = require('../views/productoView');

function huella(texto) {
  return crypto.createHash('sha256').update(String(texto)).digest();
}

/**
 * Solo deja pasar peticiones que traen la clave interna en X-Service-Key.
 * La envían el broker y el servicio de pedidos, así nadie llama a este servicio
 * directamente desde Postman.
 */
function claveServicio(claveEsperada) {
  return function verificarClave(req, res, next) {
    const recibida = req.get('X-Service-Key');
    const valida =
      Boolean(claveEsperada) &&
      Boolean(recibida) &&
      crypto.timingSafeEqual(huella(recibida), huella(claveEsperada));

    if (!valida) {
      return res.status(401).json(vista.error('Clave de servicio inválida o ausente'));
    }
    return next();
  };
}

module.exports = claveServicio;
