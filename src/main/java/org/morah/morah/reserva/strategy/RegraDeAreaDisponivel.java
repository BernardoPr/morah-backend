package org.morah.morah.reserva.strategy;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.reserva.modelo.AreaComum;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Regra 1: a area precisa estar ativa (nao em manutencao) e exigir reserva (academia, por exemplo, nao exige). */
@Component
@Order(1)
public class RegraDeAreaDisponivel implements RegraDeReservaStrategy {

    @Override
    public void validar(PedidoDeReserva pedido) {
        AreaComum area = pedido.area();

        if (!area.estaAtiva()) {
            throw new RegraDeNegocioException(
                    "A area " + area.getNome() + " nao esta disponivel para reservas (status: " + area.getStatus() + ").");
        }
        if (!area.isRequerReserva()) {
            throw new RegraDeNegocioException(
                    "A area " + area.getNome() + " e de uso livre e nao precisa de reserva.");
        }
    }
}
