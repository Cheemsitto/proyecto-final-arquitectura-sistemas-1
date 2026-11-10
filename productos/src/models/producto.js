const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');
const { NoEncontradoError, ExistenciaInsuficienteError } = require('../errores');

// Modelo: representa la tabla productos y contiene las reglas que tocan los datos.
const Producto = sequelize.define(
  'Producto',
  {
    id: {
      type: DataTypes.INTEGER,
      autoIncrement: true,
      primaryKey: true,
    },
    nombre: {
      type: DataTypes.STRING(120),
      allowNull: false,
      validate: { notEmpty: true },
    },
    descripcion: {
      type: DataTypes.STRING(255),
      allowNull: true,
    },
    precio: {
      type: DataTypes.DECIMAL(10, 2),
      allowNull: false,
      validate: {
        mayorACero(valor) {
          if (!(Number(valor) > 0)) {
            throw new Error('El precio debe ser mayor a 0');
          }
        },
      },
    },
    existencia: {
      type: DataTypes.INTEGER,
      allowNull: false,
      // Después de una venta puede quedar en 0, nunca en negativo.
      validate: { isInt: true, min: 0 },
    },
  },
  {
    tableName: 'productos',
    underscored: true,
    createdAt: 'creadoEn',
    updatedAt: 'actualizadoEn',
  },
);

/**
 * Descuenta existencia de un producto. Lee la fila con bloqueo (SELECT ... FOR UPDATE)
 * dentro de una transacción para que dos pedidos simultáneos no vendan el mismo stock.
 */
Producto.descontarExistencia = async function descontarExistencia(id, cantidad) {
  return sequelize.transaction(async (transaction) => {
    const producto = await Producto.findByPk(id, {
      transaction,
      lock: transaction.LOCK.UPDATE,
    });

    if (!producto) {
      throw new NoEncontradoError(`No existe el producto con id ${id}`);
    }
    if (producto.existencia < cantidad) {
      throw new ExistenciaInsuficienteError(id, producto.existencia, cantidad);
    }

    producto.existencia -= cantidad;
    await producto.save({ transaction });
    return producto;
  });
};

module.exports = Producto;
