package org.morah.morah.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.template.NotificacaoDeDanoEmReserva;
import org.morah.morah.notificacao.template.NotificacaoDeReservaConfirmada;
import org.morah.morah.ocorrencia.dto.NovaOcorrencia;
import org.morah.morah.ocorrencia.modelo.OrigemOcorrencia;
import org.morah.morah.ocorrencia.servico.OcorrenciaService;
import org.morah.morah.reserva.dto.ReservaCreateRequest;
import org.morah.morah.reserva.dto.ReservaResponse;
import org.morah.morah.reserva.dto.VistoriaRequest;
import org.morah.morah.reserva.modelo.AreaComum;
import org.morah.morah.reserva.modelo.MomentoVistoria;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.modelo.StatusReserva;
import org.morah.morah.reserva.modelo.Vistoria;
import org.morah.morah.reserva.repositorio.ReservaRepository;
import org.morah.morah.reserva.servico.AreaComumService;
import org.morah.morah.reserva.servico.ReservaService;
import org.morah.morah.reserva.strategy.PedidoDeReserva;
import org.morah.morah.reserva.strategy.RegraDeReservaStrategy;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Regras do {@link ReservaService} com o banco substituido por mocks.
 *
 * <p>As regras de validacao (Strategy) sao representadas por um mock: aqui interessa que o
 * service aplica a lista; cada regra tem o seu proprio teste em {@link RegrasDeReservaTest}.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final UsuarioAutenticado ANA = new UsuarioAutenticado(
            1L, "Ana Souza", "11111111111", Perfil.MORADOR, 1L, "Residencial Morah", 101L, "jti");
    private static final UsuarioAutenticado VIZINHO = new UsuarioAutenticado(
            5L, "Vizinho", "55555555555", Perfil.MORADOR, 1L, "Residencial Morah", 102L, "jti");
    private static final UsuarioAutenticado JOANA = new UsuarioAutenticado(
            3L, "Joana Reis", "33333333333", Perfil.PORTARIA, 1L, "Residencial Morah", null, "jti");
    private static final UsuarioAutenticado CARLOS = new UsuarioAutenticado(
            2L, "Carlos Lima", "22222222222", Perfil.SINDICO, 1L, "Residencial Morah", null, "jti");

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private AreaComumService areaComumService;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private OcorrenciaService ocorrenciaService;
    @Mock
    private NotificacaoDeReservaConfirmada notificacaoDeReservaConfirmada;
    @Mock
    private NotificacaoDeDanoEmReserva notificacaoDeDanoEmReserva;
    @Mock
    private RegraDeReservaStrategy regra;

    private ReservaService service;

    @BeforeEach
    void preparar() {
        service = new ReservaService(reservaRepository, areaComumService, usuarioRepository, ocorrenciaService,
                notificacaoDeReservaConfirmada, notificacaoDeDanoEmReserva, List.of(regra));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private static void logarComo(UsuarioAutenticado usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private static Usuario usuario(Long id, String nome, String telefone) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setTelefone(telefone);
        return usuario;
    }

    private static Reserva reservaDaAna(Long id, StatusReserva status, Instant inicio) {
        Reserva reserva = new Reserva();
        reserva.setId(id);
        reserva.setCondominioId(1L);
        reserva.setAreaComumId(1L);
        reserva.setAreaComumNome("Salao de festas");
        reserva.setUnidadeId(101L);
        reserva.setSolicitanteId(1L);
        reserva.setSolicitanteNome("Ana Souza");
        reserva.setInicio(inicio);
        reserva.setFim(inicio.plus(4, ChronoUnit.HOURS));
        reserva.setStatus(status);
        reserva.setValor(new BigDecimal("150.00"));
        return reserva;
    }

    private static Instant daquiA(long horas) {
        return Instant.now().plus(horas, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MINUTES);
    }

    // =====================================================================
    @Nested
    @DisplayName("criar (Template Method + Strategy)")
    class Criar {

        private final Instant inicio = daquiA(48);
        private final Instant fim = inicio.plus(4, ChronoUnit.HOURS);
        private final ReservaCreateRequest requisicao = new ReservaCreateRequest(1L, inicio, fim);
        private AreaComum salao;

        @BeforeEach
        void prepararArea() {
            logarComo(ANA);
            salao = new AreaComum();
            salao.setId(1L);
            salao.setCondominioId(1L);
            salao.setNome("Salao de festas");
            salao.setTaxa(new BigDecimal("150"));
            when(areaComumService.buscarDoCondominio(1L, 1L)).thenReturn(salao);
        }

        /** O save "grava" e devolve a reserva com o id informado, como o Mongo faria. */
        private void saveGeraOId(long id) {
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(chamada -> {
                Reserva reserva = chamada.getArgument(0);
                reserva.setId(id);
                return reserva;
            });
        }

        @Test
        @DisplayName("aplica as regras, completa com token e taxa da area, grava e avisa a unidade")
        void criaENotifica() {
            saveGeraOId(10L);
            when(reservaRepository.listarConfirmadasQueSobrepoem(1L, inicio, fim)).thenAnswer(chamada -> List.of());
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "Ana Souza", "51999990001")));
            Usuario ana = usuario(1L, "Ana Souza", null);
            Usuario bruno = usuario(4L, "Bruno Alves", null);
            when(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(101L)).thenReturn(List.of(ana, bruno));

            ReservaResponse resposta = service.criar(requisicao);

            ArgumentCaptor<PedidoDeReserva> pedido = ArgumentCaptor.forClass(PedidoDeReserva.class);
            verify(regra).validar(pedido.capture());
            assertThat(pedido.getValue().area()).isSameAs(salao);
            assertThat(pedido.getValue().inicio()).isEqualTo(inicio);
            assertThat(pedido.getValue().fim()).isEqualTo(fim);

            assertThat(resposta.id()).isEqualTo(10L);
            assertThat(resposta.status()).isEqualTo(StatusReserva.CONFIRMADA);
            assertThat(resposta.unidadeId()).isEqualTo(101L);
            assertThat(resposta.areaComumNome()).isEqualTo("Salao de festas");
            assertThat(resposta.valor()).isEqualTo(new BigDecimal("150.00"));
            assertThat(resposta.solicitante().nome()).isEqualTo("Ana Souza");
            assertThat(resposta.solicitante().telefone()).isEqualTo("51999990001");

            verify(notificacaoDeReservaConfirmada).enviar(eq(ana), any(Reserva.class));
            verify(notificacaoDeReservaConfirmada).enviar(eq(bruno), any(Reserva.class));
        }

        @Test
        @DisplayName("se uma regra barra o pedido, nada e gravado")
        void regraBarra() {
            doThrow(new RegraDeNegocioException("Horario indisponivel"))
                    .when(regra).validar(any());

            assertThatThrownBy(() -> service.criar(requisicao)).isInstanceOf(RegraDeNegocioException.class);
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("concorrencia: outra reserva sobreposta com id menor gravou antes -> desfaz a nossa e 409")
        void perdeuAConcorrencia() {
            saveGeraOId(11L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
            Reserva quemGravouAntes = reservaDaAna(10L, StatusReserva.CONFIRMADA, inicio);
            when(reservaRepository.listarConfirmadasQueSobrepoem(1L, inicio, fim)).thenAnswer(chamada -> {
                Reserva nossa = reservaDaAna(11L, StatusReserva.CONFIRMADA, inicio);
                return List.of(quemGravouAntes, nossa);
            });

            assertThatThrownBy(() -> service.criar(requisicao))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageStartingWith("Horario indisponivel");

            ArgumentCaptor<Reserva> apagada = ArgumentCaptor.forClass(Reserva.class);
            verify(reservaRepository).delete(apagada.capture());
            assertThat(apagada.getValue().getId()).isEqualTo(11L);
            verify(notificacaoDeReservaConfirmada, never()).enviar(any(), any());
        }

        @Test
        @DisplayName("concorrencia: a outra sobreposta tem id maior -> a nossa vence e e mantida")
        void ganhouAConcorrencia() {
            saveGeraOId(10L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());
            when(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(101L)).thenReturn(List.of());
            when(reservaRepository.listarConfirmadasQueSobrepoem(1L, inicio, fim)).thenAnswer(chamada -> List.of(
                    reservaDaAna(10L, StatusReserva.CONFIRMADA, inicio),
                    reservaDaAna(11L, StatusReserva.CONFIRMADA, inicio)));

            ReservaResponse resposta = service.criar(requisicao);

            assertThat(resposta.id()).isEqualTo(10L);
            verify(reservaRepository, never()).delete(any());
        }

        @Test
        @DisplayName("area de outro condominio -> 404 antes de aplicar as regras")
        void areaDeOutroCondominio() {
            when(areaComumService.buscarDoCondominio(1L, 1L))
                    .thenThrow(new RecursoNaoEncontradoException("Area comum", 1L));

            assertThatThrownBy(() -> service.criar(requisicao)).isInstanceOf(RecursoNaoEncontradoException.class);
            verify(regra, never()).validar(any());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("cancelar")
    class Cancelar {

        @Test
        @DisplayName("reserva confirmada e futura vira CANCELADA (o documento nao e apagado)")
        void cancela() {
            logarComo(ANA);
            Reserva reserva = reservaDaAna(7L, StatusReserva.CONFIRMADA, daquiA(24));
            when(reservaRepository.findByIdAndCondominioId(7L, 1L)).thenReturn(Optional.of(reserva));

            service.cancelar(7L);

            assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CANCELADA);
            assertThat(reserva.getCanceladaEm()).isNotNull();
            verify(reservaRepository).save(reserva);
            verify(reservaRepository, never()).delete(any());
        }

        @Test
        @DisplayName("morador de outra unidade -> 403")
        void outraUnidade() {
            logarComo(VIZINHO);
            when(reservaRepository.findByIdAndCondominioId(7L, 1L))
                    .thenReturn(Optional.of(reservaDaAna(7L, StatusReserva.CONFIRMADA, daquiA(24))));

            assertThatThrownBy(() -> service.cancelar(7L)).isInstanceOf(AcessoNegadoException.class);
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("reserva que ja comecou -> 409")
        void jaComecou() {
            logarComo(ANA);
            when(reservaRepository.findByIdAndCondominioId(7L, 1L))
                    .thenReturn(Optional.of(reservaDaAna(7L, StatusReserva.CONFIRMADA, daquiA(-1))));

            assertThatThrownBy(() -> service.cancelar(7L))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("ja comecou");
        }

        @Test
        @DisplayName("reserva ja cancelada -> 409")
        void jaCancelada() {
            logarComo(ANA);
            when(reservaRepository.findByIdAndCondominioId(7L, 1L))
                    .thenReturn(Optional.of(reservaDaAna(7L, StatusReserva.CANCELADA, daquiA(24))));

            assertThatThrownBy(() -> service.cancelar(7L)).isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        @DisplayName("reserva de outro condominio -> 404")
        void outroCondominio() {
            logarComo(ANA);
            when(reservaRepository.findByIdAndCondominioId(7L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelar(7L)).isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("vistoria (portaria)")
    class RegistrarVistoria {

        @BeforeEach
        void logarPortaria() {
            logarComo(JOANA);
        }

        private Reserva confirmada() {
            Reserva reserva = reservaDaAna(7L, StatusReserva.CONFIRMADA, daquiA(-4));
            when(reservaRepository.findByIdAndCondominioId(7L, 1L)).thenReturn(Optional.of(reserva));
            when(reservaRepository.save(reserva)).thenReturn(reserva);
            return reserva;
        }

        @Test
        @DisplayName("vistoria de entrada sem dano: grava a vistoria e a reserva continua CONFIRMADA")
        void entrada() {
            Reserva reserva = confirmada();

            ReservaResponse resposta = service.registrarVistoria(7L,
                    new VistoriaRequest(MomentoVistoria.ENTRADA, "Tudo em ordem", null, null));

            assertThat(resposta.status()).isEqualTo(StatusReserva.CONFIRMADA);
            assertThat(reserva.getVistorias()).singleElement().satisfies(vistoria -> {
                assertThat(vistoria.getMomento()).isEqualTo(MomentoVistoria.ENTRADA);
                assertThat(vistoria.isDano()).isFalse();
                assertThat(vistoria.getRegistradaPorNome()).isEqualTo("Joana Reis");
            });
            verify(ocorrenciaService, never()).abrir(any());
        }

        @Test
        @DisplayName("vistoria de saida conclui a reserva")
        void saidaConclui() {
            confirmada();

            ReservaResponse resposta = service.registrarVistoria(7L,
                    new VistoriaRequest(MomentoVistoria.SAIDA, null, false, List.of()));

            assertThat(resposta.status()).isEqualTo(StatusReserva.CONCLUIDA);
        }

        @Test
        @DisplayName("com dano: abre ocorrencia de origem RESERVA e avisa a unidade")
        void danoAbreOcorrencia() {
            confirmada();
            Usuario ana = usuario(1L, "Ana Souza", null);
            when(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(101L)).thenReturn(List.of(ana));

            service.registrarVistoria(7L, new VistoriaRequest(MomentoVistoria.SAIDA, "Mesa quebrada", true,
                    List.of("https://fotos.morah.com.br/1.jpg")));

            ArgumentCaptor<NovaOcorrencia> ocorrencia = ArgumentCaptor.forClass(NovaOcorrencia.class);
            verify(ocorrenciaService).abrir(ocorrencia.capture());
            assertThat(ocorrencia.getValue()).isEqualTo(new NovaOcorrencia(
                    1L, 101L, OrigemOcorrencia.RESERVA, 7L, "Dano em Salao de festas", "Mesa quebrada",
                    3L, "Joana Reis"));
            verify(notificacaoDeDanoEmReserva).enviar(eq(ana), any(Reserva.class));
        }

        @Test
        @DisplayName("segunda vistoria do mesmo momento -> 409")
        void momentoRepetido() {
            Reserva reserva = reservaDaAna(7L, StatusReserva.CONFIRMADA, daquiA(-4));
            Vistoria entrada = new Vistoria();
            entrada.setMomento(MomentoVistoria.ENTRADA);
            reserva.getVistorias().add(entrada);
            when(reservaRepository.findByIdAndCondominioId(7L, 1L)).thenReturn(Optional.of(reserva));

            assertThatThrownBy(() -> service.registrarVistoria(7L,
                    new VistoriaRequest(MomentoVistoria.ENTRADA, null, false, null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("ja foi registrada");
            verify(reservaRepository, never()).save(any());
        }

        @Test
        @DisplayName("reserva que nao esta confirmada (ex.: ja concluida) -> 409")
        void naoConfirmada() {
            when(reservaRepository.findByIdAndCondominioId(7L, 1L))
                    .thenReturn(Optional.of(reservaDaAna(7L, StatusReserva.CONCLUIDA, daquiA(-4))));

            assertThatThrownBy(() -> service.registrarVistoria(7L,
                    new VistoriaRequest(MomentoVistoria.SAIDA, null, false, null)))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("detalhar: morador de outra unidade -> 403; portaria ve qualquer uma do condominio")
        void detalhar() {
            when(reservaRepository.findByIdAndCondominioId(7L, 1L))
                    .thenReturn(Optional.of(reservaDaAna(7L, StatusReserva.CONFIRMADA, daquiA(24))));

            logarComo(VIZINHO);
            assertThatThrownBy(() -> service.detalhar(7L)).isInstanceOf(AcessoNegadoException.class);

            logarComo(JOANA);
            assertThat(service.detalhar(7L).id()).isEqualTo(7L);
        }

        @Test
        @DisplayName("listar: portaria sem data ve a agenda de hoje, por inicio crescente")
        void portariaVeHoje() {
            logarComo(JOANA);
            LocalDate hoje = Datas.hoje();
            Range<Instant> diaDeHoje = Range.rightOpen(Datas.inicioDoDia(hoje), Datas.fimDoDia(hoje));
            Page<Reserva> vazia = new PageImpl<>(List.of());
            when(reservaRepository.findByCondominioIdAndInicioBetween(eq(1L), eq(diaDeHoje), any(Pageable.class)))
                    .thenReturn(vazia);

            service.listarDoContextoAtivo(null, 0, 20);

            ArgumentCaptor<Pageable> paginacao = ArgumentCaptor.forClass(Pageable.class);
            verify(reservaRepository).findByCondominioIdAndInicioBetween(eq(1L), eq(diaDeHoje), paginacao.capture());
            assertThat(paginacao.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "inicio"));
        }

        @Test
        @DisplayName("listar: morador ve so a propria unidade; sem data, as mais distantes primeiro")
        void moradorVeAUnidade() {
            logarComo(ANA);
            when(reservaRepository.findByUnidadeId(eq(101L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(reservaDaAna(7L, StatusReserva.CONFIRMADA, daquiA(24)))));

            var pagina = service.listarDoContextoAtivo(null, 0, 500);

            assertThat(pagina.content()).extracting(ReservaResponse::id).containsExactly(7L);
            ArgumentCaptor<Pageable> paginacao = ArgumentCaptor.forClass(Pageable.class);
            verify(reservaRepository).findByUnidadeId(eq(101L), paginacao.capture());
            assertThat(paginacao.getValue().getPageSize()).isEqualTo(100);
            assertThat(paginacao.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "inicio"));
        }

        @Test
        @DisplayName("listar: sindico sem data ve todas do condominio")
        void sindicoVeOCondominio() {
            logarComo(CARLOS);
            when(reservaRepository.findByCondominioId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

            service.listarDoContextoAtivo(null, 0, 20);

            verify(reservaRepository, times(1)).findByCondominioId(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("dashboard: proximasDaUnidade pede as confirmadas que nao terminaram, por inicio, com limite")
        void proximasDaUnidade() {
            when(reservaRepository.findByUnidadeIdAndStatusAndFimGreaterThan(
                    eq(101L), eq(StatusReserva.CONFIRMADA), any(Instant.class),
                    eq(PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "inicio")))))
                    .thenReturn(List.of(reservaDaAna(7L, StatusReserva.CONFIRMADA, daquiA(24))));

            assertThat(service.proximasDaUnidade(101L, 3)).extracting(ReservaResponse::id).containsExactly(7L);
            assertThat(service.proximasDaUnidade(101L, 0)).isEmpty();
        }

        @Test
        @DisplayName("dashboard: doDia ignora as canceladas e usa o dia inteiro no fuso do condominio")
        void doDia() {
            LocalDate dia = LocalDate.of(2026, 10, 5);
            when(reservaRepository.findByCondominioIdAndStatusNotAndInicioBetween(
                    1L, StatusReserva.CANCELADA,
                    Range.rightOpen(Instant.parse("2026-10-05T03:00:00Z"), Instant.parse("2026-10-06T03:00:00Z")),
                    Sort.by(Sort.Direction.ASC, "inicio")))
                    .thenReturn(List.of(reservaDaAna(7L, StatusReserva.CONFIRMADA, Instant.parse("2026-10-05T13:00:00Z"))));

            assertThat(service.doDia(1L, dia)).extracting(ReservaResponse::id).containsExactly(7L);
        }
    }
}
