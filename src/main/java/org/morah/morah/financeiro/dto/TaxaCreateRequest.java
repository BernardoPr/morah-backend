package org.morah.morah.financeiro.dto;

import java.math.BigDecimal;

import org.morah.morah.financeiro.modelo.Competencia;
import org.morah.morah.financeiro.modelo.TipoRateio;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Corpo do POST /financeiro/taxas (schema TaxaCreateRequest).
 *
 * <p>O dia do vencimento vai so ate 28 para existir em todos os meses (fevereiro incluido).
 */
public record TaxaCreateRequest(
        @NotBlank(message = "informe a descricao") String descricao,
        @NotNull(message = Competencia.MENSAGEM_DE_FORMATO)
        @Pattern(regexp = Competencia.FORMATO, message = Competencia.MENSAGEM_DE_FORMATO) String competencia,
        @NotNull(message = "informe o valor total da taxa")
        @Positive(message = "o valor da taxa deve ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "use no maximo 2 casas decimais") BigDecimal valor,
        @NotNull(message = "informe o tipo de rateio (igualitario ou fracao_ideal)") TipoRateio tipoRateio,
        @NotNull(message = "informe o dia do vencimento")
        @Min(value = 1, message = "o dia do vencimento deve ser entre 1 e 28")
        @Max(value = 28, message = "o dia do vencimento deve ser entre 1 e 28") Integer vencimentoDia) {
}
