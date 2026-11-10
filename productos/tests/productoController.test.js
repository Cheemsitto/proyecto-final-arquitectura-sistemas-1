// Pruebas del controlador con el modelo simulado (mock): no se usa base de datos.
jest.mock('../src/models/producto', () => ({
  findAll: jest.fn(),
  findByPk: jest.fn(),
  create: jest.fn(),
  descontarExistencia: jest.fn(),
}));

const request = require('supertest');
const app = require('../src/app');
const Producto = require('../src/models/producto');
const { NoEncontradoError, ExistenciaInsuficienteError } = require('../src/errores');

const CLAVE = { 'X-Service-Key': 'clave-de-prueba' };

function productoFalso(datos = {}) {
  return {
    id: 1,
    nombre: 'Teclado',
    descripcion: null,
    precio: '150.00',
    existencia: 5,
    creadoEn: '2026-11-10T00:00:00.000Z',
    actualizadoEn: '2026-11-10T00:00:00.000Z',
    update: jest.fn(async function update(cambios) {
      Object.assign(this, cambios);
      return this;
    }),
    destroy: jest.fn(async () => {}),
    ...datos,
  };
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe('Clave de servicio', () => {
  test('rechaza la petición sin X-Service-Key', async () => {
    const res = await request(app).get('/api/productos');
    expect(res.status).toBe(401);
    expect(res.body.error).toMatch(/clave de servicio/i);
    expect(Producto.findAll).not.toHaveBeenCalled();
  });

  test('rechaza una clave incorrecta', async () => {
    const res = await request(app).get('/api/productos').set('X-Service-Key', 'otra');
    expect(res.status).toBe(401);
  });

  test('health no exige clave', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
  });
});

describe('GET /api/productos', () => {
  test('lista productos con el precio como número', async () => {
    Producto.findAll.mockResolvedValue([productoFalso(), productoFalso({ id: 2, precio: '20.50' })]);

    const res = await request(app).get('/api/productos').set(CLAVE);

    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(2);
    expect(res.body[1].precio).toBe(20.5);
  });
});

describe('GET /api/productos/:id', () => {
  test('devuelve el producto', async () => {
    Producto.findByPk.mockResolvedValue(productoFalso());

    const res = await request(app).get('/api/productos/1').set(CLAVE);

    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({ id: 1, nombre: 'Teclado', precio: 150, existencia: 5 });
    expect(Producto.findByPk).toHaveBeenCalledWith(1);
  });

  test('responde 404 si no existe', async () => {
    Producto.findByPk.mockResolvedValue(null);

    const res = await request(app).get('/api/productos/99').set(CLAVE);

    expect(res.status).toBe(404);
  });

  test('responde 400 si el id no es numérico', async () => {
    const res = await request(app).get('/api/productos/abc').set(CLAVE);

    expect(res.status).toBe(400);
    expect(Producto.findByPk).not.toHaveBeenCalled();
  });
});

describe('POST /api/productos', () => {
  test('crea un producto válido', async () => {
    Producto.create.mockImplementation(async (datos) => productoFalso({ id: 7, ...datos }));

    const res = await request(app)
      .post('/api/productos')
      .set(CLAVE)
      .send({ nombre: '  Mouse  ', precio: 99.9, existencia: 5 });

    expect(res.status).toBe(201);
    expect(res.headers.location).toBe('/api/productos/7');
    expect(Producto.create).toHaveBeenCalledWith({
      nombre: 'Mouse',
      descripcion: null,
      precio: 99.9,
      existencia: 5,
    });
  });

  test.each([
    ['precio 0', { nombre: 'X', precio: 0, existencia: 5 }, 'El precio debe ser mayor a 0'],
    ['precio negativo', { nombre: 'X', precio: -10, existencia: 5 }, 'El precio debe ser mayor a 0'],
    ['existencia 0', { nombre: 'X', precio: 10, existencia: 0 }, 'La existencia debe ser mayor a 0'],
    [
      'existencia decimal',
      { nombre: 'X', precio: 10, existencia: 2.5 },
      'La existencia es obligatoria y debe ser un número entero',
    ],
    ['sin nombre', { precio: 10, existencia: 1 }, 'El nombre es obligatorio'],
  ])('rechaza %s', async (_caso, cuerpo, mensaje) => {
    const res = await request(app).post('/api/productos').set(CLAVE).send(cuerpo);

    expect(res.status).toBe(400);
    expect(res.body.detalles).toContain(mensaje);
    expect(Producto.create).not.toHaveBeenCalled();
  });

  test('rechaza un JSON mal formado', async () => {
    const res = await request(app)
      .post('/api/productos')
      .set(CLAVE)
      .set('Content-Type', 'application/json')
      .send('{"nombre": ');

    expect(res.status).toBe(400);
  });
});

describe('PUT /api/productos/:id', () => {
  test('actualiza el producto', async () => {
    const existente = productoFalso();
    Producto.findByPk.mockResolvedValue(existente);

    const res = await request(app)
      .put('/api/productos/1')
      .set(CLAVE)
      .send({ nombre: 'Teclado RGB', precio: 175, existencia: 8 });

    expect(res.status).toBe(200);
    expect(existente.update).toHaveBeenCalledWith({
      nombre: 'Teclado RGB',
      descripcion: null,
      precio: 175,
      existencia: 8,
    });
    expect(res.body.precio).toBe(175);
  });

  test('responde 404 si no existe', async () => {
    Producto.findByPk.mockResolvedValue(null);

    const res = await request(app)
      .put('/api/productos/5')
      .set(CLAVE)
      .send({ nombre: 'X', precio: 1, existencia: 1 });

    expect(res.status).toBe(404);
  });
});

describe('DELETE /api/productos/:id', () => {
  test('elimina el producto', async () => {
    const existente = productoFalso();
    Producto.findByPk.mockResolvedValue(existente);

    const res = await request(app).delete('/api/productos/1').set(CLAVE);

    expect(res.status).toBe(200);
    expect(existente.destroy).toHaveBeenCalled();
  });
});

describe('PATCH /api/productos/:id/descontar', () => {
  test('descuenta la existencia', async () => {
    Producto.descontarExistencia.mockResolvedValue(productoFalso({ existencia: 3 }));

    const res = await request(app).patch('/api/productos/1/descontar').set(CLAVE).send({ cantidad: 2 });

    expect(res.status).toBe(200);
    expect(res.body.existencia).toBe(3);
    expect(Producto.descontarExistencia).toHaveBeenCalledWith(1, 2);
  });

  test('rechaza una cantidad de 0', async () => {
    const res = await request(app).patch('/api/productos/1/descontar').set(CLAVE).send({ cantidad: 0 });

    expect(res.status).toBe(400);
    expect(Producto.descontarExistencia).not.toHaveBeenCalled();
  });

  test('responde 404 si el producto no existe', async () => {
    Producto.descontarExistencia.mockRejectedValue(new NoEncontradoError('No existe el producto con id 9'));

    const res = await request(app).patch('/api/productos/9/descontar').set(CLAVE).send({ cantidad: 1 });

    expect(res.status).toBe(404);
  });

  test('responde 409 si no hay existencia suficiente', async () => {
    Producto.descontarExistencia.mockRejectedValue(new ExistenciaInsuficienteError(1, 5, 10));

    const res = await request(app).patch('/api/productos/1/descontar').set(CLAVE).send({ cantidad: 10 });

    expect(res.status).toBe(409);
    expect(res.body).toMatchObject({ existenciaDisponible: 5, cantidadSolicitada: 10 });
  });
});
