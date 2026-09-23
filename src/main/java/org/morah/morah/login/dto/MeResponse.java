package org.morah.morah.login.dto;

import java.util.List;

/** Schema MeResponse: resposta do GET /auth/me. */
public record MeResponse(Pessoa pessoa, List<PerfilDisponivel> perfisDisponiveis, ContextoAtivo contextoAtivo) {

    public record Pessoa(Long id, String nome, String cpf, String email) {
    }
}
