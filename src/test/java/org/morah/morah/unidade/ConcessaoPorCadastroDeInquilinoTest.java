package org.morah.morah.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.notificacao.template.NotificacaoDeConviteDeInquilino;
import org.morah.morah.notificacao.template.NotificacaoDeConviteDeInquilino.Convite;
import org.morah.morah.unidade.dto.InquilinoCreateRequest;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.unidade.servico.GeradorDeSenhaTemporaria;
import org.morah.morah.unidade.template.ConcessaoPorCadastroDeInquilino;
import org.morah.morah.unidade.template.DestinoDoAcesso;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Template Method {@code ConcessaoDeAcessoTemplate} no fluxo de cadastro de inquilino. */
@ExtendWith(MockitoExtension.class)
class ConcessaoPorCadastroDeInquilinoTest {

    private static final DestinoDoAcesso APTO_101 =
            new DestinoDoAcesso(DadosDeTeste.unidade(101L, "Apto 101"), DadosDeTeste.NOME_DO_CONDOMINIO);

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIM = LocalDate.of(2027, 10, 31);

    @Mock
    private VinculoUnidadeRepository vinculoUnidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder codificadorDeSenha;

    @Mock
    private GeradorDeSenhaTemporaria geradorDeSenha;

    @Mock
    private NotificacaoDeConviteDeInquilino notificacaoDeConvite;

    private ConcessaoPorCadastroDeInquilino concessao;

    @BeforeEach
    void montar() {
        concessao = new ConcessaoPorCadastroDeInquilino(vinculoUnidadeRepository, usuarioRepository,
                codificadorDeSenha, geradorDeSenha, notificacaoDeConvite);
    }

    private static InquilinoCreateRequest requisicao(String email) {
        return new InquilinoCreateRequest("Paula Dias", "555.555.555-55", email, "51988887777", INICIO, FIM);
    }

    private void vinculoGravadoRecebeId() {
        when(vinculoUnidadeRepository.save(any(VinculoUnidade.class))).thenAnswer(chamada -> {
            VinculoUnidade vinculo = chamada.getArgument(0);
            vinculo.setId(77L);
            return vinculo;
        });
    }

    @Test
    @DisplayName("inquilino sem cadastro: cria o usuario com senha temporaria, libera o app e envia o convite")
    void inquilinoNovo() {
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail("paula@exemplo.com")).thenReturn(false);
        when(geradorDeSenha.gerar()).thenReturn("SenhaTemp2345");
        when(codificadorDeSenha.encode("SenhaTemp2345")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> {
            Usuario usuario = chamada.getArgument(0);
            if (usuario.getId() == null) {
                usuario.setId(50L);
            }
            return usuario;
        });
        vinculoGravadoRecebeId();

        VinculoUnidade vinculo = concessao.conceder(APTO_101, requisicao("  Paula@Exemplo.com "));

        // vinculo da unidade: INQUILINO com o periodo do contrato, ligado ao usuario novo
        assertThat(vinculo.getTipoVinculo()).isEqualTo(TipoVinculo.INQUILINO);
        assertThat(vinculo.getPessoaCpf()).isEqualTo("55555555555");
        assertThat(vinculo.getPessoaId()).isEqualTo(50L);
        assertThat(vinculo.getInicio()).isEqualTo(INICIO);
        assertThat(vinculo.getFim()).isEqualTo(FIM);

        // usuario novo: so o hash e gravado, e-mail normalizado, perfil MORADOR do 101
        ArgumentCaptor<Usuario> usuarioGravado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(2)).save(usuarioGravado.capture());
        Usuario paula = usuarioGravado.getValue();
        assertThat(paula.getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(paula.getEmail()).isEqualTo("paula@exemplo.com");
        assertThat(paula.isAtivo()).isTrue();
        assertThat(paula.getVinculos()).containsExactly(
                new VinculoPerfil(Perfil.MORADOR, 1L, "Residencial Morah", 101L, "Apto 101"));

        // convite por e-mail com a senha temporaria
        ArgumentCaptor<Convite> convite = ArgumentCaptor.forClass(Convite.class);
        verify(notificacaoDeConvite).enviar(eq(paula), convite.capture());
        assertThat(convite.getValue().senhaTemporaria()).isEqualTo("SenhaTemp2345");
        assertThat(convite.getValue().vinculoId()).isEqualTo(77L);
        assertThat(convite.getValue().identificacaoDaUnidade()).isEqualTo("Apto 101");
    }

    @Test
    @DisplayName("inquilino ja cadastrado (e desativado): reativa, acrescenta o perfil e convida sem senha nova")
    void inquilinoExistente() {
        Usuario paula = DadosDeTeste.usuario(50L, "Paula Dias", "55555555555", DadosDeTeste.moradorDa(202L));
        paula.setAtivo(false);

        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.of(paula));
        vinculoGravadoRecebeId();

        concessao.conceder(APTO_101, requisicao(null));

        assertThat(paula.isAtivo()).isTrue();
        assertThat(paula.getVinculos()).extracting(VinculoPerfil::getUnidadeId).containsExactly(202L, 101L);
        verify(usuarioRepository).save(paula);
        verify(geradorDeSenha, never()).gerar();
        verify(codificadorDeSenha, never()).encode(anyString());

        ArgumentCaptor<Convite> convite = ArgumentCaptor.forClass(Convite.class);
        verify(notificacaoDeConvite).enviar(eq(paula), convite.capture());
        assertThat(convite.getValue().senhaTemporaria()).isNull();
    }

    @Test
    @DisplayName("inquilino sem cadastro e sem e-mail -> 409 (nao ha para onde mandar o convite)")
    void inquilinoNovoSemEmail() {
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> concessao.conceder(APTO_101, requisicao("  ")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("e-mail");
        verify(usuarioRepository, never()).save(any());
        verify(vinculoUnidadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("inquilino sem cadastro com e-mail de outro usuario -> 409")
    void emailJaUsado() {
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findByCpf("55555555555")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail("ana@morah.com.br")).thenReturn(true);

        assertThatThrownBy(() -> concessao.conceder(APTO_101, requisicao("ana@morah.com.br")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("e-mail");
        verify(vinculoUnidadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("inquilino ativo com o mesmo CPF na unidade -> 409")
    void inquilinoJaAtivo() {
        VinculoUnidade atual = DadosDeTeste.vinculo(3L, 101L, "Paula Dias", "55555555555",
                TipoVinculo.INQUILINO, false, LocalDate.of(2025, 1, 1), LocalDate.of(2099, 1, 1));
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of(atual));

        assertThatThrownBy(() -> concessao.conceder(APTO_101, requisicao("paula@exemplo.com")))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(usuarioRepository, never()).findByCpf(any());
        verify(vinculoUnidadeRepository, never()).save(any());
    }
}
