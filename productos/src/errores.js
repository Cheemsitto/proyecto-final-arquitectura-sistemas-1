class ValidacionError extends Error {
  constructor(mensaje, detalles = []) {
    super(mensaje);
    this.name = 'ValidacionError';
    this.detalles = detalles;
  }
}

class NoEncontradoError extends Error {
  constructor(mensaje) {
    super(mensaje);
    this.name = 'NoEncontradoError';
  }
}

class ExistenciaInsuficienteError extends Error {
  constructor(id, disponible, solicitada) {
    super(`Existencia insuficiente para el producto ${id}`);
    this.name = 'ExistenciaInsuficienteError';
    this.disponible = disponible;
    this.solicitada = solicitada;
  }
}

module.exports = { ValidacionError, NoEncontradoError, ExistenciaInsuficienteError };
