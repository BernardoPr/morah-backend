package org.morah.morah.notificacao.repositorio;

import org.morah.morah.notificacao.modelo.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacaoRepository extends MongoRepository<Notificacao, Long> {

    Page<Notificacao> findByDestinatarioIdOrderByEnviadaEmDesc(Long destinatarioId, Pageable paginacao);
}
