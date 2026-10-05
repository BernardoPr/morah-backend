package org.morah.morah.encomenda.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo do POST /encomendas/{id}/ocorrencias (schema OcorrenciaCreateRequest).
 * A resposta e o {@code OcorrenciaResponse} do modulo compartilhado de ocorrencias.
 */
public record OcorrenciaCreateRequest(
        @NotBlank(message = "informe o titulo") String titulo,
        @NotBlank(message = "informe a descricao") String descricao) {
}
