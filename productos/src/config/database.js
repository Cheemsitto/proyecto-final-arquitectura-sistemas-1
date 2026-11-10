const { Sequelize } = require('sequelize');
const config = require('./index');

const sequelize = new Sequelize(config.db.nombre, config.db.usuario, config.db.password, {
  host: config.db.host,
  port: config.db.puerto,
  dialect: 'mysql',
  logging: false,
  timezone: '+00:00',
  dialectOptions: config.db.ssl ? { ssl: config.db.ssl } : {},
});

module.exports = sequelize;
