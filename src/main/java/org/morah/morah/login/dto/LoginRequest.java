package org.morah.morah.login.dto;

import org.morah.morah.comum.modelo.Perfil;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo do POST /auth/login.
 *
 * <p>{@code condominioId} e {@code perfil} sao opcionais: se o usuario tiver mais de um
 * vinculo e nao escolher nenhum, a API usa o primeiro e ele pode trocar depois em
 * POST /auth/contexto.
 */
public record LoginRequest(
        @NotBlank(message = "informe o CPF") String cpf,
        @NotBlank(message = "informe a senha") String senha,
        Long condominioId,
        Perfil perfil) {
}
