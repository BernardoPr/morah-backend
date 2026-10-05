package org.morah.morah.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.reserva.modelo.AreaComum;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.repositorio.ReservaRepository;
import org.morah.morah.reserva.strategy.PedidoDeReserva;
import org.morah.morah.reserva.strategy.RegraDeAreaDisponivel;
import org.morah.morah.reserva.strategy.RegraDeConflitoDeHorario;
import org.morah.morah.reserva.strategy.RegraDeHorarioDeFuncionamento;
import org.morah.morah.reserva.strategy.RegraDeInicioNoFuturo;
import org.morah.morah.reserva.strategy.RegraDePeriodoValido;
import org.morah.morah.reserva.strategy.RegraDeReservaStrategy;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Cada regra do STRATEGY de reserva testada isoladamente - a vantagem de uma regra por classe:
 * nao precisa subir o service nem o Spring para testar uma regra.
 */
class RegrasDeReservaTest {

    /** "Agora" fixo: 04/10/2026 12:00 em Sao Paulo (15:00 UTC). */
    private static final Instant AGORA = local("2026-10-04T12:00");

    private static Instant local(String dataHora) {
        return LocalDateTime.parse(dataHora).atZone(Datas.FUSO).toInstant();
    }

    private static AreaComum salao() {
        AreaComum area = new AreaComum();
        area.setId(1L);
        area.setCondominioId(1L);
        area.setNome("Salao de festas");
        area.setTaxa(new BigDecimal("150.00"));
        area.setRequerReserva(true);
        area.setStatus(AreaComum.ATIVA);
        area.setHoraAbertura("10:00");
        area.setHoraFechamento("22:00");
        area.setDuracaoSlotMinutos(240);
        return area;
    }

    private static PedidoDeReserva pedido(AreaComum area, String inicio, String fim) {
        return new PedidoDeReserva(area, local(inicio), local(fim), AGORA);
    }

    // ---------- regra 1: area disponivel ----------

    @Test
    @DisplayName("area em manutencao nao aceita reserva")
    void areaEmManutencao() {
        AreaComum area = salao();
        area.setStatus(AreaComum.EM_MANUTENCAO);

        assertThatThrownBy(() -> new RegraDeAreaDisponivel().validar(pedido(area, "2026-10-05T10:00", "2026-10-05T14:00")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("manutencao");
    }

    @Test
    @DisplayName("area de uso livre (requerReserva = false) nao aceita reserva")
    void areaDeUsoLivre() {
        AreaComum area = salao();
        area.setRequerReserva(false);

        assertThatThrownBy(() -> new RegraDeAreaDisponivel().validar(pedido(area, "2026-10-05T10:00", "2026-10-05T14:00")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("uso livre");
    }

    @Test
    @DisplayName("area ativa que exige reserva passa")
    void areaAtiva() {
        assertThatCode(() -> new RegraDeAreaDisponivel().validar(pedido(salao(), "2026-10-05T10:00", "2026-10-05T14:00")))
                .doesNotThrowAnyException();
    }

    // ---------- regra 2: periodo valido ----------

    @Test
    @DisplayName("fim antes do inicio (ou igual) e recusado")
    void fimAntesDoInicio() {
        RegraDePeriodoValido regra = new RegraDePeriodoValido();

        assertThatThrownBy(() -> regra.validar(pedido(salao(), "2026-10-05T14:00", "2026-10-05T10:00")))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> regra.validar(pedido(salao(), "2026-10-05T14:00", "2026-10-05T14:00")))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // ---------- regra 3: inicio no futuro ----------

    @Test
    @DisplayName("reserva que comeca no passado e recusada")
    void inicioNoPassado() {
        assertThatThrownBy(() -> new RegraDeInicioNoFuturo().validar(pedido(salao(), "2026-10-04T10:00", "2026-10-04T14:00")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    @DisplayName("reserva que comeca depois de agora passa")
    void inicioNoFuturo() {
        assertThatCode(() -> new RegraDeInicioNoFuturo().validar(pedido(salao(), "2026-10-04T14:00", "2026-10-04T18:00")))
                .doesNotThrowAnyException();
    }

    // ---------- regra 4: horario de funcionamento ----------

    @Test
    @DisplayName("antes da abertura ou depois do fechamento e recusado")
    void foraDoHorario() {
        RegraDeHorarioDeFuncionamento regra = new RegraDeHorarioDeFuncionamento();

        assertThatThrownBy(() -> regra.validar(pedido(salao(), "2026-10-05T09:00", "2026-10-05T11:00")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Fora do horario");
        assertThatThrownBy(() -> regra.validar(pedido(salao(), "2026-10-05T20:00", "2026-10-05T22:30")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Fora do horario");
    }

    @Test
    @DisplayName("reserva que atravessa a meia-noite e recusada")
    void diasDiferentes() {
        AreaComum area = salao();
        area.setHoraAbertura("00:00");
        area.setHoraFechamento("23:59");

        assertThatThrownBy(() -> new RegraDeHorarioDeFuncionamento()
                .validar(pedido(area, "2026-10-05T21:00", "2026-10-06T01:00")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("mesmo dia");
    }

    @Test
    @DisplayName("compara na hora local do condominio: 13:00 UTC sao 10:00 em Sao Paulo")
    void usaOFusoDoCondominio() {
        RegraDeHorarioDeFuncionamento regra = new RegraDeHorarioDeFuncionamento();
        var dentro = new PedidoDeReserva(salao(),
                Instant.parse("2026-10-05T13:00:00Z"), Instant.parse("2026-10-06T01:00:00Z"), AGORA);
        var umaHoraAntes = new PedidoDeReserva(salao(),
                Instant.parse("2026-10-05T12:00:00Z"), Instant.parse("2026-10-05T16:00:00Z"), AGORA);

        assertThatCode(() -> regra.validar(dentro)).doesNotThrowAnyException();
        assertThatThrownBy(() -> regra.validar(umaHoraAntes)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("area sem horario de funcionamento nao aceita reserva")
    void semHorarioConfigurado() {
        AreaComum area = salao();
        area.setHoraAbertura(null);

        assertThatThrownBy(() -> new RegraDeHorarioDeFuncionamento()
                .validar(pedido(area, "2026-10-05T10:00", "2026-10-05T14:00")))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // ---------- regra 5: conflito ----------

    @Test
    @DisplayName("outra reserva confirmada no periodo -> 409 Horario indisponivel")
    void conflito() {
        ReservaRepository repositorio = mock(ReservaRepository.class);
        PedidoDeReserva pedido = pedido(salao(), "2026-10-05T10:00", "2026-10-05T14:00");
        when(repositorio.listarConfirmadasQueSobrepoem(1L, pedido.inicio(), pedido.fim()))
                .thenReturn(List.of(new Reserva()));

        assertThatThrownBy(() -> new RegraDeConflitoDeHorario(repositorio).validar(pedido))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageStartingWith("Horario indisponivel");
    }

    @Test
    @DisplayName("sem reserva confirmada no periodo, passa")
    void semConflito() {
        ReservaRepository repositorio = mock(ReservaRepository.class);
        PedidoDeReserva pedido = pedido(salao(), "2026-10-05T10:00", "2026-10-05T14:00");
        when(repositorio.listarConfirmadasQueSobrepoem(1L, pedido.inicio(), pedido.fim())).thenReturn(List.of());

        assertThatCode(() -> new RegraDeConflitoDeHorario(repositorio).validar(pedido)).doesNotThrowAnyException();
    }

    // ---------- a lista que o Spring injeta ----------

    @Test
    @DisplayName("o @Order define a sequencia: a regra que consulta o banco roda por ultimo")
    void ordemDasRegras() {
        List<RegraDeReservaStrategy> regras = new ArrayList<>(List.of(
                new RegraDeConflitoDeHorario(mock(ReservaRepository.class)),
                new RegraDeHorarioDeFuncionamento(),
                new RegraDeAreaDisponivel(),
                new RegraDeInicioNoFuturo(),
                new RegraDePeriodoValido()));
        Collections.shuffle(regras);

        // Mesmo comparador que o Spring usa ao injetar uma List<RegraDeReservaStrategy>.
        regras.sort(AnnotationAwareOrderComparator.INSTANCE);

        assertThat(regras).extracting(regra -> regra.getClass().getSimpleName()).containsExactly(
                "RegraDeAreaDisponivel",
                "RegraDePeriodoValido",
                "RegraDeInicioNoFuturo",
                "RegraDeHorarioDeFuncionamento",
                "RegraDeConflitoDeHorario");
    }
}
