package org.morah.morah.login.dto;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.usuario.modelo.VinculoPerfil;

/** Schema ContextoAtivo: onde e como o usuario esta navegando agora. */
public record ContextoAtivo(Perfil perfil, Long condominioId, String condominioNome, Long unidadeId) {

    public static ContextoAtivo de(VinculoPerfil vinculo) {
        return new ContextoAtivo(
                vinculo.getPerfil(),
                vinculo.getCondominioId(),
                vinculo.getCondominioNome(),
                vinculo.getUnidadeId());
    }
}
