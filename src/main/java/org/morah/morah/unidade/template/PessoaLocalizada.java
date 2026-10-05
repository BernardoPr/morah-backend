package org.morah.morah.unidade.template;

import org.morah.morah.usuario.modelo.Usuario;

/**
 * Resultado do passo "localizar a pessoa" do {@link ConcessaoDeAcessoTemplate}.
 *
 * @param usuario         cadastro da pessoa no app; nulo quando ela nao tera acesso (ex.: dependente sem CPF)
 * @param senhaTemporaria preenchida so quando o usuario acabou de ser criado - vai no convite
 */
public record PessoaLocalizada(Usuario usuario, String senhaTemporaria) {

    /** A pessoa entra so no cadastro da unidade, sem acesso ao app. */
    public static PessoaLocalizada semAcessoAoApp() {
        return new PessoaLocalizada(null, null);
    }

    /** A pessoa ja tinha cadastro no app. */
    public static PessoaLocalizada existente(Usuario usuario) {
        return new PessoaLocalizada(usuario, null);
    }

    /** O cadastro foi criado agora, com uma senha temporaria. */
    public static PessoaLocalizada criadaAgora(Usuario usuario, String senhaTemporaria) {
        return new PessoaLocalizada(usuario, senhaTemporaria);
    }

    public boolean temAcessoAoApp() {
        return usuario != null;
    }

    public boolean foiCriadaAgora() {
        return senhaTemporaria != null;
    }

    /** Nunca escreve a senha em log por acidente (o toString padrao do record a mostraria). */
    @Override
    public String toString() {
        return "PessoaLocalizada[usuarioId=%s, criadaAgora=%s]"
                .formatted(usuario == null ? null : usuario.getId(), foiCriadaAgora());
    }
}
