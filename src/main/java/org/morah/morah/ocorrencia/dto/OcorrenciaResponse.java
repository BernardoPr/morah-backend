package org.morah.morah.ocorrencia.dto;

import java.time.Instant;

import org.morah.morah.ocorrencia.modelo.Ocorrencia;
import org.morah.morah.ocorrencia.modelo.StatusOcorrencia;

/** Resposta do contrato (schema Ocorrencia). */
public record OcorrenciaResponse(
        Long id,
        String titulo,
        String descricao,
        StatusOcorrencia status,
        Instant abertaEm,
        Instant encerradaEm) {

    public static OcorrenciaResponse de(Ocorrencia ocorrencia) {
        return new OcorrenciaResponse(
                ocorrencia.getId(),
                ocorrencia.getTitulo(),
                ocorrencia.getDescricao(),
                ocorrencia.getStatus(),
                ocorrencia.getAbertaEm(),
                ocorrencia.getEncerradaEm());
    }
}
