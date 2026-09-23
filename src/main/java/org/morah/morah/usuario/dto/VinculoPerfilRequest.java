package org.morah.morah.usuario.dto;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.usuario.modelo.VinculoPerfil;

import jakarta.validation.constraints.NotNull;

/** Um vinculo enviado no cadastro do usuario. */
public record VinculoPerfilRequest(
        @NotNull(message = "informe o perfil") Perfil perfil,
        @NotNull(message = "informe o condominioId") Long condominioId,
        String condominioNome,
        Long unidadeId,
        String unidadeIdentificacao) {

    public VinculoPerfil paraModelo() {
        return new VinculoPerfil(perfil, condominioId, condominioNome, unidadeId, unidadeIdentificacao);
    }
}
