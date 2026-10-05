package org.morah.morah.financeiro.dto;

import java.math.BigDecimal;

import org.morah.morah.financeiro.modelo.Taxa;
import org.morah.morah.financeiro.modelo.TipoRateio;

/** Resposta do contrato (schema Taxa = campos do TaxaCreateRequest + id). */
public record TaxaResponse(
        Long id,
        String descricao,
        String competencia,
        BigDecimal valor,
        TipoRateio tipoRateio,
        Integer vencimentoDia) {

    public static TaxaResponse de(Taxa taxa) {
        return new TaxaResponse(
                taxa.getId(),
                taxa.getDescricao(),
                taxa.getCompetencia(),
                taxa.getValor(),
                taxa.getTipoRateio(),
                taxa.getVencimentoDia());
    }
}
