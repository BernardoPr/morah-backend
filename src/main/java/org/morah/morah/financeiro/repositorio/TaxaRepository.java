package org.morah.morah.financeiro.repositorio;

import java.util.Optional;

import org.morah.morah.financeiro.modelo.Taxa;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxaRepository extends MongoRepository<Taxa, Long> {

    /** Garante que a taxa e do condominio do token (outro condominio = 404). */
    Optional<Taxa> findByIdAndCondominioId(Long id, Long condominioId);

    /** Usado pela carga inicial para nao criar a mesma taxa duas vezes. */
    Optional<Taxa> findFirstByCondominioIdAndCompetenciaAndDescricao(Long condominioId, String competencia,
                                                                     String descricao);
}
