package org.morah.morah.portaria.state;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.springframework.stereotype.Component;

/**
 * Entrega o objeto-estado correspondente ao status gravado na autorizacao.
 *
 * <p>Mesmo truque dos seletores de Strategy do projeto: o Spring injeta a LISTA com todos os
 * {@link EstadoDaAutorizacao} e aqui ela vira um mapa (status -> estado). A diferenca e que a
 * chave nao vem da requisicao, e sim do proprio objeto: quem "escolhe" o comportamento e o estado
 * interno da autorizacao.
 *
 * <p>O construtor confere se todo status tem um estado: se alguem criar um status novo e esquecer
 * a classe, a aplicacao nem sobe (melhor que um erro 500 no meio da portaria).
 */
@Component
public class SeletorDeEstadoDaAutorizacao {

    private final Map<StatusAutorizacaoVisita, EstadoDaAutorizacao> estados =
            new EnumMap<>(StatusAutorizacaoVisita.class);

    public SeletorDeEstadoDaAutorizacao(List<EstadoDaAutorizacao> estadosDisponiveis) {
        estadosDisponiveis.forEach(estado -> estados.put(estado.status(), estado));

        for (StatusAutorizacaoVisita status : StatusAutorizacaoVisita.values()) {
            if (!estados.containsKey(status)) {
                throw new IllegalStateException("Nenhum estado cadastrado para o status " + status);
            }
        }
    }

    /** Estado atual da autorizacao (pelo status gravado). */
    public EstadoDaAutorizacao estadoDe(AutorizacaoVisita autorizacao) {
        return obter(autorizacao.getStatus());
    }

    public EstadoDaAutorizacao obter(StatusAutorizacaoVisita status) {
        return estados.get(status);
    }
}
