const { validarProducto, validarCantidad, validarId } = require('../src/models/reglasProducto');
const { ValidacionError } = require('../src/errores');

describe('validarProducto', () => {
  test('acepta un producto válido y limpia el texto', () => {
    expect(
      validarProducto({ nombre: ' Mouse ', descripcion: ' Óptico ', precio: 10.5, existencia: 3 }),
    ).toEqual({ nombre: 'Mouse', descripcion: 'Óptico', precio: 10.5, existencia: 3 });
  });

  test('reúne todos los errores en una sola respuesta', () => {
    try {
      validarProducto({ nombre: '', precio: 0, existencia: 0 });
      throw new Error('debió fallar');
    } catch (err) {
      expect(err).toBeInstanceOf(ValidacionError);
      expect(err.detalles).toEqual([
        'El nombre es obligatorio',
        'El precio debe ser mayor a 0',
        'La existencia debe ser mayor a 0',
      ]);
    }
  });

  test('rechaza precios con más de dos decimales', () => {
    expect(() => validarProducto({ nombre: 'X', precio: 1.999, existencia: 1 })).toThrow(ValidacionError);
  });

  test('rechaza precio enviado como texto', () => {
    expect(() => validarProducto({ nombre: 'X', precio: '10', existencia: 1 })).toThrow(ValidacionError);
  });

  test('rechaza un cuerpo vacío', () => {
    expect(() => validarProducto(undefined)).toThrow(ValidacionError);
  });
});

describe('validarCantidad y validarId', () => {
  test.each([0, -1, 1.5, '2', null])('cantidad inválida: %p', (valor) => {
    expect(() => validarCantidad(valor)).toThrow(ValidacionError);
  });

  test('cantidad válida', () => {
    expect(validarCantidad(4)).toBe(4);
  });

  test('id válido desde texto', () => {
    expect(validarId('12')).toBe(12);
  });

  test.each(['0', 'abc', '-3', '1.5'])('id inválido: %p', (valor) => {
    expect(() => validarId(valor)).toThrow(ValidacionError);
  });
});
