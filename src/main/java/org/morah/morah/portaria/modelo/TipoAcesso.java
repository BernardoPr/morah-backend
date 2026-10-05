package org.morah.morah.portaria.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Propriedade "tipo" do AcessoCreateRequest ("entrada" | "saida").
 *
 * <p>No schema Acesso o mesmo campo indica o ultimo movimento registrado naquele acesso.
 */
public enum TipoAcesso {

    ENTRADA("entrada"),
    SAIDA("saida");

    private final String valor;

    TipoAcesso(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static TipoAcesso de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
