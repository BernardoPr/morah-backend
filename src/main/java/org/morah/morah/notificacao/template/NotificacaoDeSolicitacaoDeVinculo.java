package org.morah.morah.notificacao.template;

import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/** Avisa o sindico que um morador pediu a inclusao de alguem na unidade (precisa de aprovacao). */
@Component
public class NotificacaoDeSolicitacaoDeVinculo extends NotificacaoTemplate<SolicitacaoVinculo> {

    public NotificacaoDeSolicitacaoDeVinculo(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    /** O contrato nao tem um tipo "unidade"; o mais proximo e um aviso para o sindico. */
    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.AVISO;
    }

    @Override
    protected String montarTitulo(SolicitacaoVinculo solicitacao) {
        return "Nova solicitacao de vinculo - " + solicitacao.getUnidadeIdentificacao();
    }

    @Override
    protected String montarMensagem(Usuario destinatario, SolicitacaoVinculo solicitacao) {
        return "%s pediu a inclusao de %s como %s. Aprove ou rejeite a solicitacao no app."
                .formatted(solicitacao.getSolicitanteNome(),
                        solicitacao.getNomeSolicitado(),
                        solicitacao.getTipoVinculo().getValor());
    }

    @Override
    protected Long referenciaId(SolicitacaoVinculo solicitacao) {
        return solicitacao.getId();
    }
}
