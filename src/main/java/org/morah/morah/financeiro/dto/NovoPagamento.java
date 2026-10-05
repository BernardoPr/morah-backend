package org.morah.morah.financeiro.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Uso interno (nao sai na API): dados de um pagamento a registrar em uma cobranca.
 *
 * <p>Os dois caminhos de quitacao - baixa manual do sindico e confirmacao do gateway - montam
 * este record e chamam o mesmo {@code CobrancaService.registrarPagamento}, entao a regra de
 * "como uma cobranca fica paga" existe em um lugar so.
 *
 * @param valor        valor recebido; nulo = o total devido na data do pagamento
 * @param transacaoId  id no gateway (nulo na baixa manual)
 * @param baixadoPorId sindico que deu a baixa (nulo quando veio do gateway)
 */
public record NovoPagamento(
        Instant pagoEm,
        BigDecimal valor,
        String meio,
        String observacao,
        String transacaoId,
        Long baixadoPorId) {
}
