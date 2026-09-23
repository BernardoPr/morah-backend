package org.morah.morah.aviso.repositorio;

import java.util.List;

import org.morah.morah.aviso.modelo.Aviso;
import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AvisoRepository extends MongoRepository<Aviso, Long> {

    Page<Aviso> findByCondominioId(Long condominioId, Pageable paginacao);

    Page<Aviso> findByCondominioIdAndPrioridade(Long condominioId, PrioridadeAviso prioridade, Pageable paginacao);

    List<Aviso> findByCondominioId(Long condominioId, Sort ordenacao);
}
