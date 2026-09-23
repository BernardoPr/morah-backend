package org.morah.morah.notificacao.template;

import org.morah.morah.aviso.modelo.Aviso;
import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Notificacao disparada quando o sindico publica um aviso no mural.
 * Segunda implementacao do {@link NotificacaoTemplate}.
 */
@Component
public class NotificacaoDeAvisoPublicado extends NotificacaoTemplate<Aviso> {

    private static final int TAMANHO_DA_PREVIA = 120;

    public NotificacaoDeAvisoPublicado(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.AVISO;
    }

    @Override
    protected String montarTitulo(Aviso aviso) {
        return (aviso.getPrioridade() == PrioridadeAviso.URGENTE ? "[URGENTE] " : "") + aviso.getTitulo();
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Aviso aviso) {
        String conteudo = aviso.getConteudo();
        return conteudo.length() <= TAMANHO_DA_PREVIA
                ? conteudo
                : conteudo.substring(0, TAMANHO_DA_PREVIA) + "...";
    }

    @Override
    protected Long referenciaId(Aviso aviso) {
        return aviso.getId();
    }

    /** Hook: aviso urgente tambem vai por SMS, para alcancar quem nao usa o app. */
    @Override
    protected CanalNotificacao escolherCanal(Aviso aviso) {
        return aviso.getPrioridade() == PrioridadeAviso.URGENTE
                ? CanalNotificacao.SMS
                : CanalNotificacao.PUSH;
    }
}
