package org.morah.morah.financeiro.repositorio;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Consultas fixas de cobrancas. A listagem com filtros opcionais (status, competencia, unidade)
 * nao esta aqui: ela e montada com {@code MongoTemplate} + {@code Criteria} no
 * {@code CobrancaService}, porque cada combinacao de filtros viraria um metodo diferente.
 */
@Repository
public interface CobrancaRepository extends MongoRepository<Cobranca, Long> {

    /** Garante que a cobranca e do condominio do token (outro condominio = 404). */
    Optional<Cobranca> findByIdAndCondominioId(Long id, Long condominioId);

    /** Cobrancas ja geradas para uma taxa (para nao gerar duas vezes para a mesma unidade). */
    List<Cobranca> findByTaxaId(Long taxaId);

    /** Proximos boletos da unidade (dashboard): o {@code Pageable} so limita a quantidade. */
    List<Cobranca> findByUnidadeIdAndStatusOrderByVencimentoAsc(Long unidadeId, StatusCobranca status,
                                                                Pageable limite);

    /** Pendentes que venceram antes da data informada, ou seja, as atrasadas. */
    List<Cobranca> findByCondominioIdAndStatusAndVencimentoBefore(Long condominioId, StatusCobranca status,
                                                                  LocalDate data);

    /** Idempotencia do webhook: a mesma transacao do gateway ja quitou alguma cobranca? */
    boolean existsByTransacaoId(String transacaoId);
}
