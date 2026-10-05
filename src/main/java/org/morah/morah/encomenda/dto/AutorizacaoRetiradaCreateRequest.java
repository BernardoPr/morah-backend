package org.morah.morah.encomenda.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo do POST /encomendas/{id}/autorizacoes-retirada (schema AutorizacaoRetiradaCreateRequest).
 *
 * <p>{@code validadeHoras} e opcional (padrao 24h, como no contrato). Limitamos a uma semana
 * (168h): um codigo que vale para sempre seria um risco se vazasse.
 */
public record AutorizacaoRetiradaCreateRequest(
        @NotBlank(message = "informe o nome de quem vai retirar") String nomeTerceiro,
        @NotBlank(message = "informe o documento de quem vai retirar") String documento,
        @Min(value = 1, message = "a validade deve ser de 1 a 168 horas")
        @Max(value = 168, message = "a validade deve ser de 1 a 168 horas")
        Integer validadeHoras) {

    public static final int VALIDADE_PADRAO_EM_HORAS = 24;

    /** Validade pedida ou o padrao do contrato (24h). */
    public int validadeEmHoras() {
        return validadeHoras == null ? VALIDADE_PADRAO_EM_HORAS : validadeHoras;
    }
}
