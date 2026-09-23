package org.morah.morah.seguranca.jwt;

import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Atalho para descobrir quem esta logado de dentro de um service
 * (nos controllers prefira {@code @AuthenticationPrincipal UsuarioAutenticado usuario}).
 */
public final class ContextoDeSeguranca {

    private ContextoDeSeguranca() {
    }

    public static UsuarioAutenticado usuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new CredenciaisInvalidasException("Nenhum usuario autenticado na requisicao.");
        }
        return usuario;
    }
}
