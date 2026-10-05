package org.morah.morah.ocorrencia.repositorio;

import java.util.Collection;
import java.util.List;

import org.morah.morah.ocorrencia.modelo.Ocorrencia;
import org.morah.morah.ocorrencia.modelo.OrigemOcorrencia;
import org.morah.morah.ocorrencia.modelo.StatusOcorrencia;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OcorrenciaRepository extends MongoRepository<Ocorrencia, Long> {

    long countByCondominioIdAndStatusIn(Long condominioId, Collection<StatusOcorrencia> status);

    List<Ocorrencia> findByOrigemAndReferenciaId(OrigemOcorrencia origem, Long referenciaId);
}
