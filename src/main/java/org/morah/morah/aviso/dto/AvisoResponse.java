package org.morah.morah.aviso.dto;

import java.time.Instant;

import org.morah.morah.aviso.modelo.Aviso;
import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.morah.morah.comum.dto.PessoaResumo;

/** Resposta do contrato (schema Aviso). */
public record AvisoResponse(
        Long id,
        String titulo,
        String conteudo,
        PrioridadeAviso prioridade,
        PessoaResumo autor,
        Instant publicadoEm,
        Instant expiraEm,
        boolean lidoPeloUsuario) {

    /**
     * @param usuarioId quem esta vendo o aviso: o campo {@code lidoPeloUsuario} muda de pessoa
     *                  para pessoa, por isso a conversao precisa saber quem perguntou.
     */
    public static AvisoResponse de(Aviso aviso, Long usuarioId) {
        return new AvisoResponse(
                aviso.getId(),
                aviso.getTitulo(),
                aviso.getConteudo(),
                aviso.getPrioridade(),
                new PessoaResumo(aviso.getAutorId(), aviso.getAutorNome(), null),
                aviso.getPublicadoEm(),
                aviso.getExpiraEm(),
                aviso.foiLidoPor(usuarioId));
    }
}
