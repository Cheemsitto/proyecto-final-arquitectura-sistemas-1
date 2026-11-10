package com.umes.pedidos.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaveServicioFilterTest {

	private final ClaveServicioFilter filtro = new ClaveServicioFilter("clave-test");

	private MockHttpServletResponse ejecutar(String ruta, String clave, MockFilterChain cadena) throws Exception {
		MockHttpServletRequest peticion = new MockHttpServletRequest("GET", ruta);
		if (clave != null) {
			peticion.addHeader("X-Service-Key", clave);
		}
		MockHttpServletResponse respuesta = new MockHttpServletResponse();
		filtro.doFilter(peticion, respuesta, cadena);
		return respuesta;
	}

	@Test
	void rechazaPeticionSinClave() throws Exception {
		MockFilterChain cadena = new MockFilterChain();

		MockHttpServletResponse respuesta = ejecutar("/api/pedidos", null, cadena);

		assertThat(respuesta.getStatus()).isEqualTo(401);
		assertThat(respuesta.getContentAsString()).contains("Clave de servicio");
		assertThat(cadena.getRequest()).isNull();
	}

	@Test
	void rechazaClaveIncorrecta() throws Exception {
		MockFilterChain cadena = new MockFilterChain();

		assertThat(ejecutar("/api/pedidos", "otra", cadena).getStatus()).isEqualTo(401);
		assertThat(cadena.getRequest()).isNull();
	}

	@Test
	void dejaPasarConLaClaveCorrecta() throws Exception {
		MockFilterChain cadena = new MockFilterChain();

		assertThat(ejecutar("/api/pedidos", "clave-test", cadena).getStatus()).isEqualTo(200);
		assertThat(cadena.getRequest()).isNotNull();
	}

	@Test
	void noExigeClaveEnSwaggerNiHealth() throws Exception {
		MockFilterChain swagger = new MockFilterChain();
		MockFilterChain salud = new MockFilterChain();

		ejecutar("/swagger-ui/index.html", null, swagger);
		ejecutar("/health", null, salud);

		assertThat(swagger.getRequest()).isNotNull();
		assertThat(salud.getRequest()).isNotNull();
	}

	@Test
	void noArrancaSinClaveConfigurada() {
		assertThatThrownBy(() -> new ClaveServicioFilter(" ")).isInstanceOf(IllegalStateException.class);
	}
}
