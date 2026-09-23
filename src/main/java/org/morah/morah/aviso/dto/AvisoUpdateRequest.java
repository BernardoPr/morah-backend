package org.morah.morah.aviso.dto;

import java.time.Instant;

import org.morah.morah.aviso.modelo.PrioridadeAviso;

/**
 * Corpo do PATCH /avisos/{avisoId} (schema AvisoUpdateRequest).
 * Todos os campos sao opcionais: o que vier nulo nao e alterado.
 */
public record AvisoUpdateRequest(
        String titulo,
        String conteudo,
        PrioridadeAviso prioridade,
        Instant expiraEm) {
}
