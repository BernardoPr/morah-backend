package org.morah.morah.login.dto;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.usuario.modelo.VinculoPerfil;

/** Schema PerfilDisponivel: um dos vinculos que o usuario pode ativar. */
public record PerfilDisponivel(Perfil perfil, Long condominioId, String condominioNome, Long unidadeId) {

    public static PerfilDisponivel de(VinculoPerfil vinculo) {
        return new PerfilDisponivel(
                vinculo.getPerfil(),
                vinculo.getCondominioId(),
                vinculo.getCondominioNome(),
                vinculo.getUnidadeId());
    }
}
