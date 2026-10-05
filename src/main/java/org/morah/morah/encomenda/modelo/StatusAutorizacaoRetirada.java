package org.morah.morah.encomenda.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Propriedade "status" do schema AutorizacaoRetirada.
 *
 * <p>{@link #EXPIRADA} e gravado no banco apenas quando a autorizacao deixa de valer por
 * outro motivo (a encomenda foi retirada com outro codigo). A expiracao por tempo nao precisa
 * de rotina agendada: uma autorizacao {@link #ATIVA} com a validade vencida ja e exibida como
 * expirada (veja {@link AutorizacaoRetirada#statusEm}).
 */
public enum StatusAutorizacaoRetirada {

    ATIVA("ativa"),
    UTILIZADA("utilizada"),
    EXPIRADA("expirada");

    private final String valor;

    StatusAutorizacaoRetirada(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusAutorizacaoRetirada de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
