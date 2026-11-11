// Pruebas de descontarExistencia con Sequelize simulado (mock).
jest.mock('../src/config/database', () => ({
  define: jest.fn(() => ({})),
  transaction: jest.fn(),
}));

const sequelize = require('../src/config/database');
const Producto = require('../src/models/producto');
const { NoEncontradoError, ExistenciaInsuficienteError } = require('../src/errores');

const transaccion = { LOCK: { UPDATE: 'UPDATE' } };

function filaFalsa(existencia) {
  return { id: 1, existencia, save: jest.fn(async () => {}) };
}

beforeEach(() => {
  sequelize.transaction.mockImplementation(async (trabajo) => trabajo(transaccion));
  Producto.findByPk = jest.fn();
});

test('descuenta dentro de una transacción con bloqueo de fila', async () => {
  const fila = filaFalsa(5);
  Producto.findByPk.mockResolvedValue(fila);

  const resultado = await Producto.descontarExistencia(1, 2);

  expect(resultado.existencia).toBe(3);
  expect(Producto.findByPk).toHaveBeenCalledWith(1, { transaction: transaccion, lock: 'UPDATE' });
  expect(fila.save).toHaveBeenCalledWith({ transaction: transaccion });
});

test('permite dejar la existencia en 0', async () => {
  const fila = filaFalsa(2);
  Producto.findByPk.mockResolvedValue(fila);

  const resultado = await Producto.descontarExistencia(1, 2);

  expect(resultado.existencia).toBe(0);
});

test('no guarda si la existencia no alcanza', async () => {
  const fila = filaFalsa(1);
  Producto.findByPk.mockResolvedValue(fila);

  await expect(Producto.descontarExistencia(1, 2)).rejects.toBeInstanceOf(ExistenciaInsuficienteError);
  expect(fila.save).not.toHaveBeenCalled();
});

test('lanza NoEncontradoError si el producto no existe', async () => {
  Producto.findByPk.mockResolvedValue(null);

  await expect(Producto.descontarExistencia(9, 1)).rejects.toBeInstanceOf(NoEncontradoError);
});

test('reponer suma la existencia dentro de la transacción', async () => {
  const fila = filaFalsa(3);
  Producto.findByPk.mockResolvedValue(fila);

  const resultado = await Producto.reponerExistencia(1, 2);

  expect(resultado.existencia).toBe(5);
  expect(fila.save).toHaveBeenCalledWith({ transaction: transaccion });
});
