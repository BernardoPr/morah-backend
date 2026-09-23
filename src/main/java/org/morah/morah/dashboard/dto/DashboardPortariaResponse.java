package org.morah.morah.dashboard.dto;

import java.util.List;

/** Tela inicial da portaria (schema DashboardPortaria). */
public record DashboardPortariaResponse(
        String perfil,
        List<Object> autorizacoesPendentes,
        List<Object> reservasDoDia,
        long encomendasRecebidasHoje) implements DashboardResponse {
}
