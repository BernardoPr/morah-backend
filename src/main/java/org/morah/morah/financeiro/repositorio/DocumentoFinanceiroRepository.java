package org.morah.morah.financeiro.repositorio;

import org.morah.morah.financeiro.modelo.DocumentoFinanceiro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentoFinanceiroRepository extends MongoRepository<DocumentoFinanceiro, Long> {

    Page<DocumentoFinanceiro> findByCondominioId(Long condominioId, Pageable paginacao);

    /** Usado pela carga inicial para nao publicar o mesmo documento duas vezes. */
    boolean existsByCondominioIdAndTitulo(Long condominioId, String titulo);
}
