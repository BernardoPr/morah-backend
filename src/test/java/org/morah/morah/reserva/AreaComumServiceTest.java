package org.morah.morah.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.reserva.dto.DisponibilidadeResponse;
import org.morah.morah.reserva.dto.DisponibilidadeResponse.Horario;
import org.morah.morah.reserva.modelo.AreaComum;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.repositorio.AreaComumRepository;
import org.morah.morah.reserva.repositorio.ReservaRepository;
import org.morah.morah.reserva.servico.AreaComumService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Geracao da grade de horarios (GET /areas-comuns/{id}/disponibilidade), sem banco. */
@ExtendWith(MockitoExtension.class)
class AreaComumServiceTest {

    private static final LocalDate AMANHA = Datas.hoje().plusDays(1);

    @Mock
    private AreaComumRepository areaComumRepository;

    @Mock
    private ReservaRepository reservaRepository;

    private AreaComumService service;

    @BeforeEach
    void preparar() {
        service = new AreaComumService(areaComumRepository, reservaRepository);
        var ana = new UsuarioAutenticado(1L, "Ana Souza", "11111111111", Perfil.MORADOR,
                1L, "Residencial Morah", 101L, "jti");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(ana, null, List.of()));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private static Instant as(LocalDate data, String hora) {
        return data.atTime(LocalTime.parse(hora)).atZone(Datas.FUSO).toInstant();
    }

    private AreaComum area(String abertura, String fechamento, Integer duracao) {
        AreaComum area = new AreaComum();
        area.setId(1L);
        area.setCondominioId(1L);
        area.setNome("Salao de festas");
        area.setTaxa(new BigDecimal("150.00"));
        area.setHoraAbertura(abertura);
        area.setHoraFechamento(fechamento);
        area.setDuracaoSlotMinutos(duracao);
        when(areaComumRepository.findByIdAndCondominioId(1L, 1L)).thenReturn(Optional.of(area));
        return area;
    }

    private void semReservasNoDia(LocalDate data) {
        when(reservaRepository.listarConfirmadasQueSobrepoem(1L, Datas.inicioDoDia(data), Datas.fimDoDia(data)))
                .thenReturn(List.of());
    }

    @Test
    @DisplayName("gera os blocos da abertura ao fechamento e marca o ocupado por reserva confirmada")
    void geraHorariosEMarcaOcupado() {
        area("10:00", "22:00", 240);
        Reserva ocupada = new Reserva();
        ocupada.setInicio(as(AMANHA, "14:00"));
        ocupada.setFim(as(AMANHA, "18:00"));
        when(reservaRepository.listarConfirmadasQueSobrepoem(1L, Datas.inicioDoDia(AMANHA), Datas.fimDoDia(AMANHA)))
                .thenReturn(List.of(ocupada));

        DisponibilidadeResponse resposta = service.disponibilidade(1L, AMANHA);

        assertThat(resposta.data()).isEqualTo(AMANHA);
        assertThat(resposta.horarios()).containsExactly(
                new Horario(as(AMANHA, "10:00"), as(AMANHA, "14:00"), true),
                new Horario(as(AMANHA, "14:00"), as(AMANHA, "18:00"), false),
                new Horario(as(AMANHA, "18:00"), as(AMANHA, "22:00"), true));
    }

    @Test
    @DisplayName("os horarios saem em UTC, convertidos do fuso do condominio (10:00 em SP = 13:00Z)")
    void horariosEmUtc() {
        area("10:00", "22:00", 240);
        semReservasNoDia(AMANHA);

        Horario primeiro = service.disponibilidade(1L, AMANHA).horarios().getFirst();

        assertThat(primeiro.inicio()).isEqualTo(AMANHA.atTime(13, 0).toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("quadra: 08:00-22:00 em blocos de 1h gera 14 horarios")
    void blocosDeUmaHora() {
        area("08:00", "22:00", 60);
        semReservasNoDia(AMANHA);

        List<Horario> horarios = service.disponibilidade(1L, AMANHA).horarios();

        assertThat(horarios).hasSize(14).allMatch(Horario::disponivel);
        assertThat(horarios.getLast().fim()).isEqualTo(as(AMANHA, "22:00"));
    }

    @Test
    @DisplayName("se o funcionamento nao e multiplo da duracao, o ultimo bloco e encurtado ate o fechamento")
    void ultimoBlocoEncurtado() {
        area("10:00", "21:00", 240);
        semReservasNoDia(AMANHA);

        List<Horario> horarios = service.disponibilidade(1L, AMANHA).horarios();

        assertThat(horarios).hasSize(3);
        assertThat(horarios.getLast().inicio()).isEqualTo(as(AMANHA, "18:00"));
        assertThat(horarios.getLast().fim()).isEqualTo(as(AMANHA, "21:00"));
    }

    @Test
    @DisplayName("dia que ja passou: nenhum horario disponivel")
    void diaPassado() {
        LocalDate ontem = Datas.hoje().minusDays(1);
        area("10:00", "22:00", 240);
        semReservasNoDia(ontem);

        assertThat(service.disponibilidade(1L, ontem).horarios()).hasSize(3).noneMatch(Horario::disponivel);
    }

    @Test
    @DisplayName("area em manutencao ou de uso livre: grade aparece, mas nada disponivel")
    void areaQueNaoAceitaReserva() {
        AreaComum area = area("10:00", "22:00", 240);
        area.setStatus(AreaComum.EM_MANUTENCAO);
        semReservasNoDia(AMANHA);

        assertThat(service.disponibilidade(1L, AMANHA).horarios()).hasSize(3).noneMatch(Horario::disponivel);

        area.setStatus(AreaComum.ATIVA);
        area.setRequerReserva(false);
        assertThat(service.disponibilidade(1L, AMANHA).horarios()).noneMatch(Horario::disponivel);
    }

    @Test
    @DisplayName("area sem duracao de horario configurada nao tem grade")
    void semDuracao() {
        area("06:00", "23:00", null);
        semReservasNoDia(AMANHA);

        assertThat(service.disponibilidade(1L, AMANHA).horarios()).isEmpty();
    }

    @Test
    @DisplayName("area de outro condominio -> 404")
    void areaDeOutroCondominio() {
        when(areaComumRepository.findByIdAndCondominioId(anyLong(), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disponibilidade(99L, AMANHA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("lista as areas do condominio do token")
    void listaAreas() {
        AreaComum academia = new AreaComum();
        academia.setId(4L);
        academia.setNome("Academia");
        academia.setRequerReserva(false);
        academia.setTaxa(BigDecimal.ZERO);
        when(areaComumRepository.findByCondominioIdOrderByNome(1L)).thenReturn(List.of(academia));

        var areas = service.listarDoCondominioAtivo();

        assertThat(areas).singleElement().satisfies(area -> {
            assertThat(area.nome()).isEqualTo("Academia");
            assertThat(area.requerReserva()).isFalse();
            assertThat(area.taxa()).isEqualByComparingTo("0.00");
            assertThat(area.status()).isEqualTo("ativa");
        });
    }
}
