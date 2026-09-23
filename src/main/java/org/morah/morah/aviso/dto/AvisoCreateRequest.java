package org.morah.morah.aviso.dto;

import java.time.Instant;

import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.morah.morah.aviso.modelo.PublicoAlvo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do POST /avisos (schema AvisoCreateRequest).
 *
 * <p>As anotacoes de validacao ({@code @NotBlank}, {@code @NotNull}) sao verificadas
 * automaticamente por causa do {@code @Valid} no controller; o erro vira um 400 no
 * formato RFC 9457 montado pelo {@code TratadorGlobalDeErros}.
 */
public record AvisoCreateRequest(
        @NotBlank(message = "informe o titulo") String titulo,
        @NotBlank(message = "informe o conteudo") String conteudo,
        PrioridadeAviso prioridade,
        @NotNull(message = "informe o publico alvo") PublicoAlvo publicoAlvo,
        Instant expiraEm) {
}
