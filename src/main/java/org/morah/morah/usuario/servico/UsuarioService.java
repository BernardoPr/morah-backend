package org.morah.morah.usuario.servico;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.servico.ServicoCrudTemplate;
import org.morah.morah.notificacao.template.NotificacaoDeBoasVindas;
import org.morah.morah.usuario.dto.UsuarioCreateRequest;
import org.morah.morah.usuario.dto.UsuarioResponse;
import org.morah.morah.usuario.dto.VinculoPerfilRequest;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Service de usuarios - o exemplo principal do projeto.
 *
 * <p>Ele reaproveita o TEMPLATE METHOD {@link ServicoCrudTemplate} e, nos hooks,
 * coloca as duas unicas particularidades do cadastro de usuario:
 * <ul>
 *   <li>{@code validarCriacao}: CPF e e-mail nao podem se repetir;</li>
 *   <li>{@code depoisDeSalvar}: envia a notificacao de boas-vindas.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UsuarioService extends ServicoCrudTemplate<Usuario, UsuarioCreateRequest, UsuarioResponse> {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder codificadorDeSenha;
    private final NotificacaoDeBoasVindas notificacaoDeBoasVindas;

    // ---------- passos obrigatorios do template ----------

    @Override
    protected MongoRepository<Usuario, Long> repositorio() {
        return usuarioRepository;
    }

    @Override
    protected String nomeDoRecurso() {
        return "Usuario";
    }

    @Override
    protected Usuario converterParaEntidade(UsuarioCreateRequest requisicao) {
        Usuario usuario = new Usuario();
        usuario.setNome(requisicao.nome());
        usuario.setCpf(requisicao.cpf());
        usuario.setEmail(requisicao.email());
        usuario.setTelefone(requisicao.telefone());
        // A senha vira hash BCrypt; o texto digitado nunca e gravado.
        usuario.setSenhaHash(codificadorDeSenha.encode(requisicao.senha()));
        usuario.setAtivo(true);
        usuario.setVinculos(requisicao.vinculos().stream().map(VinculoPerfilRequest::paraModelo).toList());
        return usuario;
    }

    @Override
    protected UsuarioResponse converterParaResposta(Usuario usuario) {
        return UsuarioResponse.de(usuario);
    }

    // ---------- hooks ----------

    @Override
    protected void validarCriacao(UsuarioCreateRequest requisicao) {
        if (usuarioRepository.existsByCpf(requisicao.cpf())) {
            throw new RegraDeNegocioException("Ja existe um usuario com esse CPF.");
        }
        if (usuarioRepository.existsByEmail(requisicao.email())) {
            throw new RegraDeNegocioException("Ja existe um usuario com esse e-mail.");
        }
    }

    @Override
    protected void depoisDeSalvar(Usuario usuario) {
        String condominio = usuario.vinculoPadrao()
                .map(vinculo -> vinculo.getCondominioNome())
                .orElse("condominio");

        notificacaoDeBoasVindas.enviar(usuario, condominio);
    }

    // ---------- operacao especifica ----------

    /** Desativa o acesso sem apagar o historico (mais seguro que remover o documento). */
    public UsuarioResponse desativar(Long id) {
        Usuario usuario = buscarEntidade(id);
        usuario.setAtivo(false);
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }
}
