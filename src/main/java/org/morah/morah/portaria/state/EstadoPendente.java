package org.morah.morah.portaria.state;

import java.time.Instant;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.DecisaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.springframework.stereotype.Component;

/**
 * Aguardando a resposta do morador. E o unico estado com transicoes de saida:
 * <pre>
 *   PENDENTE --decidir(autorizado)--> AUTORIZADA
 *   PENDENTE --decidir(recusado)----> RECUSADA
 *   PENDENTE --prazo vencido--------> EXPIRADA
 *   PENDENTE --reenviar-------------> PENDENTE (com prazo renovado)
 * </pre>
 */
@Component
public class EstadoPendente implements EstadoDaAutorizacao {

    @Override
    public StatusAutorizacaoVisita status() {
        return StatusAutorizacaoVisita.PENDENTE;
    }

    @Override
    public boolean expirarSeVencida(AutorizacaoVisita autorizacao, Instant agora) {
        if (!autorizacao.estaVencida(agora)) {
            return false;
        }
        autorizacao.setStatus(StatusAutorizacaoVisita.EXPIRADA);
        return true;
    }

    /** Transicao principal: a decisao do morador define o proximo estado. */
    @Override
    public void decidir(AutorizacaoVisita autorizacao, DecisaoVisita decisao, Instant agora) {
        autorizacao.setStatus(decisao.statusResultante());
        autorizacao.setRespondidoEm(agora);
    }

    /**
     * Continua pendente, mas com um prazo novo de resposta: se o porteiro precisou reenviar, o
     * morador provavelmente nao viu a primeira notificacao, e manter o prazo antigo faria o pedido
     * expirar logo depois do lembrete.
     */
    @Override
    public void reenviar(AutorizacaoVisita autorizacao, Instant agora) {
        autorizacao.abrirPrazoDeResposta(agora);
    }

    @Override
    public void registrarEntrada(AutorizacaoVisita autorizacao, Instant agora) {
        throw new RegraDeNegocioException(
                "A unidade ainda nao respondeu a esta autorizacao. Aguarde a decisao do morador ou reenvie o pedido.");
    }
}
