package org.morah.morah.notificacao.template;

import java.time.format.DateTimeFormatter;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Avisa os moradores da unidade que ha um visitante na portaria esperando resposta.
 * Disparada no registro do visitante e de novo a cada "reenviar" do porteiro.
 */
@Component
public class NotificacaoDeVisitanteAguardando extends NotificacaoTemplate<AutorizacaoVisita> {

    /** Hora local do condominio (o servidor roda em UTC). */
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm").withZone(Datas.FUSO);

    public NotificacaoDeVisitanteAguardando(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.PORTARIA;
    }

    @Override
    protected String montarTitulo(AutorizacaoVisita autorizacao) {
        return "Visitante %s aguardando autorizacao".formatted(autorizacao.getVisitante().getNome());
    }

    @Override
    protected String montarMensagem(Usuario destinatario, AutorizacaoVisita autorizacao) {
        String mensagem = "%s esta na portaria (motivo: %s)."
                .formatted(autorizacao.getVisitante().getNome(), autorizacao.getMotivo());
        if (autorizacao.getExpiraEm() != null) {
            mensagem += " Autorize ou recuse ate as " + HORA.format(autorizacao.getExpiraEm()) + ".";
        }
        return mensagem;
    }

    @Override
    protected Long referenciaId(AutorizacaoVisita autorizacao) {
        return autorizacao.getId();
    }

    /** Hook: push e o canal mais rapido - o visitante esta parado no portao esperando. */
    @Override
    protected CanalNotificacao escolherCanal(AutorizacaoVisita autorizacao) {
        return CanalNotificacao.PUSH;
    }
}
