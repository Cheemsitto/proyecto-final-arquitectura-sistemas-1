package com.umes.pedidos.exception;

import com.umes.pedidos.dto.ErrorRespuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/** Traduce las excepciones de negocio y de comunicación a respuestas HTTP. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler({ ProductoNoEncontradoException.class, PedidoNoEncontradoException.class })
	public ResponseEntity<ErrorRespuesta> noEncontrado(RuntimeException ex) {
		return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(StockInsuficienteException.class)
	public ResponseEntity<ErrorRespuesta> stockInsuficiente(StockInsuficienteException ex) {
		return respuesta(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(ProductosAutenticacionException.class)
	public ResponseEntity<ErrorRespuesta> autenticacionProductos(ProductosAutenticacionException ex) {
		log.warn("Productos rechazó la clave de servicio");
		return respuesta(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(ProductosTiempoAgotadoException.class)
	public ResponseEntity<ErrorRespuesta> tiempoAgotado(ProductosTiempoAgotadoException ex) {
		log.warn("Productos no respondió a tiempo");
		return respuesta(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
	}

	@ExceptionHandler(ProductosNoDisponibleException.class)
	public ResponseEntity<ErrorRespuesta> productosNoDisponible(ProductosNoDisponibleException ex) {
		log.warn("Productos no disponible: {}", ex.getMessage());
		return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorRespuesta> datosInvalidos(MethodArgumentNotValidException ex) {
		List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getDefaultMessage())
				.sorted()
				.toList();
		return ResponseEntity.badRequest().body(new ErrorRespuesta("Datos de pedido inválidos", detalles));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorRespuesta> jsonInvalido(HttpMessageNotReadableException ex) {
		return respuesta(HttpStatus.BAD_REQUEST,
				"El cuerpo de la petición no es un JSON válido o tiene tipos incorrectos");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorRespuesta> parametroInvalido(MethodArgumentTypeMismatchException ex) {
		return respuesta(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un formato inválido");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorRespuesta> otroError(Exception ex) {
		// Errores propios de Spring MVC (ruta inexistente, método no permitido, etc.)
		if (ex instanceof ErrorResponse errorSpring) {
			String detalle = errorSpring.getBody().getDetail();
			return ResponseEntity.status(errorSpring.getStatusCode())
					.body(new ErrorRespuesta(detalle != null ? detalle : ex.getMessage()));
		}
		log.error("Error no controlado", ex);
		return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servicio de pedidos");
	}

	private ResponseEntity<ErrorRespuesta> respuesta(HttpStatus estado, String mensaje) {
		return ResponseEntity.status(estado).body(new ErrorRespuesta(mensaje));
	}
}
