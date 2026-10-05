package org.morah.morah.financeiro.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Corpo do POST /financeiro/cobrancas/{cobrancaId}/baixa-manual (schema BaixaManualRequest).
 *
 * <p>Usado quando o morador pagou "por fora" do gateway (transferencia, dinheiro na
 * administracao...). O valor e o informado pelo sindico, mesmo que seja diferente do total
 * calculado - quem negocia descontos ou acordos e ele.
 */
public record BaixaManualRequest(
        @NotBlank(message = "informe o meio de pagamento (ex.: transferencia)") String meio,
        @NotNull(message = "informe o valor pago")
        @Positive(message = "o valor pago deve ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "use no maximo 2 casas decimais") BigDecimal valor,
        @NotNull(message = "informe a data do pagamento") Instant pagoEm,
        String observacao) {
}
