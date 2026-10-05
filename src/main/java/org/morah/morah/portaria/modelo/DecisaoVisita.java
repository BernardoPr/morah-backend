package org.morah.morah.portaria.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Propriedade "decisao" do schema DecisaoAutorizacaoRequest ("autorizado" | "recusado").
 *
 * <p>E um enum separado do {@link StatusAutorizacaoVisita} porque o contrato usa palavras
 * diferentes (o morador responde "autorizado"; a autorizacao fica "autorizada") e porque o
 * morador so pode escolher dois dos quatro status. Cada decisao sabe em qual status resulta.
 */
public enum DecisaoVisita {

    AUTORIZADO("autorizado", StatusAutorizacaoVisita.AUTORIZADA),
    RECUSADO("recusado", StatusAutorizacaoVisita.RECUSADA);

    private final String valor;
    private final StatusAutorizacaoVisita statusResultante;

    DecisaoVisita(String valor, StatusAutorizacaoVisita statusResultante) {
        this.valor = valor;
        this.statusResultante = statusResultante;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    /** Status que a autorizacao assume depois desta decisao. */
    public StatusAutorizacaoVisita statusResultante() {
        return statusResultante;
    }

    @JsonCreator
    public static DecisaoVisita de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
