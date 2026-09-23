package org.morah.morah.usuario.dto;

import java.util.List;

import org.morah.morah.login.dto.PerfilDisponivel;
import org.morah.morah.usuario.modelo.Usuario;

/**
 * Resposta dos endpoints de usuario.
 *
 * <p>Repare que a senha (nem mesmo o hash) NUNCA aparece aqui - esse e o motivo de
 * existir um DTO de saida em vez de devolver a entidade do banco direto.
 */
public record UsuarioResponse(
        Long id,
        String nome,
        String cpf,
        String email,
        String telefone,
        boolean ativo,
        List<PerfilDisponivel> vinculos) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getCpf(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.isAtivo(),
                usuario.getVinculos().stream().map(PerfilDisponivel::de).toList());
    }
}
