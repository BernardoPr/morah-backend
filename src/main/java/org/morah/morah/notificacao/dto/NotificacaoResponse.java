package org.morah.morah.notificacao.dto;

import java.time.Instant;

import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;

/** Resposta do contrato (schema Notificacao). */
public record NotificacaoResponse(
        Long id,
        TipoNotificacao tipo,
        String titulo,
        String mensagem,
        Long referenciaId,
        Instant enviadaEm,
        Instant lidaEm) {

    public static NotificacaoResponse de(Notificacao notificacao) {
        return new NotificacaoResponse(
                notificacao.getId(),
                notificacao.getTipo(),
                notificacao.getTitulo(),
                notificacao.getMensagem(),
                notificacao.getReferenciaId(),
                notificacao.getEnviadaEm(),
                notificacao.getLidaEm());
    }
}
