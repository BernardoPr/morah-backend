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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.template.NotificacaoDeDecisaoDeVinculo;
import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.unidade.template.ConcessaoPorSolicitacaoAprovada;
import org.morah.morah.unidade.template.DestinoDoAcesso;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;

/**
 * Template Method {@code ConcessaoDeAcessoTemplate} no fluxo de aprovacao: o roteiro comum
 * (conferir duplicidade -> gravar -> liberar app -> notificar) com os passos da solicitacao.
 */
@ExtendWith(MockitoExtension.class)
class ConcessaoPorSolicitacaoAprovadaTest {

    private static final DestinoDoAcesso APTO_202 =
            new DestinoDoAcesso(DadosDeTeste.unidade(202L, "Apto 202"), DadosDeTeste.NOME_DO_CONDOMINIO);

    @Mock
    private VinculoUnidadeRepository vinculoUnidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacaoDeDecisaoDeVinculo notificacaoDeDecisao;

    private ConcessaoPorSolicitacaoAprovada concessao;

    @BeforeEach
    void montar() {
        concessao = new ConcessaoPorSolicitacaoAprovada(vinculoUnidadeRepository, usuarioRepository, notificacaoDeDecisao);
    }

    private void gravacaoDevolveOMesmoObjeto() {
        when(vinculoUnidadeRepository.save(any(VinculoUnidade.class))).thenAnswer(chamada -> {
            VinculoUnidade vinculo = chamada.getArgument(0);
            vinculo.setId(30L);
            return vinculo;
        });
    }

    @Test
    @DisplayName("pessoa com usuario: cria o vinculo da unidade e libera o perfil MORADOR no app")
    void aprovacaoComUsuarioExistente() {
        SolicitacaoVinculo solicitacao = DadosDeTeste.solicitacaoPendente(10L, 202L, "Marina Lima", "55555555555");
        Usuario marina = DadosDeTeste.usuario(5L, "Marina Lima", "55555555555"); // sem vinculos ainda
        Usuario carlos = DadosDeTeste.usuario(2L, "Carlos Lima", "22222222222");

        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(202L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.of(marina));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(carlos));
        gravacaoDevolveOMesmoObjeto();

        VinculoUnidade vinculo = concessao.conceder(APTO_202, solicitacao);

        assertThat(vinculo.getTipoVinculo()).isEqualTo(TipoVinculo.DEPENDENTE);
        assertThat(vinculo.getPessoaId()).isEqualTo(5L);
        assertThat(vinculo.getPessoaTelefone()).isEqualTo("51999990000"); // completado pelo cadastro
        assertThat(vinculo.getInicio()).isEqualTo(Datas.hoje());
        assertThat(vinculo.getFim()).isNull();
        assertThat(vinculo.isAtivo()).isTrue();

        assertThat(marina.getVinculos()).containsExactly(
                new VinculoPerfil(Perfil.MORADOR, 1L, "Residencial Morah", 202L, "Apto 202"));
        verify(usuarioRepository).save(marina);
        verify(notificacaoDeDecisao).enviar(carlos, solicitacao);
    }

    @Test
    @DisplayName("dependente sem CPF entra so no cadastro da unidade, sem usuario no app")
    void aprovacaoSemCpf() {
        SolicitacaoVinculo solicitacao = DadosDeTeste.solicitacaoPendente(10L, 202L, "Lucas Lima", null);
        gravacaoDevolveOMesmoObjeto();

        VinculoUnidade vinculo = concessao.conceder(APTO_202, solicitacao);

        assertThat(vinculo.getPessoaId()).isNull();
        assertThat(vinculo.getPessoaNome()).isEqualTo("Lucas Lima");
        verify(usuarioRepository, never()).findByCpf(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("quem ja tem acesso a unidade nao ganha um VinculoPerfil repetido")
    void naoDuplicaOVinculoDePerfil() {
        SolicitacaoVinculo solicitacao = DadosDeTeste.solicitacaoPendente(10L, 202L, "Marina Lima", "55555555555");
        Usuario marina = DadosDeTeste.usuario(5L, "Marina Lima", "55555555555", DadosDeTeste.moradorDa(202L));

        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(202L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.of(marina));
        gravacaoDevolveOMesmoObjeto();

        concessao.conceder(APTO_202, solicitacao);

        assertThat(marina.getVinculos()).hasSize(1);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("usuario desativado (ex.: ex-inquilino) e reativado ao ganhar o vinculo")
    void reativaUsuarioDesativado() {
        SolicitacaoVinculo solicitacao = DadosDeTeste.solicitacaoPendente(10L, 202L, "Marina Lima", "55555555555");
        Usuario marina = DadosDeTeste.usuario(5L, "Marina Lima", "55555555555");
        marina.setAtivo(false);

        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(202L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.of(marina));
        gravacaoDevolveOMesmoObjeto();

        concessao.conceder(APTO_202, solicitacao);

        assertThat(marina.isAtivo()).isTrue();
        verify(usuarioRepository).save(marina);
    }

    @Test
    @DisplayName("mesma pessoa com vinculo vigente do mesmo tipo -> 409 e nada e gravado")
    void pessoaJaVinculada() {
        SolicitacaoVinculo solicitacao = DadosDeTeste.solicitacaoPendente(10L, 202L, "Marina Lima", "55555555555");
        VinculoUnidade existente = DadosDeTeste.vinculo(7L, 202L, "Marina Lima", "55555555555",
                TipoVinculo.DEPENDENTE, false, LocalDate.of(2024, 1, 1), null);

        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(202L, 1L)).thenReturn(List.of(existente));

        assertThatThrownBy(() -> concessao.conceder(APTO_202, solicitacao))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja possui um vinculo ativo");
        verify(vinculoUnidadeRepository, never()).save(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("vinculo antigo ja vencido (fim < hoje) nao impede um novo")
    void vinculoVencidoNaoBloqueia() {
        SolicitacaoVinculo solicitacao = DadosDeTeste.solicitacaoPendente(10L, 202L, "Marina Lima", "55555555555");
        VinculoUnidade vencido = DadosDeTeste.vinculo(7L, 202L, "Marina Lima", "55555555555",
                TipoVinculo.DEPENDENTE, false, LocalDate.of(2020, 1, 1), Datas.hoje().minusDays(1));

        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(202L, 1L)).thenReturn(List.of(vencido));
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.empty());
        gravacaoDevolveOMesmoObjeto();

        assertThat(concessao.conceder(APTO_202, solicitacao).getId()).isEqualTo(30L);
    }
}
