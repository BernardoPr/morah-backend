package org.morah.morah.reserva.strategy;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Regra 3: nao se reserva o passado - a reserva tem que comecar depois de "agora".
 *
 * <p>Se o condominio quiser exigir antecedencia minima (ex.: 24 horas), basta criar outra
 * classe {@code RegraDeAntecedenciaMinima} - e exatamente o ganho do Strategy.
 */
@Component
@Order(3)
public class RegraDeInicioNoFuturo implements RegraDeReservaStrategy {

    @Override
    public void validar(PedidoDeReserva pedido) {
        if (!pedido.inicio().isAfter(pedido.agora())) {
            throw new RegraDeNegocioException("A reserva deve comecar no futuro.");
        }
    }
}
