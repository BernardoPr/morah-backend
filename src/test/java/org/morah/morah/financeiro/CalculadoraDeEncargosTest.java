package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.morah.morah.financeiro.DadosDeTeste.cobranca;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.financeiro.dto.SituacaoDaCobranca;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.servico.CalculadoraDeEncargos;

/** Status derivado ("atrasado") e encargos: multa 2% uma vez + juros 1% a.m. pro rata por dia. */
class CalculadoraDeEncargosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 25);

    private final CalculadoraDeEncargos calculadora = new CalculadoraDeEncargos(DadosDeTeste.propriedades());

    @Test
    @DisplayName("vencimento no futuro: pendente, sem multa nem juros")
    void emDia() {
        SituacaoDaCobranca situacao = calculadora.situacao(cobranca(1L, 101L, "500.00", HOJE.plusDays(5)), HOJE);

        assertThat(situacao.status()).isEqualTo(StatusCobranca.PENDENTE);
        assertThat(situacao.multa()).isEqualByComparingTo("0");
        assertThat(situacao.juros()).isEqualByComparingTo("0");
        assertThat(situacao.valorTotal()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("vence hoje: ainda nao esta atrasada")
    void venceHoje() {
        SituacaoDaCobranca situacao = calculadora.situacao(cobranca(1L, 101L, "500.00", HOJE), HOJE);

        assertThat(situacao.status()).isEqualTo(StatusCobranca.PENDENTE);
        assertThat(situacao.valorTotal()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("15 dias de atraso em R$ 500: multa 10,00 + juros 2,50 = 512,50")
    void atrasada() {
        SituacaoDaCobranca situacao = calculadora.situacao(cobranca(1L, 101L, "500.00", HOJE.minusDays(15)), HOJE);

        assertThat(situacao.status()).isEqualTo(StatusCobranca.ATRASADO);
        assertThat(situacao.diasDeAtraso()).isEqualTo(15);
        assertThat(situacao.multa()).isEqualByComparingTo("10.00");
        assertThat(situacao.juros()).isEqualByComparingTo("2.50");
        assertThat(situacao.valorTotal()).isEqualByComparingTo("512.50");
    }

    @Test
    @DisplayName("1 dia de atraso: a multa e cheia, os juros sao de 1/30 do mes (arredondados)")
    void umDiaDeAtraso() {
        // 460 x 1% / 30 = 0,1533... -> 0,15
        SituacaoDaCobranca situacao = calculadora.situacao(cobranca(1L, 101L, "460.00", HOJE.minusDays(1)), HOJE);

        assertThat(situacao.multa()).isEqualByComparingTo("9.20");
        assertThat(situacao.juros()).isEqualByComparingTo("0.15");
        assertThat(situacao.valorTotal()).isEqualByComparingTo("469.35");
    }

    @Test
    @DisplayName("paga: mostra o que foi pago (encargos congelados e valor recebido)")
    void paga() {
        Cobranca cobranca = cobranca(1L, 101L, "500.00", HOJE.minusDays(40));
        cobranca.setStatus(StatusCobranca.PAGO);
        cobranca.setPagoEm(Instant.parse("2026-09-30T15:00:00Z"));
        cobranca.setMultaPaga(new BigDecimal("10.00"));
        cobranca.setJurosPagos(new BigDecimal("2.50"));
        cobranca.setValorPago(new BigDecimal("512.50"));

        SituacaoDaCobranca situacao = calculadora.situacao(cobranca, HOJE);

        assertThat(situacao.status()).isEqualTo(StatusCobranca.PAGO);
        assertThat(situacao.multa()).isEqualByComparingTo("10.00");
        assertThat(situacao.juros()).isEqualByComparingTo("2.50");
        assertThat(situacao.valorTotal()).isEqualByComparingTo("512.50");
    }

    @Test
    @DisplayName("cancelada: nunca acumula encargos, mesmo vencida")
    void cancelada() {
        Cobranca cobranca = cobranca(1L, 101L, "500.00", HOJE.minusDays(40));
        cobranca.setStatus(StatusCobranca.CANCELADO);

        SituacaoDaCobranca situacao = calculadora.situacao(cobranca, HOJE);

        assertThat(situacao.status()).isEqualTo(StatusCobranca.CANCELADO);
        assertThat(situacao.multa()).isEqualByComparingTo("0");
        assertThat(situacao.valorTotal()).isEqualByComparingTo("500.00");
    }
}
