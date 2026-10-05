package org.morah.morah.unidade.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Schema TipoVinculo do contrato: como a pessoa esta ligada a unidade. */
public enum TipoVinculo {

    PROPRIETARIO("proprietario"),
    INQUILINO("inquilino"),
    DEPENDENTE("dependente");

    private final String valor;

    TipoVinculo(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoVinculo de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
