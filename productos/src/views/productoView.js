// Vista: define cómo se presenta un producto en las respuestas JSON.
// MySQL devuelve DECIMAL como texto, por eso el precio se convierte a número aquí.

function producto(registro) {
  return {
    id: registro.id,
    nombre: registro.nombre,
    descripcion: registro.descripcion ?? null,
    precio: Number(registro.precio),
    existencia: registro.existencia,
    creadoEn: registro.creadoEn,
    actualizadoEn: registro.actualizadoEn,
  };
}

function productos(registros) {
  return registros.map(producto);
}

function mensaje(texto) {
  return { mensaje: texto };
}

function error(texto, extra = {}) {
  return { error: texto, ...extra };
}

module.exports = { producto, productos, mensaje, error };
