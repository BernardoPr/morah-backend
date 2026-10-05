package org.morah.morah.financeiro.dto;

import java.time.Instant;
import java.time.LocalDate;

import org.morah.morah.financeiro.modelo.DocumentoFinanceiro;

/** Resposta do contrato (schema DocumentoFinanceiro). */
public record DocumentoFinanceiroResponse(
        Long id,
        String tipo,
        String titulo,
        String arquivoUrl,
        LocalDate competencia,
        Instant publicadoEm) {

    public static DocumentoFinanceiroResponse de(DocumentoFinanceiro documento) {
        return new DocumentoFinanceiroResponse(
                documento.getId(),
                documento.getTipo(),
                documento.getTitulo(),
                documento.getArquivoUrl(),
                documento.getCompetencia(),
                documento.getPublicadoEm());
    }
}
