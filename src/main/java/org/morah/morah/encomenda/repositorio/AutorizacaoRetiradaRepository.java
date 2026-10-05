package org.morah.morah.encomenda.repositorio;

import java.util.List;

import org.morah.morah.encomenda.modelo.AutorizacaoRetirada;
import org.morah.morah.encomenda.modelo.StatusAutorizacaoRetirada;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutorizacaoRetiradaRepository extends MongoRepository<AutorizacaoRetirada, Long> {

    /**
     * Autorizacoes de uma encomenda em um status (normalmente ATIVA). Usado para conferir o
     * codigo do terceiro na portaria, para nao repetir codigos e para invalidar as que sobraram
     * depois da retirada.
     */
    List<AutorizacaoRetirada> findByEncomendaIdAndStatus(Long encomendaId, StatusAutorizacaoRetirada status);
}
