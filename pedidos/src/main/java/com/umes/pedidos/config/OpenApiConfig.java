package com.umes.pedidos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String ESQUEMA = "ClaveServicio";

	@Bean
	OpenAPI documentacionPedidos() {
		return new OpenAPI()
				.info(new Info()
						.title("Servicio de Pedidos")
						.version("1.0.0")
						.description("Servicio con arquitectura en capas (controller, service, repository y client). "
								+ "Las rutas /api/pedidos exigen la cabecera X-Service-Key; en el sistema completo "
								+ "la agrega el broker después de validar el token."))
				.components(new Components().addSecuritySchemes(ESQUEMA, new SecurityScheme()
						.type(SecurityScheme.Type.APIKEY)
						.in(SecurityScheme.In.HEADER)
						.name(ProductosClientConfig.CABECERA_CLAVE)))
				.addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
	}
}
