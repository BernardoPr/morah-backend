package org.morah.morah.notificacao.template;

import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.StatusSolicitacaoVinculo;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Responde ao morador que pediu o vinculo: o sindico aprovou ou rejeitou.
 * Uma classe so para as duas respostas - o texto muda conforme o status da solicitacao.
 */
@Component
public class NotificacaoDeDecisaoDeVinculo extends NotificacaoTemplate<SolicitacaoVinculo> {

    public NotificacaoDeDecisaoDeVinculo(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    /** O contrato nao tem um tipo "unidade"; o mais proximo e um aviso. */
    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.AVISO;
    }

    @Override
    protected String montarTitulo(SolicitacaoVinculo solicitacao) {
        return foiAprovada(solicitacao)
                ? "Solicitacao de vinculo aprovada"
                : "Solicitacao de vinculo rejeitada";
    }

    @Override
    protected String montarMensagem(Usuario destinatario, SolicitacaoVinculo solicitacao) {
        String mensagem = foiAprovada(solicitacao)
                ? "%s agora consta como %s da unidade %s."
                : "O pedido para incluir %s como %s na unidade %s nao foi aprovado.";

        mensagem = mensagem.formatted(solicitacao.getNomeSolicitado(),
                solicitacao.getTipoVinculo().getValor(),
                solicitacao.getUnidadeIdentificacao());

        String justificativa = solicitacao.getJustificativa();
        return justificativa == null || justificativa.isBlank()
                ? mensagem
                : mensagem + " Justificativa: " + justificativa;
    }

    @Override
    protected Long referenciaId(SolicitacaoVinculo solicitacao) {
        return solicitacao.getId();
    }

    private boolean foiAprovada(SolicitacaoVinculo solicitacao) {
        return solicitacao.getStatus() == StatusSolicitacaoVinculo.APROVADA;
    }
}
