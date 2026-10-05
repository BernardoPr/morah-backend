package org.morah.morah.unidade.repositorio;

import java.util.List;
import java.util.Optional;

import org.morah.morah.unidade.modelo.Unidade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnidadeRepository extends MongoRepository<Unidade, Long> {

    List<Unidade> findByCondominioIdOrderByIdentificacao(Long condominioId);

    List<Unidade> findByCondominioIdAndBlocoId(Long condominioId, Long blocoId);

    /** Garante que a unidade pertence ao condominio do token (evita ver dados de outro condominio). */
    Optional<Unidade> findByIdAndCondominioId(Long id, Long condominioId);

    long countByCondominioId(Long condominioId);
}
