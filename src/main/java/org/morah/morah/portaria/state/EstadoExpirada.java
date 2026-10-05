package org.morah.morah.portaria.state;

import java.time.Instant;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.DecisaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.springframework.stereotype.Component;

/**
 * O prazo de resposta acabou. Estado final: as mensagens explicam o que fazer (registrar o
 * visitante de novo, gerando um pedido novo para a unidade).
 */
@Component
public class EstadoExpirada implements EstadoDaAutorizacao {

    private static final String REGISTRE_DE_NOVO = " Peca a portaria para registrar o visitante novamente.";

    @Override
    public StatusAutorizacaoVisita status() {
        return StatusAutorizacaoVisita.EXPIRADA;
    }

    @Override
    public void decidir(AutorizacaoVisita autorizacao, DecisaoVisita decisao, Instant agora) {
        throw new RegraDeNegocioException("O prazo para responder a esta autorizacao acabou." + REGISTRE_DE_NOVO);
    }

    @Override
    public void reenviar(AutorizacaoVisita autorizacao, Instant agora) {
        throw new RegraDeNegocioException(
                "Esta autorizacao expirou e nao pode ser reenviada. Registre o visitante novamente.");
    }

    @Override
    public void registrarEntrada(AutorizacaoVisita autorizacao, Instant agora) {
        throw new RegraDeNegocioException("Esta autorizacao expirou sem resposta da unidade." + REGISTRE_DE_NOVO);
    }
}
