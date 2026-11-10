const Producto = require('../models/producto');
const { validarProducto, validarCantidad, validarId } = require('../models/reglasProducto');
const { NoEncontradoError } = require('../errores');
const vista = require('../views/productoView');

// Controlador: recibe la petición, usa el modelo y responde con la vista.

async function buscarOFallar(idTexto) {
  const id = validarId(idTexto);
  const producto = await Producto.findByPk(id);
  if (!producto) {
    throw new NoEncontradoError(`No existe el producto con id ${id}`);
  }
  return producto;
}

async function listar(req, res) {
  const productos = await Producto.findAll({ order: [['id', 'ASC']] });
  res.json(vista.productos(productos));
}

async function obtener(req, res) {
  const producto = await buscarOFallar(req.params.id);
  res.json(vista.producto(producto));
}

async function crear(req, res) {
  const datos = validarProducto(req.body);
  const producto = await Producto.create(datos);
  res.status(201).location(`/api/productos/${producto.id}`).json(vista.producto(producto));
}

async function actualizar(req, res) {
  const producto = await buscarOFallar(req.params.id);
  const datos = validarProducto(req.body);
  await producto.update(datos);
  res.json(vista.producto(producto));
}

async function eliminar(req, res) {
  const producto = await buscarOFallar(req.params.id);
  await producto.destroy();
  res.json(vista.mensaje(`Producto ${producto.id} eliminado`));
}

// Usado por el servicio de pedidos al confirmar una venta.
async function descontar(req, res) {
  const id = validarId(req.params.id);
  const cantidad = validarCantidad(req.body?.cantidad);
  const producto = await Producto.descontarExistencia(id, cantidad);
  res.json(vista.producto(producto));
}

module.exports = { listar, obtener, crear, actualizar, eliminar, descontar };
