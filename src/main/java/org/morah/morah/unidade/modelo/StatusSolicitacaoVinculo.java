package org.morah.morah.unidade.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Propriedade "status" do schema SolicitacaoVinculo. */
public enum StatusSolicitacaoVinculo {

    PENDENTE("pendente"),
    APROVADA("aprovada"),
    REJEITADA("rejeitada");

    private final String valor;

    StatusSolicitacaoVinculo(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusSolicitacaoVinculo de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
