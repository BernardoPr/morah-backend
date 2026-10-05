package org.morah.morah.portaria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.notificacao.template.NotificacaoDeDecisaoDeVisita;
import org.morah.morah.notificacao.template.NotificacaoDeVisitanteAguardando;
import org.morah.morah.portaria.dto.AutorizacaoVisitaCreateRequest;
import org.morah.morah.portaria.dto.AutorizacaoVisitaResponse;
import org.morah.morah.portaria.dto.DecisaoAutorizacaoRequest;
import org.morah.morah.portaria.dto.VisitanteInput;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.DecisaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.morah.morah.portaria.repositorio.AutorizacaoVisitaRepository;
import org.morah.morah.portaria.servico.AutorizacaoVisitaService;
import org.morah.morah.portaria.servico.VisitanteService;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Regras do {@link AutorizacaoVisitaService} com o banco substituido por mocks. */
@ExtendWith(MockitoExtension.class)
class AutorizacaoVisitaServiceTest {

    @Mock
    private AutorizacaoVisitaRepository autorizacaoRepository;
    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private VisitanteService visitanteService;
    @Mock
    private NotificacaoDeVisitanteAguardando notificacaoDeVisitanteAguardando;
    @Mock
    private NotificacaoDeDecisaoDeVisita notificacaoDeDecisaoDeVisita;

    private AutorizacaoVisitaService service;

    private final Pageable paginacao = PageRequest.of(0, 20);

    @BeforeEach
    void montarService() {
        service = new AutorizacaoVisitaService(autorizacaoRepository, unidadeRepository, usuarioRepository,
                visitanteService, PortariaFixtures.seletorDeEstado(),
                notificacaoDeVisitanteAguardando, notificacaoDeDecisaoDeVisita);
    }

    private void salvarDevolveOProprioObjeto() {
        when(autorizacaoRepository.save(any(AutorizacaoVisita.class))).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private static Usuario usuario(Long id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        return usuario;
    }

    // ---------- registrar visitante ----------

    @Test
    @DisplayName("registrar: cria a autorizacao PENDENTE com prazo de 30 minutos e avisa a unidade")
    void registrarVisitante() {
        Unidade unidade = new Unidade();
        unidade.setId(PortariaFixtures.APTO_101);
        unidade.setBlocoNome("Bloco A");
        unidade.setIdentificacao("Apto 101");
        Usuario ana = usuario(1L, "Ana Souza");
        Usuario bruno = usuario(4L, "Bruno Alves");
        var visitanteInput = new VisitanteInput("Joao Visitante", "123.456.789", null);

        when(unidadeRepository.findByIdAndCondominioId(PortariaFixtures.APTO_101, PortariaFixtures.CONDOMINIO))
                .thenReturn(Optional.of(unidade));
        when(visitanteService.registrar(PortariaFixtures.CONDOMINIO, visitanteInput))
                .thenReturn(PortariaFixtures.visitante(10L, "Joao Visitante"));
        when(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(PortariaFixtures.APTO_101))
                .thenReturn(List.of(ana, bruno));
        salvarDevolveOProprioObjeto();

        AutorizacaoVisitaResponse resposta = service.registrarVisitante(PortariaFixtures.porteira(),
                new AutorizacaoVisitaCreateRequest(PortariaFixtures.APTO_101, visitanteInput, " Entrega de moveis ", null));

        ArgumentCaptor<AutorizacaoVisita> salva = ArgumentCaptor.forClass(AutorizacaoVisita.class);
        verify(autorizacaoRepository).save(salva.capture());
        AutorizacaoVisita autorizacao = salva.getValue();

        assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacaoVisita.PENDENTE);
        assertThat(autorizacao.getCondominioId()).isEqualTo(PortariaFixtures.CONDOMINIO);
        assertThat(autorizacao.getUnidadeDescricao()).isEqualTo("Bloco A - Apto 101");
        assertThat(autorizacao.getVisitanteId()).isEqualTo(10L);
        assertThat(autorizacao.getRegistradoPorNome()).isEqualTo("Joana Reis");
        assertThat(autorizacao.getMotivo()).isEqualTo("Entrega de moveis");
        assertThat(autorizacao.getExpiraEm())
                .isCloseTo(autorizacao.getSolicitadoEm().plus(AutorizacaoVisita.PRAZO_DE_RESPOSTA), within(1, ChronoUnit.SECONDS));

        verify(notificacaoDeVisitanteAguardando).enviar(ana, autorizacao);
        verify(notificacaoDeVisitanteAguardando).enviar(bruno, autorizacao);

        assertThat(resposta.status()).isEqualTo(StatusAutorizacaoVisita.PENDENTE);
        assertThat(resposta.visitante().nome()).isEqualTo("Joao Visitante");
        assertThat(resposta.unidadeId()).isEqualTo(PortariaFixtures.APTO_101);
    }

    @Test
    @DisplayName("registrar: unidade de outro condominio -> 404, nada e gravado")
    void registrarUnidadeDeOutroCondominio() {
        when(unidadeRepository.findByIdAndCondominioId(999L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.empty());

        var requisicao = new AutorizacaoVisitaCreateRequest(999L, new VisitanteInput("Joao", null, null), "Visita", null);

        assertThatThrownBy(() -> service.registrarVisitante(PortariaFixtures.porteira(), requisicao))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Unidade");
        verifyNoInteractions(visitanteService, autorizacaoRepository, notificacaoDeVisitanteAguardando);
    }

    // ---------- listar / detalhar ----------

    @Test
    @DisplayName("listar: portaria ve a fila do condominio; pendente vencida aparece como expirada")
    void listarComoPortaria() {
        when(autorizacaoRepository.findByCondominioId(PortariaFixtures.CONDOMINIO, paginacao))
                .thenReturn(new PageImpl<>(List.of(PortariaFixtures.pendente(1L), PortariaFixtures.pendenteVencida(2L))));

        PaginaResponse<AutorizacaoVisitaResponse> pagina = service.listar(PortariaFixtures.porteira(), null, paginacao);

        assertThat(pagina.content()).extracting(AutorizacaoVisitaResponse::status)
                .containsExactly(StatusAutorizacaoVisita.PENDENTE, StatusAutorizacaoVisita.EXPIRADA);
    }

    @Test
    @DisplayName("listar: morador ve so a propria unidade, com o filtro de status efetivo")
    void listarComoMorador() {
        when(autorizacaoRepository.findByUnidadeIdAndStatusAndExpiraEmAfter(
                eq(PortariaFixtures.APTO_101), eq(StatusAutorizacaoVisita.PENDENTE), any(Instant.class), eq(paginacao)))
                .thenReturn(new PageImpl<>(List.of(PortariaFixtures.pendente(1L))));
        when(autorizacaoRepository.buscarExpiradasDaUnidade(eq(PortariaFixtures.APTO_101), any(Instant.class), eq(paginacao)))
                .thenReturn(new PageImpl<>(List.of(PortariaFixtures.pendenteVencida(2L))));
        when(autorizacaoRepository.findByUnidadeIdAndStatus(PortariaFixtures.APTO_101, StatusAutorizacaoVisita.RECUSADA, paginacao))
                .thenReturn(new PageImpl<>(List.of()));

        var morador = PortariaFixtures.moradora101();
        assertThat(service.listar(morador, StatusAutorizacaoVisita.PENDENTE, paginacao).content()).hasSize(1);
        assertThat(service.listar(morador, StatusAutorizacaoVisita.EXPIRADA, paginacao).content())
                .extracting(AutorizacaoVisitaResponse::status).containsExactly(StatusAutorizacaoVisita.EXPIRADA);
        assertThat(service.listar(morador, StatusAutorizacaoVisita.RECUSADA, paginacao).content()).isEmpty();

        verify(autorizacaoRepository, never()).findByCondominioId(any(), any());
    }

    @Test
    @DisplayName("detalhar: morador de outra unidade -> 403; outro condominio -> 404")
    void detalharComAcessoRestrito() {
        AutorizacaoVisita doApto102 = PortariaFixtures.pendente(1L);
        doApto102.setUnidadeId(PortariaFixtures.APTO_102);
        when(autorizacaoRepository.findByIdAndCondominioId(1L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.of(doApto102));
        when(autorizacaoRepository.findByIdAndCondominioId(2L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detalhar(PortariaFixtures.moradora101(), 1L))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.detalhar(PortariaFixtures.porteira(), 2L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        // a portaria enxerga qualquer unidade do condominio
        assertThat(service.detalhar(PortariaFixtures.porteira(), 1L).unidadeId()).isEqualTo(PortariaFixtures.APTO_102);
    }

    // ---------- decidir ----------

    @Test
    @DisplayName("decidir: autoriza, registra quem respondeu e avisa os porteiros")
    void decidirAutorizando() {
        AutorizacaoVisita autorizacao = PortariaFixtures.pendente(1L);
        Usuario joana = usuario(3L, "Joana Reis");
        when(autorizacaoRepository.findByIdAndCondominioId(1L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.of(autorizacao));
        when(usuarioRepository.listarPorPerfilNoCondominio(PortariaFixtures.CONDOMINIO, Perfil.PORTARIA))
                .thenReturn(List.of(joana));
        salvarDevolveOProprioObjeto();

        AutorizacaoVisitaResponse resposta = service.decidir(PortariaFixtures.proprietario101(), 1L,
                new DecisaoAutorizacaoRequest(DecisaoVisita.AUTORIZADO, "Pode subir"));

        assertThat(resposta.status()).isEqualTo(StatusAutorizacaoVisita.AUTORIZADA);
        assertThat(resposta.respondidoEm()).isNotNull();
        assertThat(autorizacao.getRespondidoPorNome()).isEqualTo("Bruno Alves");
        assertThat(autorizacao.getJustificativa()).isEqualTo("Pode subir");
        verify(notificacaoDeDecisaoDeVisita).enviar(joana, autorizacao);
    }

    @Test
    @DisplayName("decidir: autorizacao de outra unidade -> 403")
    void decidirOutraUnidade() {
        AutorizacaoVisita doApto102 = PortariaFixtures.pendente(1L);
        doApto102.setUnidadeId(PortariaFixtures.APTO_102);
        when(autorizacaoRepository.findByIdAndCondominioId(1L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.of(doApto102));

        assertThatThrownBy(() -> service.decidir(PortariaFixtures.moradora101(), 1L,
                new DecisaoAutorizacaoRequest(DecisaoVisita.AUTORIZADO, null)))
                .isInstanceOf(AcessoNegadoException.class);
        verify(autorizacaoRepository, never()).save(any());
        verifyNoInteractions(notificacaoDeDecisaoDeVisita);
    }

    @Test
    @DisplayName("decidir: autorizacao ja decidida -> 409")
    void decidirJaDecidida() {
        when(autorizacaoRepository.findByIdAndCondominioId(1L, PortariaFixtures.CONDOMINIO))
                .thenReturn(Optional.of(PortariaFixtures.comStatus(1L, StatusAutorizacaoVisita.RECUSADA)));

        assertThatThrownBy(() -> service.decidir(PortariaFixtures.moradora101(), 1L,
                new DecisaoAutorizacaoRequest(DecisaoVisita.AUTORIZADO, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja esta recusada");
        verify(autorizacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("decidir: pendente com prazo vencido -> 409 e fica gravada como EXPIRADA")
    void decidirExpirada() {
        AutorizacaoVisita vencida = PortariaFixtures.pendenteVencida(1L);
        when(autorizacaoRepository.findByIdAndCondominioId(1L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.of(vencida));
        salvarDevolveOProprioObjeto();

        assertThatThrownBy(() -> service.decidir(PortariaFixtures.moradora101(), 1L,
                new DecisaoAutorizacaoRequest(DecisaoVisita.AUTORIZADO, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("prazo");

        verify(autorizacaoRepository).save(vencida);
        assertThat(vencida.getStatus()).isEqualTo(StatusAutorizacaoVisita.EXPIRADA);
        assertThat(vencida.getRespondidoEm()).isNull();
        verifyNoInteractions(notificacaoDeDecisaoDeVisita);
    }

    // ---------- reenviar ----------

    @Test
    @DisplayName("reenviar: renova o prazo e notifica a unidade de novo")
    void reenviarPendente() {
        AutorizacaoVisita autorizacao = PortariaFixtures.pendente(1L);
        Instant prazoAntigo = autorizacao.getExpiraEm().minusSeconds(600);
        autorizacao.setExpiraEm(prazoAntigo);
        Usuario ana = usuario(1L, "Ana Souza");
        when(autorizacaoRepository.findByIdAndCondominioId(1L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.of(autorizacao));
        when(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(PortariaFixtures.APTO_101)).thenReturn(List.of(ana));
        salvarDevolveOProprioObjeto();

        service.reenviar(PortariaFixtures.porteira(), 1L);

        assertThat(autorizacao.getExpiraEm()).isAfter(prazoAntigo);
        assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacaoVisita.PENDENTE);
        verify(notificacaoDeVisitanteAguardando).enviar(ana, autorizacao);
    }

    @Test
    @DisplayName("reenviar: autorizacao ja autorizada -> 409, sem notificacao")
    void reenviarNaoPendente() {
        when(autorizacaoRepository.findByIdAndCondominioId(1L, PortariaFixtures.CONDOMINIO))
                .thenReturn(Optional.of(PortariaFixtures.autorizada(1L)));

        assertThatThrownBy(() -> service.reenviar(PortariaFixtures.porteira(), 1L))
                .isInstanceOf(RegraDeNegocioException.class);
        verifyNoInteractions(notificacaoDeVisitanteAguardando);
    }

    @Test
    @DisplayName("reenviar: pendente vencida -> 409 e gravada como EXPIRADA")
    void reenviarExpirada() {
        AutorizacaoVisita vencida = PortariaFixtures.pendenteVencida(1L);
        when(autorizacaoRepository.findByIdAndCondominioId(1L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.of(vencida));
        salvarDevolveOProprioObjeto();

        assertThatThrownBy(() -> service.reenviar(PortariaFixtures.porteira(), 1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("expirou");
        assertThat(vencida.getStatus()).isEqualTo(StatusAutorizacaoVisita.EXPIRADA);
        verifyNoInteractions(notificacaoDeVisitanteAguardando);
    }

    // ---------- dashboard ----------

    @Test
    @DisplayName("dashboard: pendentes da unidade e do condominio respeitam o limite")
    void pendentesParaODashboard() {
        when(autorizacaoRepository.findByUnidadeIdAndStatusAndExpiraEmAfter(
                eq(PortariaFixtures.APTO_101), eq(StatusAutorizacaoVisita.PENDENTE), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(PortariaFixtures.pendente(1L))));
        when(autorizacaoRepository.findByCondominioIdAndStatusAndExpiraEmAfter(
                eq(PortariaFixtures.CONDOMINIO), eq(StatusAutorizacaoVisita.PENDENTE), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(PortariaFixtures.pendente(1L), PortariaFixtures.pendente(2L))));

        assertThat(service.pendentesDaUnidade(PortariaFixtures.APTO_101, 5)).hasSize(1);
        assertThat(service.pendentesDoCondominio(PortariaFixtures.CONDOMINIO, 5))
                .extracting(AutorizacaoVisitaResponse::id).containsExactly(1L, 2L);
        assertThat(service.pendentesDoCondominio(PortariaFixtures.CONDOMINIO, 0)).isEmpty();

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(autorizacaoRepository).findByUnidadeIdAndStatusAndExpiraEmAfter(
                eq(PortariaFixtures.APTO_101), eq(StatusAutorizacaoVisita.PENDENTE), any(Instant.class), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(5);
        assertThat(pagina.getValue().getSort().getOrderFor("solicitadoEm").isDescending()).isTrue();
    }
}
