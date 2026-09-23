package org.morah.morah.login.servico;

import java.util.List;

import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.morah.morah.login.dto.ContextoAtivo;
import org.morah.morah.login.dto.ContextoAtivoResponse;
import org.morah.morah.login.dto.LoginRequest;
import org.morah.morah.login.dto.MeResponse;
import org.morah.morah.login.dto.PerfilDisponivel;
import org.morah.morah.login.dto.RefreshRequest;
import org.morah.morah.login.dto.SelecionarContextoRequest;
import org.morah.morah.login.template.AutenticacaoPorRefreshToken;
import org.morah.morah.login.template.AutenticacaoPorSenha;
import org.morah.morah.seguranca.jwt.ServicoJwt;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.seguranca.singleton.RegistroDeTokensRevogadosSingleton;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Servico de login/sessao.
 *
 * <p>Ele nao implementa a autenticacao em si: apenas escolhe qual
 * {@code AutenticacaoTemplate} usar (senha ou refresh token) e cuida do contexto ativo.
 */
@Service
@RequiredArgsConstructor
public class LoginService {

    private final AutenticacaoPorSenha autenticacaoPorSenha;
    private final AutenticacaoPorRefreshToken autenticacaoPorRefreshToken;
    private final UsuarioRepository usuarioRepository;
    private final ServicoJwt servicoJwt;

    /** POST /auth/login */
    public ContextoAtivoResponse entrar(LoginRequest requisicao) {
        return autenticacaoPorSenha.autenticar(requisicao);
    }

    /** POST /auth/refresh */
    public ContextoAtivoResponse renovar(RefreshRequest requisicao) {
        return autenticacaoPorRefreshToken.autenticar(requisicao);
    }

    /** GET /auth/me */
    public MeResponse consultarSessao(UsuarioAutenticado logado) {
        Usuario usuario = buscarUsuario(logado);

        List<PerfilDisponivel> perfis = usuario.getVinculos().stream()
                .map(PerfilDisponivel::de)
                .toList();

        var pessoa = new MeResponse.Pessoa(
                usuario.getId(), usuario.getNome(), usuario.getCpf(), usuario.getEmail());

        var contexto = new ContextoAtivo(
                logado.perfil(), logado.condominioId(), logado.condominioNome(), logado.unidadeId());

        return new MeResponse(pessoa, perfis, contexto);
    }

    /** POST /auth/contexto: troca o condominio/perfil ativo e devolve um token novo. */
    public ContextoAtivoResponse selecionarContexto(UsuarioAutenticado logado,
                                                    SelecionarContextoRequest requisicao) {
        Usuario usuario = buscarUsuario(logado);

        VinculoPerfil vinculo = usuario.buscarVinculo(requisicao.condominioId(), requisicao.perfil())
                .orElseThrow(() -> new AcessoNegadoException(
                        "Voce nao possui o perfil " + requisicao.perfil().getValor() + " nesse condominio."));

        return new ContextoAtivoResponse(
                servicoJwt.gerarAccessToken(usuario, vinculo),
                servicoJwt.gerarRefreshToken(usuario, vinculo),
                servicoJwt.getSegundosDeValidade(),
                ContextoAtivo.de(vinculo));
    }

    /**
     * POST /auth/logout: coloca o token atual na lista de revogados
     * (o SINGLETON {@link RegistroDeTokensRevogadosSingleton}).
     */
    public void sair(UsuarioAutenticado logado) {
        RegistroDeTokensRevogadosSingleton.getInstancia().revogar(logado.idDoToken());
    }

    private Usuario buscarUsuario(UsuarioAutenticado logado) {
        return usuarioRepository.findById(logado.id())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuario do token nao existe mais."));
    }
}
