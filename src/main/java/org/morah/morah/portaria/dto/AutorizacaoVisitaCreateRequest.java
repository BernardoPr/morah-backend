package org.morah.morah.portaria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do POST /portaria/visitantes (schema AutorizacaoVisitaCreateRequest).
 *
 * <p>O {@code @Valid} no campo {@code visitante} faz a validacao "descer" para o objeto interno:
 * sem ele, um visitante sem nome passaria direto.
 */
public record AutorizacaoVisitaCreateRequest(
        @NotNull(message = "informe a unidade visitada") Long unidadeId,
        @NotNull(message = "informe os dados do visitante") @Valid VisitanteInput visitante,
        @NotBlank(message = "informe o motivo da visita") String motivo,
        String fotoUrl) {
}
