package org.morah.morah.aviso.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Schema PrioridadeAviso do contrato. */
public enum PrioridadeAviso {

    NORMAL("normal"),
    URGENTE("urgente");

    private final String valor;

    PrioridadeAviso(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static PrioridadeAviso de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
