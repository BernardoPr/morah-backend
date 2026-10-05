package org.morah.morah.portaria.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Propriedade "status" do schema Acesso.
 *
 * <p>O contrato declara o campo apenas como {@code string}; estes sao os valores que a API usa.
 */
public enum StatusAcesso {

    /** O visitante entrou e ainda nao saiu. */
    EM_ANDAMENTO("em_andamento"),
    /** A saida ja foi registrada. */
    ENCERRADO("encerrado");

    private final String valor;

    StatusAcesso(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusAcesso de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
