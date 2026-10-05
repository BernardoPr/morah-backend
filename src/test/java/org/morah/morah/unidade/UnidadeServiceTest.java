package org.morah.morah.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.unidade.dto.UnidadeResponse;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.unidade.servico.UnidadeService;

@ExtendWith(MockitoExtension.class)
class UnidadeServiceTest {

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private UnidadeService unidadeService;

    @Test
    @DisplayName("GET /unidades/minha devolve a unidade do token, filtrando pelo condominio")
    void buscaAMinhaUnidade() {
        when(unidadeRepository.findByIdAndCondominioId(101L, 1L))
                .thenReturn(Optional.of(DadosDeTeste.unidade(101L, "Apto 101")));

        UnidadeResponse resposta = unidadeService.buscarMinha(DadosDeTeste.ana());

        assertThat(resposta.id()).isEqualTo(101L);
        assertThat(resposta.identificacao()).isEqualTo("Apto 101");
        assertThat(resposta.blocoNome()).isEqualTo("Bloco A");
    }

    @Test
    @DisplayName("token sem unidade -> 404, sem consultar o banco")
    void tokenSemUnidade() {
        var semUnidade = DadosDeTeste.logado(9L, "Fulano", Perfil.MORADOR, null);

        assertThatThrownBy(() -> unidadeService.buscarMinha(semUnidade))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(unidadeRepository, never()).findByIdAndCondominioId(any(), any());
    }

    @Test
    @DisplayName("sindico consultando unidade de outro condominio -> 404")
    void unidadeDeOutroCondominio() {
        when(unidadeRepository.findByIdAndCondominioId(301L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> unidadeService.buscarDoCondominio(DadosDeTeste.carlosSindico(), 301L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("301");
    }

    @Test
    @DisplayName("buscarResposta (dashboard) devolve null em vez de lancar erro")
    void buscarRespostaNuncaQuebraODashboard() {
        when(unidadeRepository.findById(999L)).thenReturn(Optional.empty());
        when(unidadeRepository.findById(202L)).thenReturn(Optional.of(DadosDeTeste.unidade(202L, "Apto 202")));

        assertThat(unidadeService.buscarResposta(null)).isNull();
        assertThat(unidadeService.buscarResposta(999L)).isNull();
        assertThat(unidadeService.buscarResposta(202L).identificacao()).isEqualTo("Apto 202");
    }
}
