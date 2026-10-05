package org.morah.morah.portaria.repositorio;

import java.time.Instant;
import java.util.Optional;

import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Consultas das autorizacoes de visita.
 *
 * <p>Por causa da expiracao preguicosa (ver {@code AutorizacaoVisita}), o filtro por status nao e
 * um simples "status = X":
 * <ul>
 *   <li>pendente = gravada como PENDENTE <b>e</b> ainda dentro do prazo ({@code expiraEm > agora});</li>
 *   <li>expirada = gravada como EXPIRADA <b>ou</b> PENDENTE com o prazo vencido;</li>
 *   <li>autorizada/recusada = status gravado.</li>
 * </ul>
 * Cada filtro existe em duas versoes: condominio inteiro (fila da guarita) e uma unidade (morador).
 * O {@code unidadeId} sozinho ja basta no segundo caso: o id e unico no sistema e a unidade do
 * token sempre pertence ao condominio do token.
 *
 * <p>O enum e gravado no Mongo pelo nome da constante ("PENDENTE"), por isso as consultas
 * escritas a mao usam os valores em maiusculo.
 */
@Repository
public interface AutorizacaoVisitaRepository extends MongoRepository<AutorizacaoVisita, Long> {

    Optional<AutorizacaoVisita> findByIdAndCondominioId(Long id, Long condominioId);

    // ---------- condominio inteiro (portaria) ----------

    Page<AutorizacaoVisita> findByCondominioId(Long condominioId, Pageable paginacao);

    Page<AutorizacaoVisita> findByCondominioIdAndStatus(Long condominioId, StatusAutorizacaoVisita status,
                                                        Pageable paginacao);

    /** Pendentes ainda no prazo: {@code status = ?1 e expiraEm > ?2}. */
    Page<AutorizacaoVisita> findByCondominioIdAndStatusAndExpiraEmAfter(Long condominioId,
                                                                        StatusAutorizacaoVisita status,
                                                                        Instant agora, Pageable paginacao);

    @Query("{ 'condominioId': ?0, '$or': [ { 'status': 'EXPIRADA' }, "
            + "{ 'status': 'PENDENTE', 'expiraEm': { '$lte': ?1 } } ] }")
    Page<AutorizacaoVisita> buscarExpiradasDoCondominio(Long condominioId, Instant agora, Pageable paginacao);

    // ---------- uma unidade (morador/proprietario e dashboard) ----------

    Page<AutorizacaoVisita> findByUnidadeId(Long unidadeId, Pageable paginacao);

    Page<AutorizacaoVisita> findByUnidadeIdAndStatus(Long unidadeId, StatusAutorizacaoVisita status,
                                                     Pageable paginacao);

    Page<AutorizacaoVisita> findByUnidadeIdAndStatusAndExpiraEmAfter(Long unidadeId,
                                                                     StatusAutorizacaoVisita status,
                                                                     Instant agora, Pageable paginacao);

    @Query("{ 'unidadeId': ?0, '$or': [ { 'status': 'EXPIRADA' }, "
            + "{ 'status': 'PENDENTE', 'expiraEm': { '$lte': ?1 } } ] }")
    Page<AutorizacaoVisita> buscarExpiradasDaUnidade(Long unidadeId, Instant agora, Pageable paginacao);

    // ---------- registro de entrada ----------

    /**
     * Autorizacao mais recente do visitante com o status pedido e que ainda nao foi usada em
     * nenhuma entrada (usada quando o porteiro registra a entrada sem informar a autorizacao).
     */
    Optional<AutorizacaoVisita> findFirstByCondominioIdAndVisitanteIdAndStatusAndEntradaEmIsNullOrderByRespondidoEmDesc(
            Long condominioId, Long visitanteId, StatusAutorizacaoVisita status);
}
