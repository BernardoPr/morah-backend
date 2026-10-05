package org.morah.morah.notificacao.template;

import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.ocorrencia.modelo.Ocorrencia;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/** Avisa o sindico que uma ocorrencia foi aberta (extravio de encomenda ou dano em reserva). */
@Component
public class NotificacaoDeOcorrenciaAberta extends NotificacaoTemplate<Ocorrencia> {

    public NotificacaoDeOcorrenciaAberta(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    /** O contrato nao tem um tipo "ocorrencia"; o mais proximo e um aviso para o sindico. */
    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.AVISO;
    }

    @Override
    protected String montarTitulo(Ocorrencia ocorrencia) {
        return "Nova ocorrencia: " + ocorrencia.getTitulo();
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Ocorrencia ocorrencia) {
        return "%s abriu uma ocorrencia: %s".formatted(ocorrencia.getAbertaPorNome(), ocorrencia.getDescricao());
    }

    @Override
    protected Long referenciaId(Ocorrencia ocorrencia) {
        return ocorrencia.getId();
    }
}
