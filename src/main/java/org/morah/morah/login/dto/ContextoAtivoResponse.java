package org.morah.morah.login.dto;

/** Schema ContextoAtivoResponse: resposta do login, do refresh e da troca de contexto. */
public record ContextoAtivoResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        ContextoAtivo contexto) {
}
