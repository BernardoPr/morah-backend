package org.morah.morah.ocorrencia.dto;

import org.morah.morah.ocorrencia.modelo.OrigemOcorrencia;

/**
 * Dados para abrir uma ocorrencia a partir de outro modulo.
 *
 * <p>Ex.: {@code new NovaOcorrencia(condominioId, unidadeId, OrigemOcorrencia.ENCOMENDA,
 * encomenda.getId(), "Extravio", "Pacote nao encontrado", logado.id(), logado.nome())}
 */
public record NovaOcorrencia(
        Long condominioId,
        Long unidadeId,
        OrigemOcorrencia origem,
        Long referenciaId,
        String titulo,
        String descricao,
        Long abertaPorId,
        String abertaPorNome) {
}
