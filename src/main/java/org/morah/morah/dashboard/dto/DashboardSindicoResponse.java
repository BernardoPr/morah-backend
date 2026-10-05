package org.morah.morah.dashboard.dto;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.financeiro.dto.InadimplenciaResumoResponse;

/** Tela inicial do sindico (schema DashboardSindico). */
public record DashboardSindicoResponse(
        String perfil,
        InadimplenciaResumoResponse inadimplencia,
        List<AvisoResponse> avisosPublicadosRecentes,
        long solicitacoesVinculoPendentes,
        long ocorrenciasAbertas) implements DashboardResponse {
}
