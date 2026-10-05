package org.morah.morah.encomenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.encomenda.dto.AutorizacaoRetiradaResponse;
import org.morah.morah.encomenda.modelo.AutorizacaoRetirada;
import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.encomenda.modelo.StatusAutorizacaoRetirada;
import org.morah.morah.encomenda.repositorio.AutorizacaoRetiradaRepository;
import org.morah.morah.encomenda.servico.GeradorDeCodigoDeRetirada;
import org.morah.morah.encomenda.strategy.RetiradaValidada;
import org.morah.morah.encomenda.strategy.ValidacaoAutorizacaoDeTerceiroStrategy;
import org.morah.morah.encomenda.strategy.ValidacaoCodigoDoMoradorStrategy;

/**
 * Cada forma de retirada testada isoladamente: e a vantagem do Strategy - uma regra por classe,
 * um teste por classe, sem precisar montar o service inteiro.
 */
class ValidacaoDeRetiradaStrategyTest {

    private static final Instant AGORA = Instant.parse("2026-10-04T15:00:00Z");

    private final AutorizacaoRetiradaRepository repositorio = mock(AutorizacaoRetiradaRepository.class);
    private final ValidacaoCodigoDoMoradorStrategy codigoDoMorador = new ValidacaoCodigoDoMoradorStrategy();
    private final ValidacaoAutorizacaoDeTerceiroStrategy autorizacaoDeTerceiro =
            new ValidacaoAutorizacaoDeTerceiroStrategy(repositorio);

    // ---------- codigo do morador ----------

    @Test
    @DisplayName("codigo do morador: aceita so o codigo da propria encomenda")
    void codigoDoMorador() {
        Encomenda encomenda = encomenda();

        Optional<RetiradaValidada> certo = codigoDoMorador.validar(encomenda, "123456", AGORA);
        Optional<RetiradaValidada> errado = codigoDoMorador.validar(encomenda, "654321", AGORA);

        assertThat(certo).hasValueSatisfying(retirada -> {
            assertThat(retirada.forma()).isEqualTo("codigo do morador");
            assertThat(retirada.autorizacaoId()).isNull();
            assertThat(retirada.nome()).as("o porteiro informa quem retirou").isNull();
        });
        assertThat(errado).isEmpty();
    }

    // ---------- autorizacao de terceiro ----------

    @Test
    @DisplayName("autorizacao de terceiro: aceita codigo ativo e dentro da validade, com nome e documento")
    void terceiroValido() {
        when(repositorio.findByEncomendaIdAndStatus(10L, StatusAutorizacaoRetirada.ATIVA))
                .thenReturn(List.of(autorizacao(50L, "654321", AGORA.plus(1, ChronoUnit.HOURS))));

        Optional<RetiradaValidada> resultado = autorizacaoDeTerceiro.validar(encomenda(), "654321", AGORA);

        assertThat(resultado).hasValue(new RetiradaValidada("autorizacao de terceiro", 50L, "Marcos", "RG 123"));
    }

    @Test
    @DisplayName("autorizacao de terceiro: codigo com a validade vencida e recusado")
    void terceiroExpirado() {
        when(repositorio.findByEncomendaIdAndStatus(10L, StatusAutorizacaoRetirada.ATIVA))
                .thenReturn(List.of(autorizacao(50L, "654321", AGORA.minus(1, ChronoUnit.MINUTES))));

        assertThat(autorizacaoDeTerceiro.validar(encomenda(), "654321", AGORA)).isEmpty();
    }

    @Test
    @DisplayName("autorizacao de terceiro: o codigo do morador nao e reconhecido por esta estrategia")
    void terceiroNaoReconheceOCodigoDoMorador() {
        when(repositorio.findByEncomendaIdAndStatus(10L, StatusAutorizacaoRetirada.ATIVA))
                .thenReturn(List.of(autorizacao(50L, "654321", AGORA.plus(1, ChronoUnit.HOURS))));

        assertThat(autorizacaoDeTerceiro.validar(encomenda(), "123456", AGORA)).isEmpty();
    }

    @Test
    @DisplayName("autorizacao de terceiro: depois da retirada o codigo vira UTILIZADA (uso unico)")
    void registrarUsoQueimaOCodigo() {
        AutorizacaoRetirada autorizacao = autorizacao(50L, "654321", AGORA.plus(1, ChronoUnit.HOURS));
        when(repositorio.findById(50L)).thenReturn(Optional.of(autorizacao));

        autorizacaoDeTerceiro.registrarUso(encomenda(),
                new RetiradaValidada("autorizacao de terceiro", 50L, "Marcos", "RG 123"), AGORA);

        assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacaoRetirada.UTILIZADA);
        assertThat(autorizacao.getUtilizadaEm()).isEqualTo(AGORA);
        verify(repositorio).save(autorizacao);
    }

    // ---------- status exibido e geracao do codigo ----------

    @Test
    @DisplayName("autorizacao ativa com a validade vencida e exibida como expirada")
    void ativaVencidaApareceComoExpirada() {
        AutorizacaoRetirada vencida = autorizacao(50L, "654321", AGORA.minus(1, ChronoUnit.HOURS));

        assertThat(vencida.statusEm(AGORA)).isEqualTo(StatusAutorizacaoRetirada.EXPIRADA);
        assertThat(AutorizacaoRetiradaResponse.de(vencida, AGORA).status().getValor()).isEqualTo("expirada");
        assertThat(vencida.statusEm(AGORA.minus(2, ChronoUnit.HOURS))).isEqualTo(StatusAutorizacaoRetirada.ATIVA);
    }

    @Test
    @DisplayName("gerador: sempre 6 digitos e nunca repete um codigo em uso")
    void geradorDeCodigo() {
        Deque<String> sorteios = new ArrayDeque<>(List.of("123456", "654321"));
        GeradorDeCodigoDeRetirada viciado = new GeradorDeCodigoDeRetirada() {
            @Override
            public String gerar() {
                return sorteios.pop();
            }
        };

        assertThat(viciado.gerarDiferenteDe(Set.of("123456"))).isEqualTo("654321");

        GeradorDeCodigoDeRetirada real = new GeradorDeCodigoDeRetirada();
        for (int i = 0; i < 200; i++) {
            assertThat(real.gerar()).matches("\\d{6}");
        }
    }

    // ---------- apoio ----------

    private static Encomenda encomenda() {
        Encomenda encomenda = new Encomenda();
        encomenda.setId(10L);
        encomenda.setUnidadeId(101L);
        encomenda.setCodigoRetirada("123456");
        return encomenda;
    }

    private static AutorizacaoRetirada autorizacao(Long id, String codigo, Instant validade) {
        AutorizacaoRetirada autorizacao = new AutorizacaoRetirada();
        autorizacao.setId(id);
        autorizacao.setEncomendaId(10L);
        autorizacao.setNomeTerceiro("Marcos");
        autorizacao.setDocumento("RG 123");
        autorizacao.setCodigo(codigo);
        autorizacao.setValidade(validade);
        autorizacao.setStatus(StatusAutorizacaoRetirada.ATIVA);
        return autorizacao;
    }
}
