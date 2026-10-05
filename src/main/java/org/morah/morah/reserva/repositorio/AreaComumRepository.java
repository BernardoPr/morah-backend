package org.morah.morah.reserva.repositorio;

import java.util.List;
import java.util.Optional;

import org.morah.morah.reserva.modelo.AreaComum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AreaComumRepository extends MongoRepository<AreaComum, Long> {

    List<AreaComum> findByCondominioIdOrderByNome(Long condominioId);

    /** Garante que a area pertence ao condominio do token (area de outro condominio vira 404). */
    Optional<AreaComum> findByIdAndCondominioId(Long id, Long condominioId);

    long countByCondominioId(Long condominioId);
}
