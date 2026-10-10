package org.example.crm.admin.infrastructure.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Metadatos OpenAPI de la API de administración. */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "CRM Platform — API de administración",
            version = "v1",
            description =
                "APIs REST de administración. La Marca se selecciona con la cabecera X-Brand o el"
                    + " parámetro brand (código o id)."))
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
public class OpenApiConfiguration {}
