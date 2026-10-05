package org.morah.morah.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;

/**
 * Resposta do contrato (schema Cobranca).
 *
 * <p>Diferente dos outros {@code de(...)}, este recebe tambem a {@link SituacaoDaCobranca}:
 * multa, juros, valor total e o status "atrasado" nao estao gravados na entidade, sao
 * calculados para o dia da consulta.
 *
 * <p>{@code unidadeId} nao esta no schema do contrato; foi acrescentado porque, na lista do
 * sindico (que ve o condominio inteiro), sem ele nao da para saber de quem e cada boleto.
 */
public record CobrancaResponse(
        Long id,
        Long unidadeId,
        String taxaDescricao,
        String competencia,
        LocalDate vencimento,
        BigDecimal valorOriginal,
        BigDecimal multa,
        BigDecimal juros,
        BigDecimal valorTotal,
        StatusCobranca status) {

    public static CobrancaResponse de(Cobranca cobranca, SituacaoDaCobranca situacao) {
        return new CobrancaResponse(
                cobranca.getId(),
                cobranca.getUnidadeId(),
                cobranca.getTaxaDescricao(),
                cobranca.getCompetencia(),
                cobranca.getVencimento(),
                cobranca.getValorOriginal(),
                situacao.multa(),
                situacao.juros(),
                situacao.valorTotal(),
                situacao.status());
    }
}
