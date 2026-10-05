package org.morah.morah.reserva.strategy;

import java.time.ZonedDateTime;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.reserva.modelo.AreaComum;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Regra 4: a reserva cabe no horario de funcionamento da area, num unico dia.
 *
 * <p>O pedido chega em UTC ({@code Instant}), mas o horario da area e a hora local do condominio
 * (ex.: 10:00 em Sao Paulo). Por isso convertemos para o fuso {@link Datas#FUSO} antes de comparar:
 * "2026-10-10T13:00:00Z" sao 10:00 no condominio.
 */
@Component
@Order(4)
public class RegraDeHorarioDeFuncionamento implements RegraDeReservaStrategy {

    @Override
    public void validar(PedidoDeReserva pedido) {
        AreaComum area = pedido.area();
        if (!area.temHorarioDeFuncionamento()) {
            throw new RegraDeNegocioException(
                    "A area " + area.getNome() + " nao tem horario de funcionamento configurado.");
        }

        ZonedDateTime inicio = pedido.inicio().atZone(Datas.FUSO);
        ZonedDateTime fim = pedido.fim().atZone(Datas.FUSO);

        if (!inicio.toLocalDate().equals(fim.toLocalDate())) {
            throw new RegraDeNegocioException("A reserva deve comecar e terminar no mesmo dia.");
        }

        boolean antesDeAbrir = inicio.toLocalTime().isBefore(area.abertura());
        boolean depoisDeFechar = fim.toLocalTime().isAfter(area.fechamento());
        if (antesDeAbrir || depoisDeFechar) {
            throw new RegraDeNegocioException("Fora do horario de funcionamento da area %s (das %s as %s)."
                    .formatted(area.getNome(), area.getHoraAbertura(), area.getHoraFechamento()));
        }
    }
}
