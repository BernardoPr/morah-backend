package org.morah.morah.unidade.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Propriedade "decisao" do corpo do PATCH /unidades/{unidadeId}/vinculos/solicitacoes/{id}.
 *
 * <p>Enum separado de {@link StatusSolicitacaoVinculo} de proposito: o sindico so pode
 * responder "aprovada" ou "rejeitada". Se reaproveitassemos o status, "pendente" seria
 * aceito pelo Jackson e precisariamos de um {@code if} a mais para barra-lo. Valor fora da
 * lista (ex.: "talvez") vira 400 no {@code TratadorGlobalDeErros}.
 */
public enum DecisaoSolicitacao {

    APROVADA("aprovada"),
    REJEITADA("rejeitada");

    private final String valor;

    DecisaoSolicitacao(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static DecisaoSolicitacao de(String valor) {
        return valueOf(valor.toUpperCase());
    }

    /** Status que a solicitacao assume depois da decisao. */
    public StatusSolicitacaoVinculo comoStatus() {
        return this == APROVADA ? StatusSolicitacaoVinculo.APROVADA : StatusSolicitacaoVinculo.REJEITADA;
    }
}
