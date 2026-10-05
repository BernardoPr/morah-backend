package org.morah.morah.notificacao.template;

import java.time.format.DateTimeFormatter;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.modelo.Vistoria;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Avisa os moradores da unidade que a vistoria da portaria encontrou dano no espaco reservado
 * (o sindico e avisado a parte, pela ocorrencia aberta no mesmo momento).
 *
 * <p>A observacao vem da ultima vistoria com dano gravada na reserva.
 */
@Component
public class NotificacaoDeDanoEmReserva extends NotificacaoTemplate<Reserva> {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public NotificacaoDeDanoEmReserva(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.RESERVA;
    }

    @Override
    protected String montarTitulo(Reserva reserva) {
        return "Dano registrado na vistoria: " + reserva.getAreaComumNome();
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Reserva reserva) {
        Vistoria vistoria = reserva.getVistorias().stream()
                .filter(Vistoria::isDano)
                .reduce((primeira, segunda) -> segunda)
                .orElse(null);

        String momento = vistoria == null ? "" : " de " + vistoria.getMomento().getValor();
        String observacao = vistoria == null || vistoria.getObservacao() == null || vistoria.getObservacao().isBlank()
                ? "sem observacao"
                : vistoria.getObservacao();

        return "A vistoria%s da reserva de %s em %s registrou dano: %s. O sindico foi avisado por uma ocorrencia."
                .formatted(momento, reserva.getAreaComumNome(),
                        DATA.format(reserva.getInicio().atZone(Datas.FUSO)), observacao);
    }

    @Override
    protected Long referenciaId(Reserva reserva) {
        return reserva.getId();
    }

    /** Hook sobrescrito: dano pode virar cobranca, entao o aviso vai por e-mail (fica registrado). */
    @Override
    protected CanalNotificacao escolherCanal(Reserva reserva) {
        return CanalNotificacao.EMAIL;
    }
}
