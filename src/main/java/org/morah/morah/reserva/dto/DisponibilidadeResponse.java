package org.morah.morah.reserva.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Resposta do contrato (schema Disponibilidade): os horarios de uma area em um dia.
 *
 * <p>Os horarios indisponiveis tambem aparecem (com {@code disponivel = false}), para o app
 * desenhar a grade completa do dia.
 */
public record DisponibilidadeResponse(LocalDate data, List<Horario> horarios) {

    /** Um bloco reservavel do dia. */
    public record Horario(Instant inicio, Instant fim, boolean disponivel) {
    }
}
