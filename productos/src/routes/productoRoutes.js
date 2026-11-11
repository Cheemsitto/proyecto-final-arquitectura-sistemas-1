const { Router } = require('express');
const controlador = require('../controllers/productoController');

const router = Router();

router.get('/', controlador.listar);
router.get('/:id', controlador.obtener);
router.post('/', controlador.crear);
router.put('/:id', controlador.actualizar);
router.delete('/:id', controlador.eliminar);
router.patch('/:id/descontar', controlador.descontar);
router.patch('/:id/reponer', controlador.reponer);

module.exports = router;
