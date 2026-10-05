package org.morah.morah.usuario.repositorio;

import java.util.List;
import java.util.Optional;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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

    /**
     * Usuarios ativos com acesso a unidade (moradores, proprietarios, inquilinos).
     * Usado para notificar "a unidade" (visitante na portaria, encomenda recebida...).
     * O nome do metodo vira a consulta {@code { "vinculos.unidadeId": ?0, "ativo": true }}.
     */
    List<Usuario> findByVinculosUnidadeIdAndAtivoTrue(Long unidadeId);

    /** Usuarios ativos com qualquer vinculo no condominio (ex.: aviso para o condominio inteiro). */
    List<Usuario> findByVinculosCondominioIdAndAtivoTrue(Long condominioId);

    /**
     * Usuarios ativos que tem o perfil no condominio (ex.: todos os sindicos do condominio 1).
     *
     * <p>O {@code $elemMatch} exige que condominio e perfil estejam no MESMO vinculo: sem ele,
     * quem e sindico no condominio 2 e morador no 1 apareceria como sindico do 1.
     */
    @Query("{ 'ativo': true, 'vinculos': { $elemMatch: { 'condominioId': ?0, 'perfil': ?1 } } }")
    List<Usuario> buscarPorPerfilNoCondominio(Long condominioId, String perfil);

    /** Versao tipada de {@link #buscarPorPerfilNoCondominio} (no banco o perfil fica como "SINDICO"). */
    default List<Usuario> listarPorPerfilNoCondominio(Long condominioId, Perfil perfil) {
        return buscarPorPerfilNoCondominio(condominioId, perfil.name());
    }
}
