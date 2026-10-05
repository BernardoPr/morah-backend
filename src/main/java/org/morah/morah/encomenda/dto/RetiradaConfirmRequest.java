package org.morah.morah.encomenda.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo do POST /encomendas/{id}/retirada (schema RetiradaConfirmRequest).
 *
 * <p>O {@code codigo} pode ser o do morador ou o de uma autorizacao de terceiro. Nome e
 * documento de quem retirou sao opcionais: se o codigo for de uma autorizacao e eles nao vierem,
 * usamos os dados que o morador cadastrou na autorizacao.
 */
public record RetiradaConfirmRequest(
        @NotBlank(message = "informe o codigo de retirada") String codigo,
        String retiradoPorNome,
        String retiradoPorDocumento) {
}
