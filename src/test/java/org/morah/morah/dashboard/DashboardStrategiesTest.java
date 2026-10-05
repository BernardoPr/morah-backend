package org.morah.morah.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.morah.morah.aviso.servico.AvisoService;
import org.morah.morah.comum.dto.PaginaMetadata;
import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.dashboard.dto.DashboardMoradorResponse;
import org.morah.morah.dashboard.dto.DashboardPortariaResponse;
import org.morah.morah.dashboard.dto.DashboardSindicoResponse;
import org.morah.morah.dashboard.strategy.DashboardMoradorStrategy;
import org.morah.morah.dashboard.strategy.DashboardPortariaStrategy;
import org.morah.morah.dashboard.strategy.DashboardSindicoStrategy;
import org.morah.morah.encomenda.servico.EncomendaService;
import org.morah.morah.financeiro.dto.InadimplenciaResumoResponse;
import org.morah.morah.financeiro.servico.CobrancaService;
import org.morah.morah.financeiro.servico.InadimplenciaService;
import org.morah.morah.ocorrencia.servico.OcorrenciaService;
import org.morah.morah.portaria.servico.AutorizacaoVisitaService;
import org.morah.morah.reserva.servico.ReservaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.dto.UnidadeResponse;
import org.morah.morah.unidade.servico.SolicitacaoVinculoService;
import org.morah.morah.unidade.servico.UnidadeService;
import org.springframework.data.domain.Pageable;

/** Cada tela inicial junta os dados certos dos modulos, sempre filtrando pelo contexto do token. */
class DashboardStrategiesTest {

    private final AvisoService avisoService = mock(AvisoService.class);
    private final UnidadeService unidadeService = mock(UnidadeService.class);
    private final CobrancaService cobrancaService = mock(CobrancaService.class);
    private final EncomendaService encomendaService = mock(EncomendaService.class);
    private final AutorizacaoVisitaService autorizacaoVisitaService = mock(AutorizacaoVisitaService.class);
    private final ReservaService reservaService = mock(ReservaService.class);
    private final InadimplenciaService inadimplenciaService = mock(InadimplenciaService.class);
    private final SolicitacaoVinculoService solicitacaoVinculoService = mock(SolicitacaoVinculoService.class);
    private final OcorrenciaService ocorrenciaService = mock(OcorrenciaService.class);

    {
        given(avisoService.listarVisiveis(any(), any(), any(Pageable.class)))
                .willReturn(new PaginaResponse<>(List.of(), new PaginaMetadata(0, 5, 0, 0)));
    }

    @Test
    void moradorRecebeOsDadosDaPropriaUnidade() {
        var ana = usuario(Perfil.MORADOR, 101L);
        var unidade = new UnidadeResponse(101L, 1L, "Bloco A", "Apto 101", "apartamento",
                new BigDecimal("70"), new BigDecimal("0.23"), "ativa");
        given(unidadeService.buscarResposta(101L)).willReturn(unidade);

        var estrategia = new DashboardMoradorStrategy(avisoService, unidadeService, cobrancaService,
                encomendaService, autorizacaoVisitaService, reservaService);

        var resposta = (DashboardMoradorResponse) estrategia.montar(ana);

        assertThat(resposta.perfil()).isEqualTo("morador");
        assertThat(resposta.unidade()).isEqualTo(unidade);
        // Todas as listas pedem os dados da unidade do token, nunca de outra.
        verify(cobrancaService).proximasDaUnidade(eq(101L), any(Integer.class));
        verify(encomendaService).pendentesDaUnidade(eq(101L), any(Integer.class));
        verify(autorizacaoVisitaService).pendentesDaUnidade(eq(101L), any(Integer.class));
        verify(reservaService).proximasDaUnidade(eq(101L), any(Integer.class));
    }

    @Test
    void moradorSemUnidadeVeSoOsAvisos() {
        var estrategia = new DashboardMoradorStrategy(avisoService, unidadeService, cobrancaService,
                encomendaService, autorizacaoVisitaService, reservaService);

        var resposta = (DashboardMoradorResponse) estrategia.montar(usuario(Perfil.MORADOR, null));

        assertThat(resposta.unidade()).isNull();
        assertThat(resposta.proximosBoletos()).isEmpty();
        verifyNoInteractions(cobrancaService, encomendaService, autorizacaoVisitaService, reservaService);
    }

    @Test
    void sindicoRecebeOsIndicadoresDoCondominio() {
        var resumo = new InadimplenciaResumoResponse(4, 1, new BigDecimal("25.00"), new BigDecimal("520.40"));
        given(inadimplenciaService.resumo(1L)).willReturn(resumo);
        given(solicitacaoVinculoService.contarPendentes(1L)).willReturn(2L);
        given(ocorrenciaService.contarAbertas(1L)).willReturn(3L);

        var estrategia = new DashboardSindicoStrategy(
                avisoService, inadimplenciaService, solicitacaoVinculoService, ocorrenciaService);

        var resposta = (DashboardSindicoResponse) estrategia.montar(usuario(Perfil.SINDICO, null));

        assertThat(resposta.inadimplencia()).isEqualTo(resumo);
        assertThat(resposta.solicitacoesVinculoPendentes()).isEqualTo(2);
        assertThat(resposta.ocorrenciasAbertas()).isEqualTo(3);
    }

    @Test
    void portariaRecebeOPainelDoDia() {
        given(encomendaService.contarRecebidasHoje(1L)).willReturn(7L);

        var estrategia = new DashboardPortariaStrategy(autorizacaoVisitaService, reservaService, encomendaService);

        var resposta = (DashboardPortariaResponse) estrategia.montar(usuario(Perfil.PORTARIA, null));

        assertThat(resposta.encomendasRecebidasHoje()).isEqualTo(7);
        verify(reservaService).doDia(1L, Datas.hoje());
        verify(autorizacaoVisitaService).pendentesDoCondominio(eq(1L), any(Integer.class));
    }

    private UsuarioAutenticado usuario(Perfil perfil, Long unidadeId) {
        return new UsuarioAutenticado(1L, "Teste", "11111111111", perfil, 1L, "Residencial Morah", unidadeId, "jti");
    }
}
