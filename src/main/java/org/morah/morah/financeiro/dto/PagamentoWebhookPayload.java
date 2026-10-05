package org.morah.morah.financeiro.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Corpo do POST /financeiro/webhooks/pagamentos (schema PagamentoWebhookPayload), enviado pelo
 * gateway de pagamento.
 *
 * <p>Nao tem {@code @NotBlank}/{@code @NotNull}: o controller recebe o corpo CRU (texto) para
 * conferir a assinatura HMAC antes de qualquer coisa, entao o {@code @Valid} nao entra em acao.
 * Os campos obrigatorios (transacaoId, cobrancaId, status) sao conferidos no
 * {@code WebhookPagamentoService}, depois da assinatura.
 *
 * <p>{@code ignoreUnknown}: gateways costumam mandar campos a mais; nao e motivo para recusar.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagamentoWebhookPayload(
        String transacaoId,
        Long cobrancaId,
        BigDecimal valor,
        Status status,
        Instant pagoEm) {

    /** Propriedade "status" do schema: resultado do pagamento no gateway. */
    public enum Status {

        CONFIRMADO("confirmado"),
        FALHOU("falhou");

        private final String valor;

        Status(String valor) {
            this.valor = valor;
        }

        @JsonValue
        public String getValor() {
            return valor;
        }

        @JsonCreator
        public static Status de(String valor) {
            return valueOf(valor.toUpperCase());
        }
    }
}
