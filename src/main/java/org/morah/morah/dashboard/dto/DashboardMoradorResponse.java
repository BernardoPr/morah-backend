package org.morah.morah.dashboard.dto;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.encomenda.dto.EncomendaResponse;
import org.morah.morah.financeiro.dto.CobrancaResponse;
import org.morah.morah.portaria.dto.AutorizacaoVisitaResponse;
import org.morah.morah.reserva.dto.ReservaResponse;
import org.morah.morah.unidade.dto.UnidadeResponse;

/** Tela inicial de morador/proprietario (schema DashboardMorador). */
public record DashboardMoradorResponse(
        String perfil,
        UnidadeResponse unidade,
        List<CobrancaResponse> proximosBoletos,
        List<AvisoResponse> avisosRecentes,
        List<EncomendaResponse> encomendasPendentes,
        List<AutorizacaoVisitaResponse> autorizacoesPendentes,
        List<ReservaResponse> proximasReservas) implements DashboardResponse {
}
