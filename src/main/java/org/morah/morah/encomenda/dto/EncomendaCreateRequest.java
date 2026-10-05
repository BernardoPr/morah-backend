package org.morah.morah.encomenda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Corpo do POST /encomendas (schema EncomendaCreateRequest).
 *
 * <p>So {@code unidadeId} e {@code transportadora} sao obrigatorios: muitas vezes o porteiro
 * nao sabe quem mandou o pacote nem tem o codigo de rastreio.
 */
public record EncomendaCreateRequest(
        @NotNull(message = "informe a unidade") Long unidadeId,
        String remetente,
        @NotBlank(message = "informe a transportadora") String transportadora,
        String codigoRastreio,
        @Pattern(regexp = "^https?://\\S+$", message = "informe uma URL http(s) valida para a foto")
        String fotoUrl) {
}
