const { ValidacionError } = require('../errores');

const PRECIO_MAXIMO = 99999999.99;

function tieneMasDeDosDecimales(numero) {
  return Number(numero.toFixed(2)) !== numero;
}

/**
 * Reglas de negocio para crear o editar un producto.
 * Devuelve los datos limpios o lanza ValidacionError con todos los problemas encontrados.
 */
function validarProducto(datos) {
  const entrada = datos && typeof datos === 'object' ? datos : {};
  const errores = [];

  const nombre = typeof entrada.nombre === 'string' ? entrada.nombre.trim() : '';
  if (!nombre) {
    errores.push('El nombre es obligatorio');
  } else if (nombre.length > 120) {
    errores.push('El nombre admite como máximo 120 caracteres');
  }

  let descripcion = null;
  if (entrada.descripcion !== undefined && entrada.descripcion !== null) {
    if (typeof entrada.descripcion !== 'string') {
      errores.push('La descripción debe ser texto');
    } else if (entrada.descripcion.length > 255) {
      errores.push('La descripción admite como máximo 255 caracteres');
    } else {
      descripcion = entrada.descripcion.trim() || null;
    }
  }

  const { precio, existencia } = entrada;
  if (typeof precio !== 'number' || !Number.isFinite(precio)) {
    errores.push('El precio es obligatorio y debe ser un número');
  } else if (precio <= 0) {
    errores.push('El precio debe ser mayor a 0');
  } else if (precio > PRECIO_MAXIMO) {
    errores.push(`El precio no puede ser mayor a ${PRECIO_MAXIMO}`);
  } else if (tieneMasDeDosDecimales(precio)) {
    errores.push('El precio admite como máximo 2 decimales');
  }

  if (!Number.isInteger(existencia)) {
    errores.push('La existencia es obligatoria y debe ser un número entero');
  } else if (existencia <= 0) {
    errores.push('La existencia debe ser mayor a 0');
  }

  if (errores.length > 0) {
    throw new ValidacionError('Datos de producto inválidos', errores);
  }

  return { nombre, descripcion, precio, existencia };
}

function validarCantidad(cantidad) {
  if (!Number.isInteger(cantidad) || cantidad <= 0) {
    throw new ValidacionError('Cantidad inválida', [
      'La cantidad debe ser un número entero mayor a 0',
    ]);
  }
  return cantidad;
}

function validarId(valor) {
  const id = Number(valor);
  if (!Number.isInteger(id) || id <= 0) {
    throw new ValidacionError('Id inválido', ['El id debe ser un número entero mayor a 0']);
  }
  return id;
}

module.exports = { validarProducto, validarCantidad, validarId };
