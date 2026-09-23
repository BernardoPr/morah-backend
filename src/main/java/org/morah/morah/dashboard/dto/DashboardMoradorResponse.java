package org.morah.morah.dashboard.dto;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;

/** Tela inicial de morador/proprietario (schema DashboardMorador). */
public record DashboardMoradorResponse(
        String perfil,
        Long unidadeId,
        List<AvisoResponse> avisosRecentes,
        List<Object> proximosBoletos,
        List<Object> encomendasPendentes,
        List<Object> autorizacoesPendentes,
        List<Object> proximasReservas) implements DashboardResponse {
}
