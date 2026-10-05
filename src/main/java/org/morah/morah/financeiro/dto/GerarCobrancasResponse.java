package org.morah.morah.financeiro.dto;

import java.util.List;

/**
 * Resposta do contrato (schema GerarCobrancasResponse): quantas cobrancas foram geradas e,
 * para cada unidade que ficou de fora, o motivo.
 */
public record GerarCobrancasResponse(Long taxaId, int totalGeradas, List<Falha> falhas) {

    /** Item do array "falhas": unidade que nao recebeu cobranca e por que. */
    public record Falha(Long unidadeId, String motivo) {
    }
}
