package org.morah.morah.reserva.modelo;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Propriedade "momento" do schema VistoriaRequest: vistoria ao entregar ou ao receber o espaco. */
public enum MomentoVistoria {

    ENTRADA("entrada"),
    SAIDA("saida");

    private final String valor;

    MomentoVistoria(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static MomentoVistoria de(String valor) {
        return valueOf(valor.toUpperCase(Locale.ROOT));
    }
}
