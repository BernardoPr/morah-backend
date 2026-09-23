package org.morah.morah.login.template;

import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.login.dto.RefreshRequest;
import org.morah.morah.seguranca.jwt.ServicoJwt;
import org.morah.morah.seguranca.singleton.RegistroDeTokensRevogadosSingleton;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;

/**
 * Renovacao da sessao com refresh token (POST /auth/refresh).
 *
 * <p>Mesmo esqueleto do login por senha, com passos diferentes: em vez de conferir senha,
 * conferimos a assinatura e o tipo do token; o contexto ativo e recuperado das claims.
 */
@Component
public class AutenticacaoPorRefreshToken extends AutenticacaoTemplate<RefreshRequest> {

    public AutenticacaoPorRefreshToken(UsuarioRepository usuarioRepository, ServicoJwt servicoJwt) {
        super(usuarioRepository, servicoJwt);
    }

    @Override
    protected Usuario buscarUsuario(RefreshRequest entrada) {
        Claims claims = servicoJwt.lerClaims(entrada.refreshToken());
        return usuarioRepository.findById(Long.valueOf(claims.getSubject()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuario do token nao existe mais."));
    }

    @Override
    protected void conferirCredencial(Usuario usuario, RefreshRequest entrada) {
        Claims claims = servicoJwt.lerClaims(entrada.refreshToken());

        boolean ehRefresh = ServicoJwt.TIPO_REFRESH.equals(claims.get("tipo", String.class));
        if (!ehRefresh) {
            throw new CredenciaisInvalidasException("Envie um refresh token, nao um access token.");
        }
        if (RegistroDeTokensRevogadosSingleton.getInstancia().estaRevogado(claims.getId())) {
            throw new CredenciaisInvalidasException("Sessao encerrada. Faca login novamente.");
        }
    }

    @Override
    protected VinculoPerfil escolherVinculo(Usuario usuario, RefreshRequest entrada) {
        Claims claims = servicoJwt.lerClaims(entrada.refreshToken());
        Number condominioId = claims.get("condominio_ativo", Number.class);
        String perfil = claims.get("perfil_ativo", String.class);

        if (condominioId == null || perfil == null) {
            return usuario.vinculoPadrao()
                    .orElseThrow(() -> new CredenciaisInvalidasException("Usuario sem vinculo."));
        }
        return usuario.buscarVinculo(condominioId.longValue(), Perfil.de(perfil))
                .orElseThrow(() -> new CredenciaisInvalidasException("O vinculo do token nao existe mais."));
    }

    /** Hook: o refresh token usado e queimado, para nao servir duas vezes (rotacao de token). */
    @Override
    protected void aposAutenticar(Usuario usuario, RefreshRequest entrada) {
        Claims claims = servicoJwt.lerClaims(entrada.refreshToken());
        RegistroDeTokensRevogadosSingleton.getInstancia().revogar(claims.getId());
    }
}
