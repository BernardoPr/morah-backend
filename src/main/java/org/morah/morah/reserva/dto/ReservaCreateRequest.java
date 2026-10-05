package org.morah.morah.reserva.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo do POST /reservas (schema ReservaCreateRequest).
 *
 * <p>Aqui so conferimos se os campos vieram. As regras de negocio (area ativa, horario no
 * futuro, dentro do funcionamento, sem conflito...) ficam nas estrategias
 * {@code RegraDeReservaStrategy}, aplicadas pelo {@code ReservaService}.
 */
public record ReservaCreateRequest(
        @NotNull(message = "informe a area comum") Long areaComumId,
        @NotNull(message = "informe o inicio") Instant inicio,
        @NotNull(message = "informe o fim") Instant fim) {
}
