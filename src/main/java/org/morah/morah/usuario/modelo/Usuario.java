package org.morah.morah.usuario.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.morah.morah.comum.modelo.Perfil;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * MODELO DE EXEMPLO DO BANCO (colecao "usuarios").
 *
 * <p>Use este arquivo como referencia para criar as demais entidades:
 * estenda {@link EntidadeBase}, anote com {@code @Document} e marque com
 * {@code @Indexed(unique = true)} os campos que nao podem se repetir.
 *
 * <p>Exemplo de documento gravado no Atlas:
 * <pre>{@code
 * {
 *   "_id": 1,
 *   "cpf": "12345678901",
 *   "email": "ana@morah.com.br",
 *   "nome": "Ana Souza",
 *   "senhaHash": "$2a$10$...",
 *   "ativo": true,
 *   "vinculos": [
 *     { "perfil": "MORADOR", "condominioId": 1, "condominioNome": "Ed. Morah", "unidadeId": 10 }
 *   ],
 *   "criadoEm": "2026-09-18T12:00:00Z"
 * }
 * }</pre>
 */
@Getter
@Setter
@Document(collection = "usuarios")
public class Usuario extends EntidadeBase {

    @Indexed(unique = true)
    private String cpf;

    @Indexed(unique = true)
    private String email;

    private String nome;

    private String telefone;

    /** Nunca guardamos a senha em texto puro: aqui fica o hash BCrypt. */
    private String senhaHash;

    private boolean ativo = true;

    private List<VinculoPerfil> vinculos = new ArrayList<>();

    /** Procura o vinculo (condominio + perfil) que o usuario pediu no login. */
    public Optional<VinculoPerfil> buscarVinculo(Long condominioId, Perfil perfil) {
        return vinculos.stream()
                .filter(vinculo -> vinculo.getCondominioId().equals(condominioId)
                        && vinculo.getPerfil() == perfil)
                .findFirst();
    }

    /** Vinculo usado quando o usuario nao escolheu nenhum (ele so tem um, por exemplo). */
    public Optional<VinculoPerfil> vinculoPadrao() {
        return vinculos.stream().findFirst();
    }
}
