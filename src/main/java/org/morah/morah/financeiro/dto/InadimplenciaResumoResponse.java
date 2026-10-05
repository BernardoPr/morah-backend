package org.morah.morah.financeiro.dto;

import java.math.BigDecimal;

/**
 * Resposta do contrato (schema InadimplenciaResumo). Tambem e o bloco "inadimplencia" do
 * dashboard do sindico.
 *
 * @param percentualInadimplencia de 0 a 100, com 2 casas (ex.: 25.00 = uma em cada quatro unidades)
 * @param valorTotalEmAberto      soma do valor total (com multa e juros) das cobrancas atrasadas
 */
public record InadimplenciaResumoResponse(
        long totalUnidades,
        long unidadesInadimplentes,
        BigDecimal percentualInadimplencia,
        BigDecimal valorTotalEmAberto) {
}
