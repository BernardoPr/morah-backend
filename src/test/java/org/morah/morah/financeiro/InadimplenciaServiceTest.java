package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.morah.morah.financeiro.DadosDeTeste.cobranca;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.dto.InadimplenciaResumoResponse;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.financeiro.servico.CalculadoraDeEncargos;
import org.morah.morah.financeiro.servico.InadimplenciaService;
import org.morah.morah.unidade.repositorio.UnidadeRepository;

@ExtendWith(MockitoExtension.class)
class InadimplenciaServiceTest {

    @Mock
    private CobrancaRepository cobrancaRepository;
    @Mock
    private UnidadeRepository unidadeRepository;

    private InadimplenciaService service;

    @BeforeEach
    void montar() {
        service = new InadimplenciaService(cobrancaRepository, unidadeRepository,
                new CalculadoraDeEncargos(DadosDeTeste.propriedades()));
    }

    @Test
    @DisplayName("unidade com varios boletos atrasados conta uma vez; o valor em aberto inclui encargos")
    void resumo() {
        LocalDate hoje = Datas.hoje();
        given(unidadeRepository.countByCondominioId(1L)).willReturn(4L);
        given(cobrancaRepository.findByCondominioIdAndStatusAndVencimentoBefore(eq(1L), eq(StatusCobranca.PENDENTE), any()))
                .willReturn(List.of(
                        cobranca(1L, 101L, "500.00", hoje.minusDays(15)),  // 500 + 10,00 + 2,50 = 512,50
                        cobranca(2L, 101L, "460.00", hoje.minusDays(1)),   // 460 + 9,20 + 0,15 = 469,35
                        cobranca(3L, 201L, "540.00", hoje.minusDays(15)))); // 540 + 10,80 + 2,70 = 553,50

        InadimplenciaResumoResponse resumo = service.resumo(1L);

        assertThat(resumo.totalUnidades()).isEqualTo(4);
        assertThat(resumo.unidadesInadimplentes()).isEqualTo(2);
        assertThat(resumo.percentualInadimplencia()).isEqualByComparingTo("50.00");
        assertThat(resumo.valorTotalEmAberto()).isEqualByComparingTo("1535.35");
    }

    @Test
    @DisplayName("condominio sem unidades: percentual zero (sem divisao por zero)")
    void semUnidades() {
        given(unidadeRepository.countByCondominioId(9L)).willReturn(0L);
        given(cobrancaRepository.findByCondominioIdAndStatusAndVencimentoBefore(eq(9L), eq(StatusCobranca.PENDENTE), any()))
                .willReturn(List.of());

        InadimplenciaResumoResponse resumo = service.resumo(9L);

        assertThat(resumo.percentualInadimplencia()).isEqualByComparingTo("0");
        assertThat(resumo.valorTotalEmAberto()).isEqualByComparingTo("0");
    }
}
