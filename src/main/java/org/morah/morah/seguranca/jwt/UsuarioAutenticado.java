package org.morah.morah.seguranca.jwt;

import org.morah.morah.comum.modelo.Perfil;

/**
 * Dados do usuario logado, extraidos do token a cada requisicao.
 *
 * <p>Nos controllers, basta pedir:
 * <pre>{@code
 * @GetMapping("/exemplo")
 * public Algo exemplo(@AuthenticationPrincipal UsuarioAutenticado usuario) { ... }
 * }</pre>
 *
 * <p>Por isso nenhum endpoint precisa receber condominioId/unidadeId na URL: o contexto
 * ativo ja vem assinado dentro do token (decisao registrada no contrato).
 */
public record UsuarioAutenticado(
        Long id,
        String nome,
        String cpf,
        Perfil perfil,
        Long condominioId,
        String condominioNome,
        Long unidadeId,
        String idDoToken) {
}
