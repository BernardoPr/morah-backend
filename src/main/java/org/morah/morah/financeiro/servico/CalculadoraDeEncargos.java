package org.morah.morah.financeiro.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.config.PropriedadesFinanceiro;
import org.morah.morah.financeiro.dto.SituacaoDaCobranca;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Calcula, para uma data, o status exibido e os encargos de atraso de uma cobranca.
 *
 * <p>Regras (percentuais vindos do bloco {@code morah.financeiro} do application.yaml):
 * <ul>
 *   <li><b>Em dia</b> (pendente com vencimento hoje ou depois): status "pendente", sem encargos.</li>
 *   <li><b>Atrasada</b> (pendente com vencimento antes de hoje): status "atrasado";
 *       multa = {@code multaPercentual}% do valor original, cobrada uma vez;
 *       juros simples = {@code jurosMensalPercentual}% ao mes, pro rata por dia
 *       ({@code dias x juros / 30}), sobre o valor original;
 *       valor total = original + multa + juros.</li>
 *   <li><b>Paga</b>: mostra o que foi pago - multa e juros "congelados" no dia do pagamento e
 *       {@code valorTotal} = valor efetivamente recebido (pode diferir de original + encargos se o
 *       sindico registrou um acordo/desconto na baixa manual).</li>
 *   <li><b>Cancelada</b>: sem encargos; valor total = valor original.</li>
 * </ul>
 *
 * <p>Exemplo: R$ 500,00 vencido ha 15 dias, multa 2% e juros 1% a.m. -> multa R$ 10,00,
 * juros 500 x 1% x 15/30 = R$ 2,50, total R$ 512,50.
 *
 * <p>Por que calcular na leitura e nao gravar? Porque os juros mudam todo dia. Gravando, o valor
 * ficaria velho no dia seguinte (ou exigiria um job diario so para atualizar).
 */
@Component
@RequiredArgsConstructor
public class CalculadoraDeEncargos {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal DIAS_DO_MES_COMERCIAL = BigDecimal.valueOf(30);

    private final PropriedadesFinanceiro propriedades;

    /** Situacao da cobranca no dia {@code hoje}, qualquer que seja o status gravado. */
    public SituacaoDaCobranca situacao(Cobranca cobranca, LocalDate hoje) {
        return switch (cobranca.getStatus()) {
            case PAGO -> comoFoiPaga(cobranca);
            case CANCELADO -> new SituacaoDaCobranca(StatusCobranca.CANCELADO, 0,
                    zero(), zero(), dinheiro(cobranca.getValorOriginal()));
            case PENDENTE, ATRASADO -> emAberto(cobranca, hoje);
        };
    }

    /**
     * Quanto uma cobranca em aberto deve na data informada. Tambem e usado no momento do
     * pagamento, com a data do pagamento, para "congelar" a multa e os juros cobrados.
     */
    public SituacaoDaCobranca emAberto(Cobranca cobranca, LocalDate data) {
        BigDecimal original = dinheiro(cobranca.getValorOriginal());
        long diasDeAtraso = Math.max(0, ChronoUnit.DAYS.between(cobranca.getVencimento(), data));

        if (diasDeAtraso == 0) {
            return new SituacaoDaCobranca(StatusCobranca.PENDENTE, 0, zero(), zero(), original);
        }

        BigDecimal multa = original.multiply(propriedades.multaPercentual())
                .divide(CEM, 2, RoundingMode.HALF_UP);

        // original x (juros% / 100) x (dias / 30), com uma unica divisao no final para nao perder precisao.
        BigDecimal juros = original.multiply(propriedades.jurosMensalPercentual())
                .multiply(BigDecimal.valueOf(diasDeAtraso))
                .divide(CEM.multiply(DIAS_DO_MES_COMERCIAL), 2, RoundingMode.HALF_UP);

        return new SituacaoDaCobranca(StatusCobranca.ATRASADO, diasDeAtraso, multa, juros,
                original.add(multa).add(juros));
    }

    private SituacaoDaCobranca comoFoiPaga(Cobranca cobranca) {
        BigDecimal multa = dinheiro(cobranca.getMultaPaga());
        BigDecimal juros = dinheiro(cobranca.getJurosPagos());
        BigDecimal valorPago = cobranca.getValorPago() != null
                ? dinheiro(cobranca.getValorPago())
                : dinheiro(cobranca.getValorOriginal()).add(multa).add(juros);
        long diasDeAtraso = cobranca.getPagoEm() == null ? 0
                : Math.max(0, ChronoUnit.DAYS.between(cobranca.getVencimento(),
                        Datas.dataDe(cobranca.getPagoEm())));
        return new SituacaoDaCobranca(StatusCobranca.PAGO, diasDeAtraso, multa, juros, valorPago);
    }

    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor == null ? zero() : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
