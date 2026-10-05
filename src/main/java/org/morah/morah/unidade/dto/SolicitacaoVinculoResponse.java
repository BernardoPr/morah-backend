package org.morah.morah.unidade.dto;

import java.time.Instant;

import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.StatusSolicitacaoVinculo;
import org.morah.morah.unidade.modelo.TipoVinculo;

/**
 * Resposta do contrato (schema SolicitacaoVinculo).
 *
 * <p>So os campos do contrato: CPF, observacao e dados de quem decidiu ficam gravados na
 * entidade (historico), mas nao fazem parte do schema combinado com o front.
 */
public record SolicitacaoVinculoResponse(
        Long id,
        Long unidadeId,
        TipoVinculo tipoVinculo,
        String nomeSolicitado,
        StatusSolicitacaoVinculo status,
        Instant solicitadoEm) {

    public static SolicitacaoVinculoResponse de(SolicitacaoVinculo solicitacao) {
        return new SolicitacaoVinculoResponse(
                solicitacao.getId(),
                solicitacao.getUnidadeId(),
                solicitacao.getTipoVinculo(),
                solicitacao.getNomeSolicitado(),
                solicitacao.getStatus(),
                solicitacao.getSolicitadoEm());
    }
}
