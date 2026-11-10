const config = require('./config');
const app = require('./app');
const sequelize = require('./config/database');
const crearBaseSiNoExiste = require('./config/crearBase');
require('./models/producto');

async function iniciar() {
  if (!config.claveServicio) {
    throw new Error('Falta la variable de entorno SERVICE_KEY');
  }

  await crearBaseSiNoExiste();
  await sequelize.authenticate();
  if (config.db.sincronizar) {
    await sequelize.sync();
  }

  const alIniciar = () => {
    console.log(`Servicio de productos escuchando en el puerto ${config.puerto}`);
    console.log(`Swagger: http://localhost:${config.puerto}/api-docs`);
  };
  if (config.host) {
    app.listen(config.puerto, config.host, alIniciar);
  } else {
    app.listen(config.puerto, alIniciar);
  }
}

iniciar().catch((err) => {
  console.error('No se pudo iniciar el servicio de productos:', err.message);
  process.exit(1);
});
