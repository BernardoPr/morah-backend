package org.morah.morah.usuario.repositorio;

import java.util.Optional;

import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio do MongoDB.
 *
 * <p>Nao precisamos escrever nenhuma consulta: o Spring Data cria a implementacao a partir
 * do NOME do metodo (findByCpf -> procura pelo campo "cpf").
 */
@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, Long> {

    Optional<Usuario> findByCpf(String cpf);

    Optional<Usuario> findByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}
