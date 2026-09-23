package org.morah.morah.aviso.strategy;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;

/**
 * PADRAO DE PROJETO: STRATEGY (exemplo 3 de 3) - contrato comum.
 *
 * <p><b>Problema:</b> o sindico quer baixar a lista de avisos em formatos diferentes
 * (CSV para abrir no Excel, JSON para integrar com outro sistema). Um {@code switch}
 * dentro do controller cresceria a cada formato novo.
 *
 * <p><b>Solucao:</b> um formato = uma classe. O {@link SeletorDeExportador} escolhe pela
 * string recebida na query string ({@code GET /avisos/exportar?formato=csv}).
 */
public interface ExportadorDeAvisosStrategy {

    /** Nome usado na query string (ex.: "csv"). */
    String formato();

    /** Content-Type devolvido no download. */
    String tipoDeConteudo();

    String exportar(List<AvisoResponse> avisos);
}
