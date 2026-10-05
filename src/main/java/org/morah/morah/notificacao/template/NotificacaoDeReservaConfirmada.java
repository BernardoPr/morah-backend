package org.morah.morah.notificacao.template;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Avisa os moradores da unidade que a reserva de area comum foi confirmada.
 *
 * <p>Os horarios sao mostrados na hora local do condominio ({@link Datas#FUSO}), e nao em UTC.
 */
@Component
public class NotificacaoDeReservaConfirmada extends NotificacaoTemplate<Reserva> {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public NotificacaoDeReservaConfirmada(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.RESERVA;
    }

    @Override
    protected String montarTitulo(Reserva reserva) {
        return "Reserva confirmada";
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Reserva reserva) {
        ZonedDateTime inicio = reserva.getInicio().atZone(Datas.FUSO);
        ZonedDateTime fim = reserva.getFim().atZone(Datas.FUSO);

        String mensagem = "%s em %s, das %s as %s, esta confirmada para a sua unidade."
                .formatted(reserva.getAreaComumNome(), DATA.format(inicio), HORA.format(inicio), HORA.format(fim));

        BigDecimal valor = reserva.getValor();
        if (valor != null && valor.signum() > 0) {
            mensagem += " Valor: R$ " + String.format(Locale.of("pt", "BR"), "%.2f", valor) + ".";
        }
        return mensagem;
    }

    @Override
    protected Long referenciaId(Reserva reserva) {
        return reserva.getId();
    }
}
