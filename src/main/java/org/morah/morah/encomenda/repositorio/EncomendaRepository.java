package org.morah.morah.encomenda.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.encomenda.modelo.StatusEncomenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EncomendaRepository extends MongoRepository<Encomenda, Long> {

    // ---------- listagem da portaria/sindico (condominio inteiro) ----------

    Page<Encomenda> findByCondominioId(Long condominioId, Pageable paginacao);

    Page<Encomenda> findByCondominioIdAndStatus(Long condominioId, StatusEncomenda status, Pageable paginacao);

    // ---------- listagem do morador (so a unidade dele) ----------

    Page<Encomenda> findByCondominioIdAndUnidadeId(Long condominioId, Long unidadeId, Pageable paginacao);

    Page<Encomenda> findByCondominioIdAndUnidadeIdAndStatus(Long condominioId, Long unidadeId,
                                                           StatusEncomenda status, Pageable paginacao);

    /** Garante que a encomenda pertence ao condominio do token (senao vira 404). */
    Optional<Encomenda> findByIdAndCondominioId(Long id, Long condominioId);

    /** Encomendas pendentes da unidade (dashboard do morador); o limite vem no {@code Pageable}. */
    List<Encomenda> findByUnidadeIdAndStatus(Long unidadeId, StatusEncomenda status, Pageable limite);

    /**
     * Quantas encomendas chegaram no intervalo [inicio, fim) - dashboard da portaria.
     *
     * <p>Escrita a mao porque o nome derivado ({@code RecebidaEmGreaterThanEqualAndRecebidaEmLessThan})
     * repete o mesmo campo duas vezes, o que o Spring Data MongoDB nao consegue montar.
     */
    @Query(value = "{ 'condominioId': ?0, 'recebidaEm': { $gte: ?1, $lt: ?2 } }", count = true)
    long contarRecebidasEntre(Long condominioId, Instant inicio, Instant fim);

    /** Usado pela carga inicial para nao duplicar os dados de exemplo. */
    long countByCondominioId(Long condominioId);
}
