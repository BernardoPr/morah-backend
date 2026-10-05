package org.morah.morah.portaria.dto;

import org.morah.morah.portaria.modelo.DecisaoVisita;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo do PATCH /portaria/autorizacoes/{id}/decisao (schema DecisaoAutorizacaoRequest).
 * Valor fora do enum ("talvez") vira 400 no {@code TratadorGlobalDeErros}.
 */
public record DecisaoAutorizacaoRequest(
        @NotNull(message = "informe a decisao (autorizado ou recusado)") DecisaoVisita decisao,
        String justificativa) {
}
