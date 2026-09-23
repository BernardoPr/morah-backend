package org.morah.morah.login.template;

import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.morah.morah.login.dto.LoginRequest;
import org.morah.morah.seguranca.jwt.ServicoJwt;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Autenticacao com CPF + senha (POST /auth/login).
 *
 * <p>Implementa apenas os passos variaveis: onde buscar o usuario, como conferir a senha
 * e qual vinculo ativar. Todo o restante vem do {@link AutenticacaoTemplate}.
 */
@Component
public class AutenticacaoPorSenha extends AutenticacaoTemplate<LoginRequest> {

    private final PasswordEncoder codificadorDeSenha;

    public AutenticacaoPorSenha(UsuarioRepository usuarioRepository,
                                ServicoJwt servicoJwt,
                                PasswordEncoder codificadorDeSenha) {
        super(usuarioRepository, servicoJwt);
        this.codificadorDeSenha = codificadorDeSenha;
    }

    @Override
    protected Usuario buscarUsuario(LoginRequest entrada) {
        return usuarioRepository.findByCpf(somenteNumeros(entrada.cpf()))
                // Mensagem generica de proposito: nao revelamos se o CPF existe.
                .orElseThrow(() -> new CredenciaisInvalidasException("CPF ou senha invalidos."));
    }

    @Override
    protected void conferirCredencial(Usuario usuario, LoginRequest entrada) {
        if (!codificadorDeSenha.matches(entrada.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("CPF ou senha invalidos.");
        }
    }

    @Override
    protected VinculoPerfil escolherVinculo(Usuario usuario, LoginRequest entrada) {
        if (entrada.condominioId() != null && entrada.perfil() != null) {
            return usuario.buscarVinculo(entrada.condominioId(), entrada.perfil())
                    .orElseThrow(() -> new CredenciaisInvalidasException(
                            "Voce nao possui esse perfil nesse condominio."));
        }
        return usuario.vinculoPadrao()
                .orElseThrow(() -> new CredenciaisInvalidasException(
                        "Usuario sem vinculo com nenhum condominio."));
    }

    /** Aceita CPF com ou sem pontuacao. */
    private String somenteNumeros(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }
}
