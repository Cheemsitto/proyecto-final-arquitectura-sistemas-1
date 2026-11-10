const express = require('express');
const swaggerUi = require('swagger-ui-express');
const config = require('./config');
const especificacion = require('./docs/openapi');
const rutasProductos = require('./routes/productoRoutes');
const claveServicio = require('./middlewares/claveServicio');
const { rutaNoEncontrada, manejarErrores } = require('./middlewares/manejoErrores');

const app = express();

app.disable('x-powered-by');
app.use(express.json());

app.get('/health', (req, res) => res.json({ servicio: 'productos', estado: 'ok' }));
app.get('/api-docs.json', (req, res) => res.json(especificacion));
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(especificacion));

app.use('/api/productos', claveServicio(config.claveServicio), rutasProductos);

app.use(rutaNoEncontrada);
app.use(manejarErrores);

module.exports = app;
