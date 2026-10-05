package org.morah.morah.portaria.repositorio;

import java.util.Optional;

import org.morah.morah.portaria.modelo.Visitante;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitanteRepository extends MongoRepository<Visitante, Long> {

    /** Garante que o visitante pertence ao condominio do token (outro condominio = 404). */
    Optional<Visitante> findByIdAndCondominioId(Long id, Long condominioId);

    /** Usado para reaproveitar o cadastro de quem ja visitou o condominio com o mesmo documento. */
    Optional<Visitante> findFirstByCondominioIdAndDocumento(Long condominioId, String documento);
}
