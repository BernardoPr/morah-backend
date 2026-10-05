package org.morah.morah.portaria.repositorio;

import java.time.Instant;
import java.util.Optional;

import org.morah.morah.portaria.modelo.Acesso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AcessoRepository extends MongoRepository<Acesso, Long> {

    Page<Acesso> findByCondominioId(Long condominioId, Pageable paginacao);

    /**
     * Acessos cuja entrada aconteceu no intervalo {@code [inicio, fim)}.
     *
     * <p>Escrita a mao porque a consulta derivada do nome com dois filtros no mesmo campo
     * ({@code EntradaGreaterThanEqualAndEntradaLessThan}) nao e aceita pelo Spring Data MongoDB.
     */
    @Query("{ 'condominioId': ?0, 'entrada': { '$gte': ?1, '$lt': ?2 } }")
    Page<Acesso> buscarEntradasEntre(Long condominioId, Instant inicio, Instant fim, Pageable paginacao);

    /** O visitante esta dentro do condominio? (entrada registrada e saida ainda nao). */
    boolean existsByCondominioIdAndVisitanteIdAndSaidaIsNull(Long condominioId, Long visitanteId);

    /** Acesso em aberto do visitante, que sera completado pela saida. */
    Optional<Acesso> findFirstByCondominioIdAndVisitanteIdAndSaidaIsNullOrderByEntradaDesc(Long condominioId,
                                                                                           Long visitanteId);
}
