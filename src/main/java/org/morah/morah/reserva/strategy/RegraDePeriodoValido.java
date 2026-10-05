package org.morah.morah.reserva.strategy;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Regra 2: o fim tem que ser depois do inicio (reserva de duracao zero ou negativa nao existe). */
@Component
@Order(2)
public class RegraDePeriodoValido implements RegraDeReservaStrategy {

    @Override
    public void validar(PedidoDeReserva pedido) {
        if (!pedido.fim().isAfter(pedido.inicio())) {
            throw new RegraDeNegocioException("O fim da reserva deve ser depois do inicio.");
        }
    }
}
