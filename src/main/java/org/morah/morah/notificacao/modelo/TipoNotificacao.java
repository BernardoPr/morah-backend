package org.morah.morah.notificacao.modelo;

import com.fasterxml.jackson.annotation.JsonValue;

/** Tipos previstos no contrato (schema Notificacao -> propriedade "tipo"). */
public enum TipoNotificacao {

    PORTARIA("portaria"),
    ENCOMENDA("encomenda"),
    FINANCEIRO("financeiro"),
    AVISO("aviso"),
    RESERVA("reserva");

    private final String valor;

    TipoNotificacao(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }
}
