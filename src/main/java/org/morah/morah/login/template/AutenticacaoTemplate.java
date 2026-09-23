package org.morah.morah.login.template;

import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.morah.morah.login.dto.ContextoAtivo;
import org.morah.morah.login.dto.ContextoAtivoResponse;
import org.morah.morah.seguranca.jwt.ServicoJwt;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;

/**
 * PADRAO DE PROJETO: TEMPLATE METHOD (exemplo 2 de 3).
 *
 * <p><b>Problema:</b> existem formas diferentes de autenticar (CPF + senha hoje; refresh token
 * para renovar a sessao; amanha biometria ou Keycloak). Todas terminam igual: conferir se o
 * usuario esta ativo, escolher o contexto e devolver os tokens. Sem um esqueleto comum, esse
 * final acabaria copiado em cada implementacao - e uma delas certamente esqueceria um passo.
 *
 * <p><b>Solucao:</b> {@link #autenticar} e final e guarda o roteiro completo.
 * Cada forma de autenticacao implementa apenas os passos que a diferenciam.
 *
 * <p><b>Implementacoes:</b> {@link AutenticacaoPorSenha} e {@link AutenticacaoPorRefreshToken}.
 *
 * @param <E> tipo do objeto de entrada (LoginRequest, RefreshRequest...)
 */
public abstract class AutenticacaoTemplate<E> {

    protected final UsuarioRepository usuarioRepository;
    protected final ServicoJwt servicoJwt;

    protected AutenticacaoTemplate(UsuarioRepository usuarioRepository, ServicoJwt servicoJwt) {
        this.usuarioRepository = usuarioRepository;
        this.servicoJwt = servicoJwt;
    }

    /** TEMPLATE METHOD: a ordem dos passos da autenticacao, igual para todas as formas. */
    public final ContextoAtivoResponse autenticar(E entrada) {
        Usuario usuario = buscarUsuario(entrada);                  // passo obrigatorio
        conferirCredencial(usuario, entrada);                      // passo obrigatorio
        garantirQueEstaAtivo(usuario);                             // passo comum
        VinculoPerfil vinculo = escolherVinculo(usuario, entrada); // passo obrigatorio
        aposAutenticar(usuario, entrada);                          // hook

        return new ContextoAtivoResponse(
                servicoJwt.gerarAccessToken(usuario, vinculo),
                servicoJwt.gerarRefreshToken(usuario, vinculo),
                servicoJwt.getSegundosDeValidade(),
                ContextoAtivo.de(vinculo));
    }

    // ---------- passos obrigatorios ----------

    protected abstract Usuario buscarUsuario(E entrada);

    /** Confere senha, assinatura do token, biometria... conforme a forma de autenticacao. */
    protected abstract void conferirCredencial(Usuario usuario, E entrada);

    /** Define qual vinculo (condominio + perfil) vai para dentro do token. */
    protected abstract VinculoPerfil escolherVinculo(Usuario usuario, E entrada);

    // ---------- passo comum ----------

    private void garantirQueEstaAtivo(Usuario usuario) {
        if (!usuario.isAtivo()) {
            throw new CredenciaisInvalidasException("Usuario inativo. Procure a administracao do condominio.");
        }
    }

    // ---------- hook ----------

    /** Ex.: registrar data do ultimo acesso, revogar o refresh token usado, auditar. */
    protected void aposAutenticar(Usuario usuario, E entrada) {
        // por padrao, nao faz nada
    }
}
