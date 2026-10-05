package org.morah.morah.dashboard.dto;

import java.util.List;

import org.morah.morah.portaria.dto.AutorizacaoVisitaResponse;
import org.morah.morah.reserva.dto.ReservaResponse;

/** Tela inicial da portaria (schema DashboardPortaria). */
public record DashboardPortariaResponse(
        String perfil,
        List<AutorizacaoVisitaResponse> autorizacoesPendentes,
        List<ReservaResponse> reservasDoDia,
        long encomendasRecebidasHoje) implements DashboardResponse {
}
