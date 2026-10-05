package org.morah.morah.portaria.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Schema StatusAutorizacaoVisita do contrato.
 *
 * <p>Cada valor tem um objeto-estado correspondente no pacote {@code portaria.state}, que sabe
 * quais operacoes a autorizacao aceita naquele momento (padrao State).
 */
public enum StatusAutorizacaoVisita {

    /** Aguardando a resposta do morador (dentro do prazo). */
    PENDENTE("pendente"),
    /** O morador liberou a entrada. */
    AUTORIZADA("autorizada"),
    /** O morador negou a entrada. */
    RECUSADA("recusada"),
    /** O prazo de resposta acabou sem decisao. */
    EXPIRADA("expirada");

    private final String valor;

    StatusAutorizacaoVisita(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusAutorizacaoVisita de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
