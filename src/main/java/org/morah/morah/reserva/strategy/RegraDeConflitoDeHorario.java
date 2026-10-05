package org.morah.morah.reserva.strategy;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.reserva.repositorio.ReservaRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Regra 5: nao pode existir outra reserva CONFIRMADA da mesma area que se sobreponha ao periodo.
 *
 * <p>E a unica regra que consulta o banco, por isso roda por ultimo: se uma regra mais barata
 * ja barrou o pedido, a consulta nem acontece.
 *
 * <p>Sozinha ela nao resolve duas reservas feitas no mesmo instante (as duas consultam, nao
 * encontram nada e gravam). Essa corrida e tratada depois de gravar, no
 * {@code ReservaService.depoisDeSalvar}.
 */
@Component
@Order(5)
@RequiredArgsConstructor
public class RegraDeConflitoDeHorario implements RegraDeReservaStrategy {

    /** Mesma mensagem usada pela protecao de concorrencia do service. */
    public static final String HORARIO_INDISPONIVEL =
            "Horario indisponivel: ja existe uma reserva confirmada para esta area nesse periodo.";

    private final ReservaRepository reservaRepository;

    @Override
    public void validar(PedidoDeReserva pedido) {
        boolean ocupado = !reservaRepository
                .listarConfirmadasQueSobrepoem(pedido.area().getId(), pedido.inicio(), pedido.fim())
                .isEmpty();

        if (ocupado) {
            throw new RegraDeNegocioException(HORARIO_INDISPONIVEL);
        }
    }
}
