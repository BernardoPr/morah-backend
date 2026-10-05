package org.morah.morah.reserva.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.morah.morah.reserva.modelo.AreaComum;

/**
 * Resposta do contrato (schema AreaComum).
 *
 * <p>O horario de funcionamento e o tamanho dos horarios nao fazem parte do schema: o front
 * descobre os horarios pela rota de disponibilidade.
 */
public record AreaComumResponse(
        Long id,
        String nome,
        String descricao,
        Integer capacidade,
        BigDecimal taxa,
        boolean requerReserva,
        String status) {

    public static AreaComumResponse de(AreaComum area) {
        return new AreaComumResponse(
                area.getId(),
                area.getNome(),
                area.getDescricao(),
                area.getCapacidade(),
                area.getTaxa() == null ? null : area.getTaxa().setScale(2, RoundingMode.HALF_UP),
                area.isRequerReserva(),
                area.getStatus());
    }
}
