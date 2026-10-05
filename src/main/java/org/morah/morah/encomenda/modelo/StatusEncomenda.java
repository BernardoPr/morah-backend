package org.morah.morah.encomenda.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Schema StatusEncomenda do contrato.
 *
 * <p>Ciclo de vida: toda encomenda nasce {@link #AGUARDANDO_RETIRADA} (registrada pela portaria)
 * e termina {@link #RETIRADA} (entregue a alguem com codigo valido) ou {@link #EXTRAVIADA}
 * (morador abriu ocorrencia de extravio antes de retirar).
 */
public enum StatusEncomenda {

    AGUARDANDO_RETIRADA("aguardando_retirada"),
    RETIRADA("retirada"),
    EXTRAVIADA("extraviada");

    private final String valor;

    StatusEncomenda(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusEncomenda de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
