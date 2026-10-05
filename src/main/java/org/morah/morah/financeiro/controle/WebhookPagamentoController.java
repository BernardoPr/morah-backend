package org.morah.morah.financeiro.controle;

import org.morah.morah.financeiro.dto.PagamentoWebhookPayload;
import org.morah.morah.financeiro.servico.WebhookPagamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Webhook do gateway de pagamento. Rota PUBLICA (liberada em {@code ConfiguracaoSeguranca}):
 * quem chama e o gateway, autenticado pela assinatura HMAC do cabecalho, nao por JWT.
 *
 * <p>O corpo e recebido como TEXTO CRU ({@code String}) de proposito: a assinatura e calculada
 * sobre os bytes exatos que o gateway enviou. Se deixassemos o Spring converter para objeto e
 * depois gerar o JSON de novo, espacos e ordem dos campos poderiam mudar e a assinatura nunca bateria.
 */
@Tag(name = "Financeiro", description = "Boletos/cobrancas, PIX, documentos financeiros e gestao de inadimplencia")
@RestController
@RequestMapping("/financeiro/webhooks")
@RequiredArgsConstructor
public class WebhookPagamentoController {

    private final WebhookPagamentoService webhookPagamentoService;

    @Operation(
            summary = "Webhook do gateway de pagamento (confirmacao de PIX)",
            description = "Chamado pelo gateway, nao pelo app. Cabecalho X-Webhook-Signature = HMAC-SHA256 "
                    + "(hexadecimal, prefixo 'sha256=' opcional) do corpo, com o segredo compartilhado.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PagamentoWebhookPayload.class))))
    @SecurityRequirements // sem bearer: a autenticacao e a assinatura
    @PostMapping("/pagamentos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receber(@RequestHeader(name = "X-Webhook-Signature", required = false) String assinatura,
                        @RequestBody String corpo) {
        webhookPagamentoService.receber(corpo, assinatura);
    }
}
