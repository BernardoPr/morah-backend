package org.morah.morah.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.template.NotificacaoDeDecisaoDeVinculo;
import org.morah.morah.notificacao.template.NotificacaoDeSolicitacaoDeVinculo;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.dto.DecisaoSolicitacaoVinculoRequest;
import org.morah.morah.unidade.dto.SolicitacaoVinculoCreateRequest;
import org.morah.morah.unidade.dto.SolicitacaoVinculoResponse;
import org.morah.morah.unidade.modelo.DecisaoSolicitacao;
import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.StatusSolicitacaoVinculo;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.SolicitacaoVinculoRepository;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.unidade.servico.SolicitacaoVinculoService;
import org.morah.morah.unidade.servico.UnidadeService;
import org.morah.morah.unidade.template.ConcessaoPorSolicitacaoAprovada;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Regras das solicitacoes de vinculo. Usa o service, o {@link UnidadeService} e o Template Method
 * de aprovacao de verdade; so os repositorios e as notificacoes sao "de mentira".
 */
@ExtendWith(MockitoExtension.class)
class SolicitacaoVinculoServiceTest {

    @Mock
    private SolicitacaoVinculoRepository solicitacaoVinculoRepository;

    @Mock
    private VinculoUnidadeRepository vinculoUnidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private NotificacaoDeSolicitacaoDeVinculo notificacaoDeSolicitacao;

    @Mock
    private NotificacaoDeDecisaoDeVinculo notificacaoDeDecisao;

    private SolicitacaoVinculoService service;

    private final UsuarioAutenticado sindico = DadosDeTeste.carlosSindico();

    @BeforeEach
    void montar() {
        var concessao = new ConcessaoPorSolicitacaoAprovada(vinculoUnidadeRepository, usuarioRepository, notificacaoDeDecisao);
        service = new SolicitacaoVinculoService(solicitacaoVinculoRepository, vinculoUnidadeRepository,
                usuarioRepository, new UnidadeService(unidadeRepository), concessao,
                notificacaoDeSolicitacao, notificacaoDeDecisao);
    }

    @AfterEach
    void limparLogin() {
        SecurityContextHolder.clearContext();
    }

    /** A criacao usa o ServicoCrudTemplate, cujos hooks leem o usuario do contexto de seguranca. */
    private void logarComo(UsuarioAutenticado usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private void unidadeExiste(Long id) {
        when(unidadeRepository.findByIdAndCondominioId(id, 1L))
                .thenReturn(Optional.of(DadosDeTeste.unidade(id, "Apto " + id)));
    }

    private void pendentesDaUnidade(Long unidadeId, SolicitacaoVinculo... pendentes) {
        when(solicitacaoVinculoRepository.findByUnidadeIdAndCondominioIdAndStatusOrderBySolicitadoEmAsc(
                unidadeId, 1L, StatusSolicitacaoVinculo.PENDENTE)).thenReturn(List.of(pendentes));
    }

    // ---------- POST /unidades/minha/vinculos/solicitacoes ----------

    @Test
    @DisplayName("solicitar: grava PENDENTE com os dados do token, CPF so com digitos, e avisa os sindicos")
    void solicitarVinculo() {
        logarComo(DadosDeTeste.ana());
        unidadeExiste(101L);
        pendentesDaUnidade(101L);
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of());
        when(solicitacaoVinculoRepository.save(any(SolicitacaoVinculo.class))).thenAnswer(chamada -> {
            SolicitacaoVinculo solicitacao = chamada.getArgument(0);
            solicitacao.setId(10L);
            return solicitacao;
        });
        Usuario carlos = DadosDeTeste.usuario(2L, "Carlos Lima", "22222222222");
        when(usuarioRepository.listarPorPerfilNoCondominio(1L, Perfil.SINDICO)).thenReturn(List.of(carlos));

        SolicitacaoVinculoResponse resposta = service.criar(new SolicitacaoVinculoCreateRequest(
                TipoVinculo.DEPENDENTE, "  Pedro Souza ", "123.456.789-01", "filho"));

        assertThat(resposta.id()).isEqualTo(10L);
        assertThat(resposta.status()).isEqualTo(StatusSolicitacaoVinculo.PENDENTE);
        assertThat(resposta.unidadeId()).isEqualTo(101L);
        assertThat(resposta.nomeSolicitado()).isEqualTo("Pedro Souza");
        assertThat(resposta.solicitadoEm()).isNotNull();

        ArgumentCaptor<SolicitacaoVinculo> gravada = ArgumentCaptor.forClass(SolicitacaoVinculo.class);
        verify(solicitacaoVinculoRepository).save(gravada.capture());
        assertThat(gravada.getValue().getCpf()).isEqualTo("12345678901");
        assertThat(gravada.getValue().getCondominioId()).isEqualTo(1L);
        assertThat(gravada.getValue().getSolicitanteId()).isEqualTo(1L);
        assertThat(gravada.getValue().getUnidadeIdentificacao()).isEqualTo("Apto 101");

        verify(notificacaoDeSolicitacao).enviar(carlos, gravada.getValue());
    }

    @Test
    @DisplayName("solicitar: ja existe pendente com o mesmo CPF -> 409")
    void solicitacaoDuplicadaPorCpf() {
        logarComo(DadosDeTeste.ana());
        unidadeExiste(101L);
        pendentesDaUnidade(101L, DadosDeTeste.solicitacaoPendente(9L, 101L, "Pedro S.", "12345678901"));

        assertThatThrownBy(() -> service.criar(new SolicitacaoVinculoCreateRequest(
                TipoVinculo.DEPENDENTE, "Pedro Souza", "12345678901", null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("pendente");
        verify(solicitacaoVinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("solicitar sem CPF: ja existe pendente com o mesmo nome (sem acento/caixa) -> 409")
    void solicitacaoDuplicadaPorNome() {
        logarComo(DadosDeTeste.ana());
        unidadeExiste(101L);
        pendentesDaUnidade(101L, DadosDeTeste.solicitacaoPendente(9L, 101L, "Joao  da SILVA", null));

        assertThatThrownBy(() -> service.criar(new SolicitacaoVinculoCreateRequest(
                TipoVinculo.DEPENDENTE, "João da Silva", "", null)))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(solicitacaoVinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("solicitar: a pessoa ja tem vinculo vigente do mesmo tipo -> 409")
    void solicitarParaQuemJaEstaVinculado() {
        logarComo(DadosDeTeste.ana());
        unidadeExiste(101L);
        pendentesDaUnidade(101L);
        VinculoUnidade existente = DadosDeTeste.vinculo(3L, 101L, "Pedro Souza", "12345678901",
                TipoVinculo.DEPENDENTE, false, LocalDate.of(2024, 1, 1), null);
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.criar(new SolicitacaoVinculoCreateRequest(
                TipoVinculo.DEPENDENTE, "Pedro Souza", "12345678901", null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja possui um vinculo ativo");
    }

    // ---------- PATCH .../solicitacoes/{id} ----------

    @Test
    @DisplayName("decidir uma solicitacao ja decidida -> 409, nada e alterado")
    void decidirSolicitacaoJaDecidida() {
        unidadeExiste(202L);
        SolicitacaoVinculo rejeitada = DadosDeTeste.solicitacaoPendente(10L, 202L, "Lucas Lima", null);
        rejeitada.setStatus(StatusSolicitacaoVinculo.REJEITADA);
        when(solicitacaoVinculoRepository.findByIdAndUnidadeIdAndCondominioId(10L, 202L, 1L))
                .thenReturn(Optional.of(rejeitada));

        assertThatThrownBy(() -> service.decidir(sindico, 202L, 10L,
                new DecisaoSolicitacaoVinculoRequest(DecisaoSolicitacao.APROVADA, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("rejeitada");
        verify(solicitacaoVinculoRepository, never()).save(any());
        verify(vinculoUnidadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("aprovar: cria o vinculo da unidade, da o perfil MORADOR ao usuario do CPF e avisa quem pediu")
    void aprovarComUsuarioExistente() {
        unidadeExiste(202L);
        SolicitacaoVinculo solicitacao = DadosDeTeste.solicitacaoPendente(10L, 202L, "Marina Lima", "55555555555");
        when(solicitacaoVinculoRepository.findByIdAndUnidadeIdAndCondominioId(10L, 202L, 1L))
                .thenReturn(Optional.of(solicitacao));
        when(solicitacaoVinculoRepository.save(solicitacao)).thenReturn(solicitacao);
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(202L, 1L)).thenReturn(List.of());
        when(vinculoUnidadeRepository.save(any(VinculoUnidade.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Usuario marina = DadosDeTeste.usuario(5L, "Marina Lima", "55555555555");
        Usuario carlos = DadosDeTeste.usuario(2L, "Carlos Lima", "22222222222");
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.of(marina));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(carlos));

        SolicitacaoVinculoResponse resposta = service.decidir(sindico, 202L, 10L,
                new DecisaoSolicitacaoVinculoRequest(DecisaoSolicitacao.APROVADA, "ok"));

        assertThat(resposta.status()).isEqualTo(StatusSolicitacaoVinculo.APROVADA);
        assertThat(solicitacao.getDecididoPorId()).isEqualTo(2L);
        assertThat(solicitacao.getDecididoEm()).isNotNull();

        ArgumentCaptor<VinculoUnidade> vinculo = ArgumentCaptor.forClass(VinculoUnidade.class);
        verify(vinculoUnidadeRepository).save(vinculo.capture());
        assertThat(vinculo.getValue().getUnidadeId()).isEqualTo(202L);
        assertThat(vinculo.getValue().getPessoaId()).isEqualTo(5L);
        assertThat(vinculo.getValue().getTipoVinculo()).isEqualTo(TipoVinculo.DEPENDENTE);
        assertThat(vinculo.getValue().getInicio()).isEqualTo(Datas.hoje());

        assertThat(marina.getVinculos()).containsExactly(
                new VinculoPerfil(Perfil.MORADOR, 1L, "Residencial Morah", 202L, "Apto 202"));
        verify(usuarioRepository).save(marina);
        verify(notificacaoDeDecisao).enviar(carlos, solicitacao);
    }

    @Test
    @DisplayName("rejeitar: nao cria vinculo, guarda a justificativa e avisa quem pediu")
    void rejeitar() {
        unidadeExiste(202L);
        SolicitacaoVinculo solicitacao = DadosDeTeste.solicitacaoPendente(10L, 202L, "Lucas Lima", null);
        when(solicitacaoVinculoRepository.findByIdAndUnidadeIdAndCondominioId(10L, 202L, 1L))
                .thenReturn(Optional.of(solicitacao));
        when(solicitacaoVinculoRepository.save(solicitacao)).thenReturn(solicitacao);
        Usuario carlos = DadosDeTeste.usuario(2L, "Carlos Lima", "22222222222");
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(carlos));

        SolicitacaoVinculoResponse resposta = service.decidir(sindico, 202L, 10L,
                new DecisaoSolicitacaoVinculoRequest(DecisaoSolicitacao.REJEITADA, " falta documento "));

        assertThat(resposta.status()).isEqualTo(StatusSolicitacaoVinculo.REJEITADA);
        assertThat(solicitacao.getJustificativa()).isEqualTo("falta documento");
        verify(vinculoUnidadeRepository, never()).save(any());
        verify(notificacaoDeDecisao).enviar(carlos, solicitacao);
    }

    @Test
    @DisplayName("decidir em unidade de outro condominio -> 404, sem procurar a solicitacao")
    void decidirEmUnidadeDeOutroCondominio() {
        when(unidadeRepository.findByIdAndCondominioId(301L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decidir(sindico, 301L, 10L,
                new DecisaoSolicitacaoVinculoRequest(DecisaoSolicitacao.APROVADA, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(solicitacaoVinculoRepository, never()).findByIdAndUnidadeIdAndCondominioId(any(), any(), any());
    }

    @Test
    @DisplayName("solicitacao que nao e daquela unidade -> 404")
    void solicitacaoDeOutraUnidade() {
        unidadeExiste(101L);
        when(solicitacaoVinculoRepository.findByIdAndUnidadeIdAndCondominioId(10L, 101L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decidir(sindico, 101L, 10L,
                new DecisaoSolicitacaoVinculoRequest(DecisaoSolicitacao.REJEITADA, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- GET .../solicitacoes e dashboard ----------

    @Test
    @DisplayName("lista as pendentes da unidade; unidade de outro condominio -> 404")
    void listarPendentes() {
        unidadeExiste(202L);
        pendentesDaUnidade(202L, DadosDeTeste.solicitacaoPendente(10L, 202L, "Lucas Lima", null));
        when(unidadeRepository.findByIdAndCondominioId(301L, 1L)).thenReturn(Optional.empty());

        assertThat(service.listarPendentes(sindico, 202L))
                .extracting(SolicitacaoVinculoResponse::nomeSolicitado)
                .containsExactly("Lucas Lima");
        assertThatThrownBy(() -> service.listarPendentes(sindico, 301L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("contarPendentes (dashboard do sindico) nao quebra com condominio nulo")
    void contarPendentes() {
        when(solicitacaoVinculoRepository.countByCondominioIdAndStatus(1L, StatusSolicitacaoVinculo.PENDENTE))
                .thenReturn(3L);

        assertThat(service.contarPendentes(1L)).isEqualTo(3L);
        assertThat(service.contarPendentes(null)).isZero();
    }
}
