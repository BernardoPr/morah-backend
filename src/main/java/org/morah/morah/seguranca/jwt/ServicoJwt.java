package org.morah.morah.seguranca.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.config.PropriedadesMorah;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Cria e le os tokens JWT da aplicacao.
 *
 * <p>Sao dois tipos de token:
 * <ul>
 *   <li><b>access</b>: curto (1h), carrega o contexto ativo e acompanha cada requisicao;</li>
 *   <li><b>refresh</b>: longo (7 dias), serve apenas para pedir um novo access em /auth/refresh.</li>
 * </ul>
 */
@Service
public class ServicoJwt {

    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey chave;
    private final long minutosToken;
    private final long diasRefresh;

    public ServicoJwt(PropriedadesMorah propriedades) {
        this.chave = Keys.hmacShaKeyFor(propriedades.jwt().segredo().getBytes(StandardCharsets.UTF_8));
        this.minutosToken = propriedades.jwt().minutosToken();
        this.diasRefresh = propriedades.jwt().diasRefresh();
    }

    public String gerarAccessToken(Usuario usuario, VinculoPerfil vinculo) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(usuario.getId()))
                .claim("tipo", TIPO_ACCESS)
                .claim("nome", usuario.getNome())
                .claim("cpf", usuario.getCpf())
                .claim("perfil_ativo", vinculo.getPerfil().getValor())
                .claim("condominio_ativo", vinculo.getCondominioId())
                .claim("condominio_nome", vinculo.getCondominioNome())
                .claim("unidade_ativa", vinculo.getUnidadeId())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(minutosToken * 60)))
                .signWith(chave)
                .compact();
    }

    public String gerarRefreshToken(Usuario usuario, VinculoPerfil vinculo) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(usuario.getId()))
                .claim("tipo", TIPO_REFRESH)
                .claim("perfil_ativo", vinculo.getPerfil().getValor())
                .claim("condominio_ativo", vinculo.getCondominioId())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusSeconds(diasRefresh * 24 * 60 * 60)))
                .signWith(chave)
                .compact();
    }

    /** Quantos segundos o access token ainda vale (campo expiresIn do contrato). */
    public long getSegundosDeValidade() {
        return minutosToken * 60;
    }

    /** Le e confere a assinatura. Lanca 401 se o token for invalido ou estiver expirado. */
    public Claims lerClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException excecao) {
            throw new CredenciaisInvalidasException("Token invalido ou expirado.");
        }
    }

    /** Monta o objeto do usuario logado a partir das claims de um access token. */
    public UsuarioAutenticado paraUsuarioAutenticado(Claims claims) {
        return new UsuarioAutenticado(
                Long.valueOf(claims.getSubject()),
                claims.get("nome", String.class),
                claims.get("cpf", String.class),
                Perfil.de(claims.get("perfil_ativo", String.class)),
                numero(claims, "condominio_ativo"),
                claims.get("condominio_nome", String.class),
                numero(claims, "unidade_ativa"),
                claims.getId());
    }

    private Long numero(Claims claims, String nome) {
        Number valor = claims.get(nome, Number.class);
        return valor == null ? null : valor.longValue();
    }
}
