package org.morah.morah.financeiro.strategy;

import java.math.BigDecimal;

/**
 * Resultado do rateio para UMA unidade: o valor que ela deve pagar ou o motivo de ter ficado de
 * fora. Use as fabricas {@link #ok} e {@link #falha} em vez do construtor.
 */
public record ParcelaDoRateio(Long unidadeId, BigDecimal valor, String motivoDaFalha) {

    public static ParcelaDoRateio ok(Long unidadeId, BigDecimal valor) {
        return new ParcelaDoRateio(unidadeId, valor, null);
    }

    public static ParcelaDoRateio falha(Long unidadeId, String motivo) {
        return new ParcelaDoRateio(unidadeId, null, motivo);
    }

    public boolean falhou() {
        return motivoDaFalha != null;
    }
}
