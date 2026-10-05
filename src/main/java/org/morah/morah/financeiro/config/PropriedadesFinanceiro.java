package org.morah.morah.financeiro.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Espelha o bloco "morah.financeiro" do application.yaml (mesma ideia do {@code PropriedadesMorah}).
 *
 * <p>Fica dentro do modulo financeiro porque so ele usa estes valores. E encontrado sozinho pelo
 * {@code @ConfigurationPropertiesScan} da {@code MorahApplication}. Nos testes basta dar
 * {@code new PropriedadesFinanceiro(...)} - nao precisa subir o Spring.
 *
 * @param webhookSegredo        segredo HMAC-SHA256 combinado com o gateway de pagamento
 * @param chavePix              chave PIX do condominio (vai dentro do codigo copia-e-cola)
 * @param minutosValidadePix    por quanto tempo um codigo PIX gerado continua valendo
 * @param multaPercentual       multa unica por atraso, em % do valor original (ex.: 2.0)
 * @param jurosMensalPercentual juros simples ao mes, em %, cobrados pro rata por dia (ex.: 1.0)
 */
@ConfigurationProperties(prefix = "morah.financeiro")
public record PropriedadesFinanceiro(
        String webhookSegredo,
        String chavePix,
        long minutosValidadePix,
        BigDecimal multaPercentual,
        BigDecimal jurosMensalPercentual) {

    private static final long VALIDADE_PADRAO_DO_PIX = 30;

    /** Valores padrao para a aplicacao nao quebrar se alguma chave sumir do yaml. */
    public PropriedadesFinanceiro {
        minutosValidadePix = minutosValidadePix > 0 ? minutosValidadePix : VALIDADE_PADRAO_DO_PIX;
        multaPercentual = multaPercentual == null ? BigDecimal.ZERO : multaPercentual;
        jurosMensalPercentual = jurosMensalPercentual == null ? BigDecimal.ZERO : jurosMensalPercentual;
    }
}
