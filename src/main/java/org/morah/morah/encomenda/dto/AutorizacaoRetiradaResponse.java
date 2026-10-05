package org.morah.morah.encomenda.dto;

import java.time.Instant;

import org.morah.morah.encomenda.modelo.AutorizacaoRetirada;
import org.morah.morah.encomenda.modelo.StatusAutorizacaoRetirada;

/**
 * Resposta do contrato (schema AutorizacaoRetirada).
 *
 * <p>O {@code codigo} vai na resposta porque quem pede e o proprio morador: ele repassa o codigo
 * ao terceiro, que o apresenta na portaria.
 */
public record AutorizacaoRetiradaResponse(
        Long id,
        Long encomendaId,
        String nomeTerceiro,
        String documento,
        String codigo,
        Instant validade,
        StatusAutorizacaoRetirada status) {

    public static AutorizacaoRetiradaResponse de(AutorizacaoRetirada autorizacao) {
        return de(autorizacao, Instant.now());
    }

    /** O status exibido leva em conta a validade (ativa vencida aparece como expirada). */
    public static AutorizacaoRetiradaResponse de(AutorizacaoRetirada autorizacao, Instant agora) {
        return new AutorizacaoRetiradaResponse(
                autorizacao.getId(),
                autorizacao.getEncomendaId(),
                autorizacao.getNomeTerceiro(),
                autorizacao.getDocumento(),
                autorizacao.getCodigo(),
                autorizacao.getValidade(),
                autorizacao.statusEm(agora));
    }
}
