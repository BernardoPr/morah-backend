package org.morah.morah.seguranca.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.seguranca.singleton.RegistroDeTokensRevogadosSingleton;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Roda uma vez por requisicao: le o cabecalho {@code Authorization: Bearer <token>},
 * valida o JWT e coloca o usuario no contexto do Spring Security.
 *
 * <p>Se o token nao existir ou for invalido, o filtro apenas segue em frente: quem responde
 * 401 e a configuracao de seguranca (rotas publicas continuam funcionando).
 */
@Component
@RequiredArgsConstructor
public class FiltroAutenticacaoJwt extends OncePerRequestFilter {

    private final ServicoJwt servicoJwt;

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao,
                                    HttpServletResponse resposta,
                                    FilterChain cadeia) throws ServletException, IOException {

        String token = extrairToken(requisicao);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(token);
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private void autenticar(String token) {
        try {
            Claims claims = servicoJwt.lerClaims(token);

            boolean ehAccessToken = ServicoJwt.TIPO_ACCESS.equals(claims.get("tipo", String.class));
            boolean revogado = RegistroDeTokensRevogadosSingleton.getInstancia().estaRevogado(claims.getId());
            if (!ehAccessToken || revogado) {
                return;
            }

            UsuarioAutenticado usuario = servicoJwt.paraUsuarioAutenticado(claims);
            var autenticacao = new UsernamePasswordAuthenticationToken(
                    usuario, null, permissoesDe(usuario.perfil()));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);

        } catch (RuntimeException excecao) {
            // Token invalido: segue sem autenticar (a requisicao recebera 401 adiante).
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Converte o perfil ativo em permissoes do Spring Security.
     * O contrato diz que "proprietario" tem tudo que o "morador" tem, entao ele recebe as duas.
     */
    private List<GrantedAuthority> permissoesDe(Perfil perfil) {
        List<GrantedAuthority> permissoes = new ArrayList<>();
        permissoes.add(new SimpleGrantedAuthority(perfil.comoRole()));
        if (perfil == Perfil.PROPRIETARIO) {
            permissoes.add(new SimpleGrantedAuthority(Perfil.MORADOR.comoRole()));
        }
        return permissoes;
    }

    private String extrairToken(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            return cabecalho.substring(7);
        }
        return null;
    }
}
