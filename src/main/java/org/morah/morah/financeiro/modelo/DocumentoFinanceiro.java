package org.morah.morah.financeiro.modelo;

import java.time.Instant;
import java.time.LocalDate;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "documentos_financeiros" (schema DocumentoFinanceiro do contrato): notas fiscais,
 * prestacoes de contas, balancetes... publicados pelo sindico para transparencia.
 *
 * <p>O arquivo em si nao fica no banco: guardamos so a URL de onde ele foi hospedado.
 */
@Getter
@Setter
@Document(collection = "documentos_financeiros")
public class DocumentoFinanceiro extends EntidadeBase {

    @Indexed
    private Long condominioId;

    /** Texto livre definido pelo contrato (ex.: "nota_fiscal", "prestacao_contas"). */
    private String tipo;

    private String titulo;
    private String arquivoUrl;

    /** Mes a que o documento se refere (o contrato usa format: date). */
    private LocalDate competencia;

    private Instant publicadoEm;
    private Long publicadoPorId;
}
