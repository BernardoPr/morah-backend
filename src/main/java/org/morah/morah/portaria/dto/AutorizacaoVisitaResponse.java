package org.morah.morah.portaria.dto;

import java.time.Instant;

import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;

/**
 * Resposta do contrato (schema AutorizacaoVisita).
 *
 * <p>O {@code status} devolvido e o <i>efetivo</i>: uma pendente cujo prazo ja passou aparece como
 * "expirada", mesmo que o banco ainda nao tenha sido atualizado (expiracao preguicosa).
 */
public record AutorizacaoVisitaResponse(
        Long id,
        VisitanteResponse visitante,
        Long unidadeId,
        String motivo,
        Instant solicitadoEm,
        Instant respondidoEm,
        StatusAutorizacaoVisita status) {

    public static AutorizacaoVisitaResponse de(AutorizacaoVisita autorizacao) {
        return de(autorizacao, Instant.now());
    }

    /** Versao com o "agora" explicito: uma listagem inteira usa o mesmo instante de referencia. */
    public static AutorizacaoVisitaResponse de(AutorizacaoVisita autorizacao, Instant agora) {
        return new AutorizacaoVisitaResponse(
                autorizacao.getId(),
                VisitanteResponse.de(autorizacao.getVisitanteId(), autorizacao.getVisitante()),
                autorizacao.getUnidadeId(),
                autorizacao.getMotivo(),
                autorizacao.getSolicitadoEm(),
                autorizacao.getRespondidoEm(),
                autorizacao.statusEfetivo(agora));
    }
}
