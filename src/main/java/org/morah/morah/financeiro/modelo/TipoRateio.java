package org.morah.morah.financeiro.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Como o valor de uma taxa e dividido entre as unidades (propriedade "tipoRateio" do schema
 * TaxaCreateRequest). Cada valor tem uma Strategy em {@code financeiro.strategy}.
 */
public enum TipoRateio {

    /** Todas as unidades ativas pagam o mesmo valor. */
    IGUALITARIO("igualitario"),

    /** Cada unidade paga proporcionalmente a sua fracao ideal no condominio. */
    FRACAO_IDEAL("fracao_ideal");

    private final String valor;

    TipoRateio(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoRateio de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
