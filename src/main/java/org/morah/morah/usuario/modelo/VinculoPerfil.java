package org.morah.morah.usuario.modelo;

import org.morah.morah.comum.modelo.Perfil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Liga um usuario a um condominio com um perfil.
 *
 * <p>A mesma pessoa pode ser moradora em um condominio e sindica em outro, por isso
 * o usuario tem uma LISTA de vinculos. E o vinculo escolhido no login (o "contexto ativo")
 * que define o que ela pode fazer.
 *
 * <p>Fica embutido dentro do documento do usuario (no Mongo nao precisamos de outra colecao
 * so para isso).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VinculoPerfil {

    private Perfil perfil;
    private Long condominioId;
    private String condominioNome;

    /** Preenchido para morador/proprietario; fica nulo para sindico e portaria. */
    private Long unidadeId;
    private String unidadeIdentificacao;
}
