package org.morah.morah.ocorrencia.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Propriedade "status" do schema Ocorrencia. */
public enum StatusOcorrencia {

    ABERTA("aberta"),
    EM_ANDAMENTO("em_andamento"),
    ENCERRADA("encerrada");

    private final String valor;

    StatusOcorrencia(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusOcorrencia de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
