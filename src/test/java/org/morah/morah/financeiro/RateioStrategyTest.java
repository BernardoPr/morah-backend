package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.morah.morah.financeiro.DadosDeTeste.unidade;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.financeiro.modelo.TipoRateio;
import org.morah.morah.financeiro.strategy.ParcelaDoRateio;
import org.morah.morah.financeiro.strategy.RateioIgualitarioStrategy;
import org.morah.morah.financeiro.strategy.RateioPorFracaoIdealStrategy;
import org.morah.morah.financeiro.strategy.SeletorDeRateio;

/** STRATEGY de rateio: cada estrategia isolada e o seletor que escolhe entre elas. */
class RateioStrategyTest {

    private static BigDecimal soma(List<ParcelaDoRateio> parcelas) {
        return parcelas.stream().filter(p -> !p.falhou()).map(ParcelaDoRateio::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<BigDecimal> valores(List<ParcelaDoRateio> parcelas) {
        return parcelas.stream().map(ParcelaDoRateio::valor).toList();
    }

    @Nested
    class Igualitario {

        private final RateioIgualitarioStrategy rateio = new RateioIgualitarioStrategy();

        @Test
        @DisplayName("divide em partes iguais quando a conta e exata")
        void divisaoExata() {
            var parcelas = rateio.ratear(new BigDecimal("2000.00"),
                    List.of(unidade(101L, null), unidade(102L, null), unidade(201L, null), unidade(202L, null)));

            assertThat(valores(parcelas)).containsExactly(
                    new BigDecimal("500.00"), new BigDecimal("500.00"), new BigDecimal("500.00"), new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("100,00 / 3: o centavo que sobra vai para a primeira unidade e a soma fecha")
        void centavoQueSobra() {
            var parcelas = rateio.ratear(new BigDecimal("100.00"),
                    List.of(unidade(101L, null), unidade(102L, null), unidade(201L, null)));

            assertThat(valores(parcelas)).containsExactly(
                    new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
            assertThat(soma(parcelas)).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("varios centavos sobrando: um para cada uma das primeiras unidades")
        void variosCentavos() {
            // 1000,00 / 7 = 142,857... -> 100000 centavos / 7 = 14285 e sobram 5
            var unidades = List.of(unidade(1L, null), unidade(2L, null), unidade(3L, null), unidade(4L, null),
                    unidade(5L, null), unidade(6L, null), unidade(7L, null));

            var parcelas = rateio.ratear(new BigDecimal("1000.00"), unidades);

            assertThat(parcelas).filteredOn(p -> p.valor().compareTo(new BigDecimal("142.86")) == 0).hasSize(5);
            assertThat(parcelas).filteredOn(p -> p.valor().compareTo(new BigDecimal("142.85")) == 0).hasSize(2);
            assertThat(soma(parcelas)).isEqualByComparingTo("1000.00");
        }

        @Test
        void semUnidadesNaoHaParcelas() {
            assertThat(rateio.ratear(new BigDecimal("100.00"), List.of())).isEmpty();
        }
    }

    @Nested
    class PorFracaoIdeal {

        private final RateioPorFracaoIdealStrategy rateio = new RateioPorFracaoIdealStrategy();

        @Test
        @DisplayName("fracoes da carga inicial: 2.000,00 vira 460 / 460 / 540 / 540")
        void fracoesDoCondominioDeExemplo() {
            var parcelas = rateio.ratear(new BigDecimal("2000.00"), List.of(
                    unidade(101L, "0.23"), unidade(102L, "0.23"), unidade(201L, "0.27"), unidade(202L, "0.27")));

            assertThat(valores(parcelas)).containsExactly(
                    new BigDecimal("460.00"), new BigDecimal("460.00"), new BigDecimal("540.00"), new BigDecimal("540.00"));
            assertThat(soma(parcelas)).isEqualByComparingTo("2000.00");
        }

        @Test
        @DisplayName("fracoes 'quebradas' (1/3) nao perdem centavo: a soma continua exata")
        void fracoesQuebradas() {
            var parcelas = rateio.ratear(new BigDecimal("100.00"), List.of(
                    unidade(1L, "0.3333333333"), unidade(2L, "0.3333333333"), unidade(3L, "0.3333333334")));

            assertThat(soma(parcelas)).isEqualByComparingTo("100.00");
            // O centavo vai para quem perdeu mais no arredondamento (a terceira, com fracao maior).
            assertThat(valores(parcelas)).containsExactly(
                    new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
        }

        @Test
        @DisplayName("unidade sem fracao ideal vira falha; as outras pagam so a propria fracao")
        void unidadeSemFracaoIdeal() {
            var parcelas = rateio.ratear(new BigDecimal("1000.00"), List.of(
                    unidade(101L, "0.50"), unidade(102L, null), unidade(201L, "0.25")));

            assertThat(parcelas.get(0).valor()).isEqualByComparingTo("500.00");
            assertThat(parcelas.get(1).falhou()).isTrue();
            assertThat(parcelas.get(1).motivoDaFalha()).contains("fracao ideal");
            assertThat(parcelas.get(2).valor()).isEqualByComparingTo("250.00");
        }
    }

    @Nested
    class Seletor {

        private final SeletorDeRateio seletor = new SeletorDeRateio(
                List.of(new RateioIgualitarioStrategy(), new RateioPorFracaoIdealStrategy()));

        @Test
        void escolheAEstrategiaPeloTipoDaTaxa() {
            assertThat(seletor.obter(TipoRateio.IGUALITARIO)).isInstanceOf(RateioIgualitarioStrategy.class);
            assertThat(seletor.obter(TipoRateio.FRACAO_IDEAL)).isInstanceOf(RateioPorFracaoIdealStrategy.class);
        }

        @Test
        void tipoSemEstrategiaViraErroDeNegocio() {
            SeletorDeRateio soIgualitario = new SeletorDeRateio(List.of(new RateioIgualitarioStrategy()));

            assertThatThrownBy(() -> soIgualitario.obter(TipoRateio.FRACAO_IDEAL))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nao suportado");
            assertThatThrownBy(() -> seletor.obter(null)).isInstanceOf(RegraDeNegocioException.class);
        }
    }
}
