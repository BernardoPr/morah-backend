package org.morah.morah.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configura o Swagger UI (disponivel em /v1/swagger-ui.html) e o botao "Authorize",
 * onde voce cola o accessToken devolvido por POST /auth/login.
 */
@Configuration
public class ConfiguracaoOpenApi {

    private static final String ESQUEMA = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Morah API")
                        .version("1.0.0")
                        .description("""
                                Base da API do sistema Morah (Spring Boot + MongoDB).
                                O contrato completo acordado com o front esta em Documentos/morah-api.yaml.
                                """))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA))
                .components(new Components().addSecuritySchemes(ESQUEMA,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
