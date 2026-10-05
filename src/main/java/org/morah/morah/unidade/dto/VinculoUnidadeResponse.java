package org.morah.morah.unidade.dto;

import java.time.LocalDate;

import org.morah.morah.comum.dto.PessoaResumo;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.VinculoUnidade;

/**
 * Resposta do contrato (schema VinculoUnidade).
 *
 * <p>A pessoa sai como {@link PessoaResumo} (id, nome, telefone): o CPF gravado no vinculo
 * nao e devolvido, porque quem lista os vinculos e qualquer morador da unidade.
 */
public record VinculoUnidadeResponse(
        Long id,
        Long unidadeId,
        PessoaResumo pessoa,
        TipoVinculo tipoVinculo,
        boolean principal,
        LocalDate inicio,
        LocalDate fim) {

    public static VinculoUnidadeResponse de(VinculoUnidade vinculo) {
        return new VinculoUnidadeResponse(
                vinculo.getId(),
                vinculo.getUnidadeId(),
                new PessoaResumo(vinculo.getPessoaId(), vinculo.getPessoaNome(), vinculo.getPessoaTelefone()),
                vinculo.getTipoVinculo(),
                vinculo.isPrincipal(),
                vinculo.getInicio(),
                vinculo.getFim());
    }
}
