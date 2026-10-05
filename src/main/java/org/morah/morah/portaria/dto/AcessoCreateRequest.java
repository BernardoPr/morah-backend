package org.morah.morah.portaria.dto;

import org.morah.morah.portaria.modelo.TipoAcesso;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo do POST /portaria/acessos (schema AcessoCreateRequest).
 *
 * <p>{@code autorizacaoId} e opcional: na entrada, se nao vier, a API procura a autorizacao
 * concedida mais recente do visitante; na saida ele e ignorado (fecha-se o acesso em aberto).
 */
public record AcessoCreateRequest(
        Long autorizacaoId,
        @NotNull(message = "informe o visitante") Long visitanteId,
        @NotNull(message = "informe o tipo (entrada ou saida)") TipoAcesso tipo,
        String fotoUrl) {
}
