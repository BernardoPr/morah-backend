package org.morah.morah.notificacao.template;

import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Avisa os porteiros do condominio que o morador respondeu ao pedido de entrada
 * ("Entrada de Joao autorizada pela unidade Bloco A - Apto 101").
 */
@Component
public class NotificacaoDeDecisaoDeVisita extends NotificacaoTemplate<AutorizacaoVisita> {

    public NotificacaoDeDecisaoDeVisita(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.PORTARIA;
    }

    @Override
    protected String montarTitulo(AutorizacaoVisita autorizacao) {
        // status.getValor() ja vem no feminino: "autorizada" / "recusada".
        return "Entrada de %s %s".formatted(autorizacao.getVisitante().getNome(), autorizacao.getStatus().getValor());
    }

    @Override
    protected String montarMensagem(Usuario destinatario, AutorizacaoVisita autorizacao) {
        String mensagem = "Entrada de %s %s pela unidade %s.".formatted(
                autorizacao.getVisitante().getNome(),
                autorizacao.getStatus().getValor(),
                autorizacao.getUnidadeDescricao());
        if (autorizacao.getJustificativa() != null && !autorizacao.getJustificativa().isBlank()) {
            mensagem += " Justificativa: " + autorizacao.getJustificativa();
        }
        return mensagem;
    }

    @Override
    protected Long referenciaId(AutorizacaoVisita autorizacao) {
        return autorizacao.getId();
    }
}
