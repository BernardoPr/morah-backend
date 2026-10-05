package org.morah.morah.aviso.repositorio;

import org.morah.morah.aviso.modelo.Aviso;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio dos avisos.
 *
 * <p>A listagem combina varios filtros opcionais (vigencia, prioridade, publico alvo), o que
 * daria nomes de metodo enormes; por isso ela e montada com {@code Criteria} no
 * {@code AvisoService}. Aqui ficam so as operacoes basicas herdadas do Spring Data.
 */
@Repository
public interface AvisoRepository extends MongoRepository<Aviso, Long> {
}
