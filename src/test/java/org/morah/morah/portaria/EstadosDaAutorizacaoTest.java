package org.morah.morah.portaria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.DecisaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.morah.morah.portaria.state.EstadoAutorizada;
import org.morah.morah.portaria.state.EstadoDaAutorizacao;
import org.morah.morah.portaria.state.EstadoPendente;
import org.morah.morah.portaria.state.SeletorDeEstadoDaAutorizacao;

/**
 * Testes do padrao STATE: cada status aceita so as operacoes que fazem sentido para ele e o
 * proprio estado faz a transicao para o proximo.
 */
class EstadosDaAutorizacaoTest {

    private final SeletorDeEstadoDaAutorizacao seletor = PortariaFixtures.seletorDeEstado();

    private EstadoDaAutorizacao estado(StatusAutorizacaoVisita status) {
        return seletor.obter(status);
    }

    // ---------- seletor ----------

    @ParameterizedTest
    @EnumSource(StatusAutorizacaoVisita.class)
    @DisplayName("todo status tem um objeto-estado correspondente")
    void todoStatusTemEstado(StatusAutorizacaoVisita status) {
        AutorizacaoVisita autorizacao = PortariaFixtures.comStatus(1L, status);

        assertThat(seletor.estadoDe(autorizacao).status()).isEqualTo(status);
    }

    @Test
    @DisplayName("faltar um estado impede a aplicacao de subir")
    void faltandoEstadoFalhaNaCriacao() {
        assertThatThrownBy(() -> new SeletorDeEstadoDaAutorizacao(List.of(new EstadoPendente())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTORIZADA");
    }

    // ---------- PENDENTE ----------

    @Test
    @DisplayName("pendente + autorizado -> AUTORIZADA, com data de resposta")
    void pendenteAutorizada() {
        AutorizacaoVisita autorizacao = PortariaFixtures.pendente(1L);
        Instant agora = Instant.now();

        estado(StatusAutorizacaoVisita.PENDENTE).decidir(autorizacao, DecisaoVisita.AUTORIZADO, agora);

        assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacaoVisita.AUTORIZADA);
        assertThat(autorizacao.getRespondidoEm()).isEqualTo(agora);
        // depois da transicao, o seletor ja entrega o novo estado
        assertThat(seletor.estadoDe(autorizacao)).isInstanceOf(EstadoAutorizada.class);
    }

    @Test
    @DisplayName("pendente + recusado -> RECUSADA")
    void pendenteRecusada() {
        AutorizacaoVisita autorizacao = PortariaFixtures.pendente(1L);

        estado(StatusAutorizacaoVisita.PENDENTE).decidir(autorizacao, DecisaoVisita.RECUSADO, Instant.now());

        assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacaoVisita.RECUSADA);
    }

    @Test
    @DisplayName("pendente vencida expira; pendente no prazo continua pendente")
    void expiracaoPreguicosa() {
        AutorizacaoVisita vencida = PortariaFixtures.pendenteVencida(1L);
        AutorizacaoVisita noPrazo = PortariaFixtures.pendente(2L);
        EstadoDaAutorizacao pendente = estado(StatusAutorizacaoVisita.PENDENTE);

        assertThat(vencida.statusEfetivo(Instant.now())).isEqualTo(StatusAutorizacaoVisita.EXPIRADA);
        assertThat(pendente.expirarSeVencida(vencida, Instant.now())).isTrue();
        assertThat(vencida.getStatus()).isEqualTo(StatusAutorizacaoVisita.EXPIRADA);

        assertThat(pendente.expirarSeVencida(noPrazo, Instant.now())).isFalse();
        assertThat(noPrazo.getStatus()).isEqualTo(StatusAutorizacaoVisita.PENDENTE);
    }

    @Test
    @DisplayName("reenviar a pendente renova o prazo de resposta")
    void reenviarRenovaPrazo() {
        AutorizacaoVisita autorizacao = PortariaFixtures.pendente(1L);
        Instant daquiDezMinutos = Instant.now().plus(Duration.ofMinutes(10));

        estado(StatusAutorizacaoVisita.PENDENTE).reenviar(autorizacao, daquiDezMinutos);

        assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacaoVisita.PENDENTE);
        assertThat(autorizacao.getExpiraEm()).isEqualTo(daquiDezMinutos.plus(AutorizacaoVisita.PRAZO_DE_RESPOSTA));
    }

    @Test
    @DisplayName("pendente ainda nao libera a entrada")
    void pendenteNaoLiberaEntrada() {
        assertThatThrownBy(() -> estado(StatusAutorizacaoVisita.PENDENTE)
                .registrarEntrada(PortariaFixtures.pendente(1L), Instant.now()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ainda nao respondeu");
    }

    // ---------- AUTORIZADA ----------

    @Test
    @DisplayName("autorizada libera a entrada uma unica vez")
    void autorizadaLiberaUmaEntrada() {
        AutorizacaoVisita autorizacao = PortariaFixtures.autorizada(1L);
        EstadoDaAutorizacao autorizada = estado(StatusAutorizacaoVisita.AUTORIZADA);
        Instant agora = Instant.now();

        autorizada.registrarEntrada(autorizacao, agora);
        assertThat(autorizacao.getEntradaEm()).isEqualTo(agora);
        assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacaoVisita.AUTORIZADA);

        assertThatThrownBy(() -> autorizada.registrarEntrada(autorizacao, Instant.now()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja foi usada");
    }

    @Test
    @DisplayName("autorizada ha mais de 12 horas nao libera a entrada")
    void autorizadaVencida() {
        AutorizacaoVisita autorizacao = PortariaFixtures.autorizada(1L);
        autorizacao.setRespondidoEm(Instant.now().minus(EstadoAutorizada.PRAZO_PARA_ENTRADA).minusSeconds(60));

        assertThatThrownBy(() -> estado(StatusAutorizacaoVisita.AUTORIZADA).registrarEntrada(autorizacao, Instant.now()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("venceu");
    }

    @Test
    @DisplayName("autorizada nao pode ser decidida de novo nem reenviada")
    void autorizadaEFinal() {
        AutorizacaoVisita autorizacao = PortariaFixtures.autorizada(1L);
        EstadoDaAutorizacao autorizada = estado(StatusAutorizacaoVisita.AUTORIZADA);

        assertThatThrownBy(() -> autorizada.decidir(autorizacao, DecisaoVisita.RECUSADO, Instant.now()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja esta autorizada");
        assertThatThrownBy(() -> autorizada.reenviar(autorizacao, Instant.now()))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(autorizada.expirarSeVencida(autorizacao, Instant.now().plus(Duration.ofDays(1)))).isFalse();
    }

    // ---------- RECUSADA e EXPIRADA ----------

    @ParameterizedTest
    @EnumSource(value = StatusAutorizacaoVisita.class, names = { "RECUSADA", "EXPIRADA" })
    @DisplayName("recusada e expirada sao finais: nada e aceito")
    void estadosFinais(StatusAutorizacaoVisita status) {
        AutorizacaoVisita autorizacao = PortariaFixtures.comStatus(1L, status);
        EstadoDaAutorizacao estado = estado(status);
        Instant agora = Instant.now();

        assertThatThrownBy(() -> estado.decidir(autorizacao, DecisaoVisita.AUTORIZADO, agora))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> estado.reenviar(autorizacao, agora))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> estado.registrarEntrada(autorizacao, agora))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(autorizacao.getStatus()).isEqualTo(status);
    }
}
