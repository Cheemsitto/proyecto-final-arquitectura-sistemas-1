package com.umes.pedidos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Solo deja pasar a /api/** las peticiones que traen la clave interna que agrega el broker.
 * Así el cliente no puede saltarse el broker y llamar al servicio directamente.
 */
@Component
public class ClaveServicioFilter extends OncePerRequestFilter {

	private final byte[] huellaEsperada;

	public ClaveServicioFilter(@Value("${seguridad.service-key}") String claveEsperada) {
		if (claveEsperada == null || claveEsperada.isBlank()) {
			throw new IllegalStateException("Falta la variable de entorno SERVICE_KEY");
		}
		this.huellaEsperada = huella(claveEsperada);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String recibida = request.getHeader(ProductosClientConfig.CABECERA_CLAVE);
		if (recibida == null || !MessageDigest.isEqual(huella(recibida), huellaEsperada)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.getWriter().write("{\"error\":\"Clave de servicio inválida o ausente\"}");
			return;
		}
		chain.doFilter(request, response);
	}

	private static byte[] huella(String texto) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
