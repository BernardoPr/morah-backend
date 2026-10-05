package org.morah.morah.unidade.repositorio;

import java.util.List;
import java.util.Optional;

import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VinculoUnidadeRepository extends MongoRepository<VinculoUnidade, Long> {

    /**
     * Vinculos ainda ativos da unidade. O filtro de vigencia ({@code fim >= hoje}) e feito em
     * Java ({@link VinculoUnidade#estaVigenteEm}): uma unidade tem poucos vinculos e assim a
     * regra fica num lugar so, testavel sem banco.
     */
    List<VinculoUnidade> findByUnidadeIdAndCondominioIdAndAtivoTrue(Long unidadeId, Long condominioId);

    /** Garante que o vinculo e do condominio do token (outro condominio = 404). */
    Optional<VinculoUnidade> findByIdAndCondominioId(Long id, Long condominioId);
}
