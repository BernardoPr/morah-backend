package org.morah.morah.financeiro.dto;

import java.math.BigDecimal;

import org.morah.morah.financeiro.modelo.StatusCobranca;

/**
 * Uso interno (nao sai na API): como uma cobranca esta em uma data - status exibido, dias de
 * atraso e encargos. Calculada pela {@code CalculadoraDeEncargos} e usada para montar o
 * {@link CobrancaResponse}, o PIX e o resumo de inadimplencia.
 */
public record SituacaoDaCobranca(
        StatusCobranca status,
        long diasDeAtraso,
        BigDecimal multa,
        BigDecimal juros,
        BigDecimal valorTotal) {
}
