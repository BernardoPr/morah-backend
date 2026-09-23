package org.morah.morah.login.dto;

import org.morah.morah.comum.modelo.Perfil;

import jakarta.validation.constraints.NotNull;

/** Corpo do POST /auth/contexto: troca o par (condominio, perfil) ativo. */
public record SelecionarContextoRequest(
        @NotNull(message = "informe o condominioId") Long condominioId,
        @NotNull(message = "informe o perfil") Perfil perfil) {
}
