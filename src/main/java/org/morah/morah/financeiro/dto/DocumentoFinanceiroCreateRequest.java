package org.morah.morah.financeiro.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Corpo do POST /financeiro/documentos (schema DocumentoFinanceiroCreateRequest).
 *
 * <p>O contrato declara {@code arquivoUrl} como {@code format: uri}; aceitamos so http(s),
 * que e o que o app consegue abrir.
 */
public record DocumentoFinanceiroCreateRequest(
        @NotBlank(message = "informe o tipo do documento (ex.: nota_fiscal)") String tipo,
        @NotBlank(message = "informe o titulo") String titulo,
        @NotBlank(message = "informe a URL do arquivo")
        @Pattern(regexp = "^https?://\\S+$", message = "informe uma URL http(s) valida") String arquivoUrl,
        LocalDate competencia) {
}
