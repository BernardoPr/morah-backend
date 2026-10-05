package org.morah.morah.dashboard.strategy;

import org.morah.morah.aviso.servico.AvisoService;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.dashboard.dto.DashboardSindicoResponse;
import org.morah.morah.financeiro.servico.InadimplenciaService;
import org.morah.morah.ocorrencia.servico.OcorrenciaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.servico.SolicitacaoVinculoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Tela inicial do sindico: visao administrativa do condominio. */
@Component
@RequiredArgsConstructor
public class DashboardSindicoStrategy implements DashboardStrategy {

    private static final int QUANTIDADE_DE_AVISOS = 5;

    private final AvisoService avisoService;
    private final InadimplenciaService inadimplenciaService;
    private final SolicitacaoVinculoService solicitacaoVinculoService;
    private final OcorrenciaService ocorrenciaService;

    @Override
    public Perfil perfilAtendido() {
        return Perfil.SINDICO;
    }

    @Override
    public DashboardResponse montar(UsuarioAutenticado usuario) {
        Long condominioId = usuario.condominioId();
        var paginacao = PageRequest.of(0, QUANTIDADE_DE_AVISOS, Sort.by(Sort.Direction.DESC, "publicadoEm"));

        return new DashboardSindicoResponse(
                usuario.perfil().getValor(),
                inadimplenciaService.resumo(condominioId),
                avisoService.listarVisiveis(usuario, null, paginacao).content(),
                solicitacaoVinculoService.contarPendentes(condominioId),
                ocorrenciaService.contarAbertas(condominioId));
    }
}
