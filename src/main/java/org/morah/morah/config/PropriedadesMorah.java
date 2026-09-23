package org.morah.morah.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Espelha o bloco "morah:" do application.yaml em um objeto Java.
 * Assim ninguem precisa espalhar {@code @Value("${...}")} pelo codigo.
 */
@ConfigurationProperties(prefix = "morah")
public record PropriedadesMorah(Jwt jwt, Cors cors, boolean cargaInicial) {

    public record Jwt(String segredo, long minutosToken, long diasRefresh) {
    }

    public record Cors(List<String> origens) {
    }
}
