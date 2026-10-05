package org.morah.morah.financeiro.servico;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.morah.morah.comum.erro.TratadorGlobalDeErros.ErroDeCampo;
import org.morah.morah.financeiro.dto.NovoPagamento;
import org.morah.morah.financeiro.dto.PagamentoWebhookPayload;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * POST /financeiro/webhooks/pagamentos: o gateway avisa que um PIX foi pago (ou falhou).
 *
 * <p>Ordem das verificacoes (e o porque):
 * <ol>
 *   <li><b>Assinatura HMAC</b> sobre o corpo CRU - antes de ler o JSON, para nao gastar
 *       processamento nem dar pistas a quem nao conhece o segredo (401);</li>
 *   <li><b>JSON e campos obrigatorios</b> (400);</li>
 *   <li><b>Processamento</b>, que sempre termina em 202 - mesmo quando a cobranca nao existe ou
 *       o aviso e repetido. Gateways reenviam o webhook enquanto nao recebem 2xx; responder erro
 *       para algo que nunca vai dar certo faria ele insistir para sempre.</li>
 * </ol>
 *
 * <p><b>Idempotencia:</b> o mesmo aviso pode chegar duas vezes (reenvio, rede instavel). Cobranca
 * ja paga ou transacao ja usada = ignora e registra no log; a cobranca nunca e quitada duas vezes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookPagamentoService {

    static final String MEIO_DO_GATEWAY = "pix";

    private final VerificadorDeAssinaturaHmac verificadorDeAssinatura;
    private final ObjectMapper objectMapper;
    private final CobrancaRepository cobrancaRepository;
    private final CobrancaService cobrancaService;

    public void receber(String corpo, String assinatura) {
        verificadorDeAssinatura.validar(corpo, assinatura);
        processar(ler(corpo));
    }

    private PagamentoWebhookPayload ler(String corpo) {
        PagamentoWebhookPayload payload;
        try {
            payload = objectMapper.readValue(corpo, PagamentoWebhookPayload.class);
        } catch (JacksonException excecao) {
            throw new PayloadDeWebhookInvalidoException(
                    "O corpo do webhook nao pode ser lido. Confira o JSON e os valores enviados.", List.of());
        }
        if (payload == null) {
            throw new PayloadDeWebhookInvalidoException("O corpo do webhook esta vazio.", List.of());
        }

        List<ErroDeCampo> erros = new ArrayList<>();
        if (payload.transacaoId() == null || payload.transacaoId().isBlank()) {
            erros.add(new ErroDeCampo("transacaoId", "informe o id da transacao no gateway"));
        }
        if (payload.cobrancaId() == null) {
            erros.add(new ErroDeCampo("cobrancaId", "informe a cobranca paga"));
        }
        if (payload.status() == null) {
            erros.add(new ErroDeCampo("status", "informe o status (confirmado ou falhou)"));
        }
        if (payload.valor() != null && payload.valor().signum() <= 0) {
            erros.add(new ErroDeCampo("valor", "o valor pago deve ser maior que zero"));
        }
        if (!erros.isEmpty()) {
            throw new PayloadDeWebhookInvalidoException("Verifique os campos enviados.", erros);
        }
        return payload;
    }

    private void processar(PagamentoWebhookPayload aviso) {
        Cobranca cobranca = cobrancaRepository.findById(aviso.cobrancaId()).orElse(null);
        if (cobranca == null) {
            log.warn("Webhook: cobranca {} nao existe (transacao {}). Aviso descartado.",
                    aviso.cobrancaId(), aviso.transacaoId());
            return;
        }

        if (aviso.status() == PagamentoWebhookPayload.Status.FALHOU) {
            log.info("Webhook: pagamento da cobranca {} FALHOU no gateway (transacao {}). Cobranca segue em aberto.",
                    cobranca.getId(), aviso.transacaoId());
            return;
        }

        if (cobranca.getStatus() == StatusCobranca.PAGO) {
            if (Objects.equals(aviso.transacaoId(), cobranca.getTransacaoId())) {
                log.info("Webhook: aviso repetido da transacao {} (cobranca {} ja paga). Ignorado.",
                        aviso.transacaoId(), cobranca.getId());
            } else {
                log.warn("Webhook: cobranca {} ja estava paga e chegou outra transacao ({}). Possivel pagamento "
                        + "em duplicidade: conciliar/estornar no gateway.", cobranca.getId(), aviso.transacaoId());
            }
            return;
        }

        if (cobranca.getStatus() == StatusCobranca.CANCELADO) {
            log.warn("Webhook: pagamento confirmado para a cobranca CANCELADA {} (transacao {}). Verificar estorno.",
                    cobranca.getId(), aviso.transacaoId());
            return;
        }

        if (cobrancaRepository.existsByTransacaoId(aviso.transacaoId())) {
            log.warn("Webhook: a transacao {} ja quitou outra cobranca. Aviso para a cobranca {} ignorado.",
                    aviso.transacaoId(), cobranca.getId());
            return;
        }

        cobrancaService.registrarPagamento(cobranca, new NovoPagamento(
                aviso.pagoEm() != null ? aviso.pagoEm() : Instant.now(),
                aviso.valor(),
                MEIO_DO_GATEWAY,
                "Pagamento confirmado pelo gateway",
                aviso.transacaoId(),
                null));
        log.info("Webhook: cobranca {} paga (transacao {}).", cobranca.getId(), aviso.transacaoId());
    }
}
