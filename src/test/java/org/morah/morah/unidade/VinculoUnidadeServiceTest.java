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
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.unidade.dto.VinculoUnidadeResponse;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.unidade.servico.UnidadeService;
import org.morah.morah.unidade.servico.VinculoUnidadeService;
import org.morah.morah.unidade.template.ConcessaoPorCadastroDeInquilino;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class VinculoUnidadeServiceTest {

    @Mock
    private VinculoUnidadeRepository vinculoUnidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private ConcessaoPorCadastroDeInquilino concessaoPorCadastroDeInquilino;

    private VinculoUnidadeService service;

    @BeforeEach
    void montar() {
        service = new VinculoUnidadeService(vinculoUnidadeRepository, usuarioRepository,
                new UnidadeService(unidadeRepository), concessaoPorCadastroDeInquilino);
    }

    private void unidade101Existe() {
        when(unidadeRepository.findByIdAndCondominioId(101L, 1L))
                .thenReturn(Optional.of(DadosDeTeste.unidade(101L, "Apto 101")));
    }

    /** Ana, inquilina do 101 ate 2027, ligada ao usuario 1. */
    private VinculoUnidade contratoDaAna() {
        VinculoUnidade vinculo = DadosDeTeste.vinculo(3L, 101L, "Ana Souza", "11111111111",
                TipoVinculo.INQUILINO, false, LocalDate.of(2025, 3, 1), LocalDate.of(2099, 2, 28));
        vinculo.setPessoaId(1L);
        return vinculo;
    }

    // ---------- GET /unidades/minha/vinculos ----------

    @Test
    @DisplayName("lista so os vinculos vigentes, com o principal primeiro")
    void listaVinculosVigentes() {
        unidade101Existe();
        VinculoUnidade vencido = DadosDeTeste.vinculo(9L, 101L, "Antigo Inquilino", "99999999999",
                TipoVinculo.INQUILINO, false, LocalDate.of(2020, 1, 1), Datas.hoje().minusDays(1));
        VinculoUnidade bruno = DadosDeTeste.vinculo(1L, 101L, "Bruno Alves", "44444444444",
                TipoVinculo.PROPRIETARIO, true, LocalDate.of(2020, 1, 1), null);
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L))
                .thenReturn(List.of(contratoDaAna(), vencido, bruno));

        List<VinculoUnidadeResponse> vinculos = service.listarDaMinhaUnidade(DadosDeTeste.ana());

        assertThat(vinculos).extracting(v -> v.pessoa().nome()).containsExactly("Bruno Alves", "Ana Souza");
        assertThat(vinculos.get(1).pessoa().id()).isEqualTo(1L);
    }

    // ---------- DELETE /unidades/minha/inquilinos/{vinculoId} ----------

    @Test
    @DisplayName("encerrar: vinculo inativo com fim hoje, perfil MORADOR removido e usuario sem vinculos desativado")
    void encerrarRetiraOAcesso() {
        unidade101Existe();
        VinculoUnidade contrato = contratoDaAna();
        Usuario ana = DadosDeTeste.usuario(1L, "Ana Souza", "11111111111", DadosDeTeste.moradorDa(101L));
        when(vinculoUnidadeRepository.findByIdAndCondominioId(3L, 1L)).thenReturn(Optional.of(contrato));
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));

        service.encerrarInquilino(DadosDeTeste.bruno(), 3L);

        assertThat(contrato.isAtivo()).isFalse();
        assertThat(contrato.getFim()).isEqualTo(Datas.hoje());
        verify(vinculoUnidadeRepository).save(contrato);

        assertThat(ana.getVinculos()).isEmpty();
        assertThat(ana.isAtivo()).isFalse();
        verify(usuarioRepository).save(ana);
    }

    @Test
    @DisplayName("encerrar: quem mora em outra unidade continua ativo, so perde o 101")
    void encerrarMantemOutrosVinculos() {
        unidade101Existe();
        VinculoUnidade contrato = contratoDaAna();
        Usuario ana = DadosDeTeste.usuario(1L, "Ana Souza", "11111111111",
                DadosDeTeste.moradorDa(101L), DadosDeTeste.moradorDa(202L));
        when(vinculoUnidadeRepository.findByIdAndCondominioId(3L, 1L)).thenReturn(Optional.of(contrato));
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L)).thenReturn(List.of());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(ana));

        service.encerrarInquilino(DadosDeTeste.bruno(), 3L);

        assertThat(ana.getVinculos()).extracting(VinculoPerfil::getUnidadeId).containsExactly(202L);
        assertThat(ana.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("encerrar: se a pessoa tem outro vinculo vigente na mesma unidade, o acesso ao app fica")
    void encerrarMasContinuaNaUnidade() {
        unidade101Existe();
        VinculoUnidade contrato = contratoDaAna();
        VinculoUnidade tambemDependente = DadosDeTeste.vinculo(4L, 101L, "Ana Souza", "11111111111",
                TipoVinculo.DEPENDENTE, false, LocalDate.of(2025, 3, 1), null);
        tambemDependente.setPessoaId(1L);
        when(vinculoUnidadeRepository.findByIdAndCondominioId(3L, 1L)).thenReturn(Optional.of(contrato));
        when(vinculoUnidadeRepository.findByUnidadeIdAndCondominioIdAndAtivoTrue(101L, 1L))
                .thenReturn(List.of(tambemDependente));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(
                DadosDeTeste.usuario(1L, "Ana Souza", "11111111111", DadosDeTeste.moradorDa(101L))));

        service.encerrarInquilino(DadosDeTeste.bruno(), 3L);

        assertThat(contrato.isAtivo()).isFalse();
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("encerrar vinculo de outra unidade do condominio -> 403")
    void encerrarDeOutraUnidade() {
        unidade101Existe();
        VinculoUnidade doOutroApto = DadosDeTeste.vinculo(8L, 202L, "Fulano", "88888888888",
                TipoVinculo.INQUILINO, false, LocalDate.of(2025, 1, 1), null);
        when(vinculoUnidadeRepository.findByIdAndCondominioId(8L, 1L)).thenReturn(Optional.of(doOutroApto));

        assertThatThrownBy(() -> service.encerrarInquilino(DadosDeTeste.bruno(), 8L))
                .isInstanceOf(AcessoNegadoException.class);
        verify(vinculoUnidadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("encerrar vinculo de outro condominio (ou inexistente) -> 404")
    void encerrarDeOutroCondominio() {
        unidade101Existe();
        when(vinculoUnidadeRepository.findByIdAndCondominioId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.encerrarInquilino(DadosDeTeste.bruno(), 99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("encerrar vinculo que nao e de inquilino, ou ja encerrado -> 409")
    void encerrarVinculoInvalido() {
        unidade101Existe();
        VinculoUnidade proprietario = DadosDeTeste.vinculo(1L, 101L, "Bruno Alves", "44444444444",
                TipoVinculo.PROPRIETARIO, true, LocalDate.of(2020, 1, 1), null);
        VinculoUnidade jaEncerrado = contratoDaAna();
        jaEncerrado.setAtivo(false);
        when(vinculoUnidadeRepository.findByIdAndCondominioId(1L, 1L)).thenReturn(Optional.of(proprietario));
        when(vinculoUnidadeRepository.findByIdAndCondominioId(3L, 1L)).thenReturn(Optional.of(jaEncerrado));

        assertThatThrownBy(() -> service.encerrarInquilino(DadosDeTeste.bruno(), 1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("inquilino");
        assertThatThrownBy(() -> service.encerrarInquilino(DadosDeTeste.bruno(), 3L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("encerrado");
        verify(vinculoUnidadeRepository, never()).save(any());
    }
}
