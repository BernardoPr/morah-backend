package org.morah.morah.aviso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.aviso.dto.AvisoCreateRequest;
import org.morah.morah.aviso.modelo.Aviso;
import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.morah.morah.aviso.modelo.PublicoAlvo;
import org.morah.morah.aviso.repositorio.AvisoRepository;
import org.morah.morah.aviso.servico.AvisoService;
import org.morah.morah.comum.erro.DadosInvalidosException;
import org.morah.morah.comum.erro.RecursoExpiradoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.notificacao.template.NotificacaoDeAvisoPublicado;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Regras do mural: condominio, publico alvo, vigencia, leitura e quem e notificado. */
class AvisoServiceTest {

    private static final UsuarioAutenticado SINDICO = new UsuarioAutenticado(
            2L, "Carlos Lima", "22222222222", Perfil.SINDICO, 1L, "Residencial Morah", null, "jti-1");
    private static final UsuarioAutenticado ANA_101 = new UsuarioAutenticado(
            1L, "Ana Souza", "11111111111", Perfil.MORADOR, 1L, "Residencial Morah", 101L, "jti-2");

    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final UnidadeRepository unidadeRepository = mock(UnidadeRepository.class);
    private final MongoOperations mongo = mock(MongoOperations.class);
    private final NotificacaoDeAvisoPublicado notificacao = mock(NotificacaoDeAvisoPublicado.class);

    private final AvisoService servico = new AvisoService(
            avisoRepository, usuarioRepository, unidadeRepository, mongo, notificacao);

    @AfterEach
    void limparLogin() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("abrir o aviso marca como lido com $addToSet")
    void detalharMarcaComoLido() {
        logar(ANA_101);
        given(avisoRepository.findById(10L)).willReturn(Optional.of(aviso(10L, 1L, paraCondominio(), null)));

        var resposta = servico.detalhar(10L, ANA_101);

        assertThat(resposta.lidoPeloUsuario()).isTrue();
        verify(mongo).updateFirst(any(Query.class), any(Update.class), eq(Aviso.class));
    }

    @Test
    @DisplayName("aviso fora de vigencia responde 410")
    void avisoExpirado() {
        logar(ANA_101);
        given(avisoRepository.findById(10L)).willReturn(Optional.of(
                aviso(10L, 1L, paraCondominio(), Instant.now().minusSeconds(60))));

        assertThatThrownBy(() -> servico.detalhar(10L, ANA_101)).isInstanceOf(RecursoExpiradoException.class);
    }

    @Test
    @DisplayName("aviso de outro condominio nao e encontrado")
    void outroCondominio() {
        logar(ANA_101);
        given(avisoRepository.findById(10L)).willReturn(Optional.of(aviso(10L, 2L, paraCondominio(), null)));

        assertThatThrownBy(() -> servico.detalhar(10L, ANA_101)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("morador nao ve aviso direcionado a outra unidade; o sindico ve")
    void publicoAlvoDeOutraUnidade() {
        given(avisoRepository.findById(10L)).willReturn(Optional.of(
                aviso(10L, 1L, new PublicoAlvo(PublicoAlvo.Tipo.UNIDADE, null, 202L), null)));

        logar(ANA_101);
        assertThatThrownBy(() -> servico.detalhar(10L, ANA_101)).isInstanceOf(RecursoNaoEncontradoException.class);

        logar(SINDICO);
        assertThat(servico.detalhar(10L, SINDICO).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("morador ve o aviso do proprio bloco")
    void avisoDoBloco() {
        logar(ANA_101);
        given(unidadeRepository.findById(101L)).willReturn(Optional.of(unidade(101L, 1L)));
        given(avisoRepository.findById(10L)).willReturn(Optional.of(
                aviso(10L, 1L, new PublicoAlvo(PublicoAlvo.Tipo.BLOCO, 1L, null), null)));

        assertThat(servico.detalhar(10L, ANA_101).id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("aviso para um bloco notifica cada morador do bloco uma vez, menos o autor")
    void notificaOPublicoDoBloco() {
        logar(SINDICO);
        given(unidadeRepository.findByCondominioIdAndBlocoId(1L, 1L))
                .willReturn(List.of(unidade(101L, 1L), unidade(102L, 1L)));

        Usuario ana = usuario(1L);
        Usuario bruno = usuario(4L);
        Usuario autor = usuario(2L);
        // Bruno aparece nas duas unidades: deve receber uma notificacao so.
        given(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(101L)).willReturn(List.of(ana, bruno));
        given(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(102L)).willReturn(List.of(bruno, autor));
        given(avisoRepository.save(any(Aviso.class))).willAnswer(invocacao -> {
            Aviso aviso = invocacao.getArgument(0);
            aviso.setId(10L);
            return aviso;
        });

        servico.criar(new AvisoCreateRequest("Obra", "Pintura do bloco A", PrioridadeAviso.NORMAL,
                new PublicoAlvo(PublicoAlvo.Tipo.BLOCO, 1L, null), null));

        verify(notificacao).enviar(eq(ana), any(Aviso.class));
        verify(notificacao).enviar(eq(bruno), any(Aviso.class));
        verify(notificacao, never()).enviar(eq(autor), any(Aviso.class));
    }

    @Test
    @DisplayName("publico alvo 'bloco' sem blocoId vira 400")
    void blocoSemId() {
        logar(SINDICO);

        assertThatThrownBy(() -> servico.criar(new AvisoCreateRequest("Obra", "Texto", null,
                new PublicoAlvo(PublicoAlvo.Tipo.BLOCO, null, null), null)))
                .isInstanceOf(DadosInvalidosException.class);
    }

    // ---------- apoio ----------

    private void logar(UsuarioAutenticado usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private PublicoAlvo paraCondominio() {
        return new PublicoAlvo(PublicoAlvo.Tipo.CONDOMINIO, null, null);
    }

    private Aviso aviso(Long id, Long condominioId, PublicoAlvo alvo, Instant expiraEm) {
        Aviso aviso = new Aviso();
        aviso.setId(id);
        aviso.setCondominioId(condominioId);
        aviso.setTitulo("Aviso");
        aviso.setConteudo("Conteudo");
        aviso.setPublicoAlvo(alvo);
        aviso.setAutorId(2L);
        aviso.setPublicadoEm(Instant.now().minusSeconds(3600));
        aviso.setExpiraEm(expiraEm);
        return aviso;
    }

    private Unidade unidade(Long id, Long blocoId) {
        Unidade unidade = new Unidade();
        unidade.setId(id);
        unidade.setCondominioId(1L);
        unidade.setBlocoId(blocoId);
        return unidade;
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario " + id);
        return usuario;
    }
}
