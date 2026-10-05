package org.morah.morah.portaria.dto;

import jakarta.validation.constraints.NotBlank;

/** Dados do visitante informados pelo porteiro (schema VisitanteInput). */
public record VisitanteInput(
        @NotBlank(message = "informe o nome do visitante") String nome,
        String documento,
        String telefone) {
}
