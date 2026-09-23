package org.morah.morah.dashboard.dto;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;

/** Tela inicial do sindico (schema DashboardSindico). */
public record DashboardSindicoResponse(
        String perfil,
        List<AvisoResponse> avisosPublicadosRecentes,
        long solicitacoesVinculoPendentes,
        long ocorrenciasAbertas) implements DashboardResponse {
}
