const { ValidacionError, NoEncontradoError, ExistenciaInsuficienteError } = require('../errores');
const vista = require('../views/productoView');

function rutaNoEncontrada(req, res) {
  res.status(404).json(vista.error(`No existe la ruta ${req.method} ${req.originalUrl}`));
}

// eslint-disable-next-line no-unused-vars
function manejarErrores(err, req, res, next) {
  if (err instanceof ValidacionError) {
    return res.status(400).json(vista.error(err.message, { detalles: err.detalles }));
  }
  if (err instanceof NoEncontradoError) {
    return res.status(404).json(vista.error(err.message));
  }
  if (err instanceof ExistenciaInsuficienteError) {
    return res.status(409).json(
      vista.error(err.message, {
        existenciaDisponible: err.disponible,
        cantidadSolicitada: err.solicitada,
      }),
    );
  }
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json(vista.error('El cuerpo de la petición no es un JSON válido'));
  }
  if (err.name === 'SequelizeValidationError') {
    return res.status(400).json(
      vista.error('Datos de producto inválidos', {
        detalles: err.errors.map((e) => e.message),
      }),
    );
  }

  console.error(err);
  return res.status(500).json(vista.error('Error interno del servicio de productos'));
}

module.exports = { rutaNoEncontrada, manejarErrores };
