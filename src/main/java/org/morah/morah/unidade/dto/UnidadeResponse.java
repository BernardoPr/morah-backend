package org.morah.morah.unidade.dto;

import java.math.BigDecimal;

import org.morah.morah.unidade.modelo.Unidade;

/** Resposta do contrato (schema Unidade). */
public record UnidadeResponse(
        Long id,
        Long blocoId,
        String blocoNome,
        String identificacao,
        String tipo,
        BigDecimal areaM2,
        BigDecimal fracaoIdeal,
        String status) {

    public static UnidadeResponse de(Unidade unidade) {
        return new UnidadeResponse(
                unidade.getId(),
                unidade.getBlocoId(),
                unidade.getBlocoNome(),
                unidade.getIdentificacao(),
                unidade.getTipo(),
                unidade.getAreaM2(),
                unidade.getFracaoIdeal(),
                unidade.getStatus());
    }
}
