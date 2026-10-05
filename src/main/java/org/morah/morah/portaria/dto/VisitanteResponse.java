package org.morah.morah.portaria.dto;

import org.morah.morah.portaria.modelo.DadosDoVisitante;

/** Resposta do contrato (schema Visitante), montada a partir da copia guardada no registro. */
public record VisitanteResponse(
        Long id,
        String nome,
        String documento,
        String telefone,
        String observacao) {

    public static VisitanteResponse de(Long visitanteId, DadosDoVisitante dados) {
        if (dados == null) {
            return new VisitanteResponse(visitanteId, null, null, null, null);
        }
        return new VisitanteResponse(
                visitanteId,
                dados.getNome(),
                dados.getDocumento(),
                dados.getTelefone(),
                dados.getObservacao());
    }
}
