package org.morah.morah.notificacao.template;

import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Avisa os moradores da unidade que uma encomenda chegou na portaria.
 *
 * <p>E por esta notificacao que o morador recebe o codigo de retirada: a portaria registra o
 * pacote, mas nunca ve o codigo.
 */
@Component
public class NotificacaoDeEncomendaRecebida extends NotificacaoTemplate<Encomenda> {

    public NotificacaoDeEncomendaRecebida(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.ENCOMENDA;
    }

    @Override
    protected String montarTitulo(Encomenda encomenda) {
        return "Encomenda na portaria";
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Encomenda encomenda) {
        return "Encomenda da %s chegou. Codigo de retirada: %s"
                .formatted(encomenda.getTransportadora(), encomenda.getCodigoRetirada());
    }

    @Override
    protected Long referenciaId(Encomenda encomenda) {
        return encomenda.getId();
    }
}
