package org.morah.morah.unidade.repositorio;

import java.util.List;
import java.util.Optional;

import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.StatusSolicitacaoVinculo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitacaoVinculoRepository extends MongoRepository<SolicitacaoVinculo, Long> {

    /** Solicitacoes da unidade num status (ex.: as pendentes), das mais antigas para as mais novas. */
    List<SolicitacaoVinculo> findByUnidadeIdAndCondominioIdAndStatusOrderBySolicitadoEmAsc(
            Long unidadeId, Long condominioId, StatusSolicitacaoVinculo status);

    /** A solicitacao precisa ser da unidade da URL e do condominio do token; senao, 404. */
    Optional<SolicitacaoVinculo> findByIdAndUnidadeIdAndCondominioId(Long id, Long unidadeId, Long condominioId);

    /** Contador do dashboard do sindico (campo solicitacoesVinculoPendentes). */
    long countByCondominioIdAndStatus(Long condominioId, StatusSolicitacaoVinculo status);
}
