package org.morah.morah.unidade.dto;

import org.morah.morah.unidade.modelo.DecisaoSolicitacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PATCH /unidades/{unidadeId}/vinculos/solicitacoes/{solicitacaoId}
 * (objeto inline do contrato: {@code { "decisao": "aprovada|rejeitada", "justificativa": "..." }}).
 */
public record DecisaoSolicitacaoVinculoRequest(
        @NotNull(message = "informe a decisao (aprovada ou rejeitada)") DecisaoSolicitacao decisao,
        @Size(max = 500, message = "a justificativa deve ter no maximo 500 caracteres") String justificativa) {
}
