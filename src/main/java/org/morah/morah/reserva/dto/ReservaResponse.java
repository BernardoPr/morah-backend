package org.morah.morah.reserva.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.morah.morah.comum.dto.PessoaResumo;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.modelo.StatusReserva;

/**
 * Resposta do contrato (schema Reserva).
 *
 * <p>As vistorias ficam gravadas na reserva, mas nao sao devolvidas: o schema do contrato
 * nao tem esse campo.
 */
public record ReservaResponse(
        Long id,
        Long areaComumId,
        String areaComumNome,
        Long unidadeId,
        PessoaResumo solicitante,
        Instant inicio,
        Instant fim,
        StatusReserva status,
        BigDecimal valor) {

    public static ReservaResponse de(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getAreaComumId(),
                reserva.getAreaComumNome(),
                reserva.getUnidadeId(),
                new PessoaResumo(reserva.getSolicitanteId(), reserva.getSolicitanteNome(),
                        reserva.getSolicitanteTelefone()),
                reserva.getInicio(),
                reserva.getFim(),
                reserva.getStatus(),
                reserva.getValor() == null ? null : reserva.getValor().setScale(2, RoundingMode.HALF_UP));
    }
}
