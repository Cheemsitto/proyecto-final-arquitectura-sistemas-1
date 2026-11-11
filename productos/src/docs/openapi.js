const errorSchema = {
  type: 'object',
  properties: {
    error: { type: 'string' },
    detalles: { type: 'array', items: { type: 'string' } },
  },
};

const respuestas = {
  noAutorizado: {
    description: 'Falta la clave de servicio o es incorrecta',
    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
  },
  invalido: {
    description: 'Datos inválidos',
    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
  },
  noEncontrado: {
    description: 'El producto no existe',
    content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
  },
};

const parametroId = {
  name: 'id',
  in: 'path',
  required: true,
  schema: { type: 'integer', minimum: 1 },
};

const productoJson = (descripcion) => ({
  description: descripcion,
  content: { 'application/json': { schema: { $ref: '#/components/schemas/Producto' } } },
});

module.exports = {
  openapi: '3.0.3',
  info: {
    title: 'Servicio de Productos',
    version: '1.0.0',
    description:
      'Servicio MVC con Express y Sequelize. Todas las rutas /api/productos exigen la cabecera ' +
      'X-Service-Key; en el sistema completo la agrega el broker después de validar el token.',
  },
  security: [{ ClaveServicio: [] }],
  components: {
    securitySchemes: {
      ClaveServicio: { type: 'apiKey', in: 'header', name: 'X-Service-Key' },
    },
    schemas: {
      Producto: {
        type: 'object',
        properties: {
          id: { type: 'integer', example: 1 },
          nombre: { type: 'string', example: 'Teclado mecánico' },
          descripcion: { type: 'string', nullable: true, example: 'Switches rojos' },
          precio: { type: 'number', example: 450 },
          existencia: { type: 'integer', example: 15 },
          creadoEn: { type: 'string', format: 'date-time' },
          actualizadoEn: { type: 'string', format: 'date-time' },
        },
      },
      ProductoEntrada: {
        type: 'object',
        required: ['nombre', 'precio', 'existencia'],
        properties: {
          nombre: { type: 'string', maxLength: 120, example: 'Teclado mecánico' },
          descripcion: { type: 'string', maxLength: 255, example: 'Switches rojos' },
          precio: { type: 'number', exclusiveMinimum: true, minimum: 0, example: 450 },
          existencia: { type: 'integer', minimum: 1, example: 15 },
        },
      },
      Descuento: {
        type: 'object',
        required: ['cantidad'],
        properties: { cantidad: { type: 'integer', minimum: 1, example: 2 } },
      },
      Error: errorSchema,
    },
  },
  paths: {
    '/api/productos': {
      get: {
        tags: ['Productos'],
        summary: 'Listar productos',
        responses: {
          200: {
            description: 'Lista de productos',
            content: {
              'application/json': {
                schema: { type: 'array', items: { $ref: '#/components/schemas/Producto' } },
              },
            },
          },
          401: respuestas.noAutorizado,
        },
      },
      post: {
        tags: ['Productos'],
        summary: 'Crear producto',
        description: 'El precio y la existencia deben ser mayores a 0.',
        requestBody: {
          required: true,
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/ProductoEntrada' } },
          },
        },
        responses: {
          201: productoJson('Producto creado'),
          400: respuestas.invalido,
          401: respuestas.noAutorizado,
        },
      },
    },
    '/api/productos/{id}': {
      parameters: [parametroId],
      get: {
        tags: ['Productos'],
        summary: 'Consultar producto por id',
        description: 'También lo usa el servicio de pedidos para revisar precio y existencia.',
        responses: {
          200: productoJson('Producto encontrado'),
          400: respuestas.invalido,
          401: respuestas.noAutorizado,
          404: respuestas.noEncontrado,
        },
      },
      put: {
        tags: ['Productos'],
        summary: 'Actualizar producto',
        requestBody: {
          required: true,
          content: {
            'application/json': { schema: { $ref: '#/components/schemas/ProductoEntrada' } },
          },
        },
        responses: {
          200: productoJson('Producto actualizado'),
          400: respuestas.invalido,
          401: respuestas.noAutorizado,
          404: respuestas.noEncontrado,
        },
      },
      delete: {
        tags: ['Productos'],
        summary: 'Eliminar producto',
        responses: {
          200: {
            description: 'Producto eliminado',
            content: {
              'application/json': {
                schema: { type: 'object', properties: { mensaje: { type: 'string' } } },
              },
            },
          },
          400: respuestas.invalido,
          401: respuestas.noAutorizado,
          404: respuestas.noEncontrado,
        },
      },
    },
    '/api/productos/{id}/descontar': {
      parameters: [parametroId],
      patch: {
        tags: ['Uso interno (pedidos)'],
        summary: 'Descontar existencia',
        description:
          'Lo llama el servicio de pedidos al confirmar una venta. No se expone en el broker.',
        requestBody: {
          required: true,
          content: { 'application/json': { schema: { $ref: '#/components/schemas/Descuento' } } },
        },
        responses: {
          200: productoJson('Existencia descontada'),
          400: respuestas.invalido,
          401: respuestas.noAutorizado,
          404: respuestas.noEncontrado,
          409: {
            description: 'No hay existencia suficiente',
            content: {
              'application/json': {
                schema: {
                  type: 'object',
                  properties: {
                    error: { type: 'string' },
                    existenciaDisponible: { type: 'integer' },
                    cantidadSolicitada: { type: 'integer' },
                  },
                },
              },
            },
          },
        },
      },
    },
    '/api/productos/{id}/reponer': {
      parameters: [parametroId],
      patch: {
        tags: ['Uso interno (pedidos)'],
        summary: 'Reponer existencia',
        description:
          'Lo llama el servicio de pedidos cuando un pedido reduce su cantidad. No se expone en el broker.',
        requestBody: {
          required: true,
          content: { 'application/json': { schema: { $ref: '#/components/schemas/Descuento' } } },
        },
        responses: {
          200: productoJson('Existencia repuesta'),
          400: respuestas.invalido,
          401: respuestas.noAutorizado,
          404: respuestas.noEncontrado,
        },
      },
    },
    '/health': {
      get: {
        tags: ['Estado'],
        summary: 'Estado del servicio',
        security: [],
        responses: { 200: { description: 'El servicio está activo' } },
      },
    },
  },
};
