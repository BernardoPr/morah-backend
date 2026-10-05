package org.morah.morah.unidade.template;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.notificacao.template.NotificacaoDeConviteDeInquilino;
import org.morah.morah.unidade.dto.InquilinoCreateRequest;
import org.morah.morah.unidade.modelo.Cpf;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.unidade.servico.GeradorDeSenhaTemporaria;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Concessao de acesso quando o PROPRIETARIO cadastra um inquilino (POST /unidades/minha/inquilinos).
 *
 * <p>Preenche os passos variaveis do {@link ConcessaoDeAcessoTemplate}:
 * <ul>
 *   <li>o vinculo e do tipo INQUILINO, com o periodo do contrato de locacao;</li>
 *   <li>o hook {@link #localizarPessoa} e sobrescrito: se o CPF ainda nao tem usuario, ele e
 *       criado aqui com uma senha temporaria aleatoria (so o hash BCrypt vai para o banco);</li>
 *   <li>o proprio inquilino recebe o convite de acesso por e-mail.</li>
 * </ul>
 */
@Component
public class ConcessaoPorCadastroDeInquilino extends ConcessaoDeAcessoTemplate<InquilinoCreateRequest> {

    private final PasswordEncoder codificadorDeSenha;
    private final GeradorDeSenhaTemporaria geradorDeSenha;
    private final NotificacaoDeConviteDeInquilino notificacaoDeConvite;

    public ConcessaoPorCadastroDeInquilino(VinculoUnidadeRepository vinculoUnidadeRepository,
                                           UsuarioRepository usuarioRepository,
                                           PasswordEncoder codificadorDeSenha,
                                           GeradorDeSenhaTemporaria geradorDeSenha,
                                           NotificacaoDeConviteDeInquilino notificacaoDeConvite) {
        super(vinculoUnidadeRepository, usuarioRepository);
        this.codificadorDeSenha = codificadorDeSenha;
        this.geradorDeSenha = geradorDeSenha;
        this.notificacaoDeConvite = notificacaoDeConvite;
    }

    @Override
    protected VinculoUnidade montarVinculo(Unidade unidade, InquilinoCreateRequest requisicao) {
        VinculoUnidade vinculo = new VinculoUnidade();
        vinculo.setCondominioId(unidade.getCondominioId());
        vinculo.setUnidadeId(unidade.getId());
        vinculo.setPessoaNome(requisicao.nome().trim());
        vinculo.setPessoaCpf(Cpf.normalizar(requisicao.cpf()));
        vinculo.setPessoaTelefone(requisicao.telefone());
        vinculo.setTipoVinculo(TipoVinculo.INQUILINO);
        vinculo.setPrincipal(false);
        vinculo.setInicio(requisicao.contratoInicio());
        vinculo.setFim(requisicao.contratoFim());
        vinculo.setAtivo(true);
        return vinculo;
    }

    /** Hook sobrescrito: inquilino sem cadastro ganha um usuario novo (e por isso o e-mail e exigido). */
    @Override
    protected PessoaLocalizada localizarPessoa(VinculoUnidade vinculo, InquilinoCreateRequest requisicao) {
        Optional<Usuario> existente = usuarioRepository.findByCpf(vinculo.getPessoaCpf());
        if (existente.isPresent()) {
            return PessoaLocalizada.existente(existente.get());
        }
        return criarUsuario(vinculo, requisicao);
    }

    @Override
    protected void notificar(DestinoDoAcesso destino, PessoaLocalizada pessoa,
                             VinculoUnidade vinculo, InquilinoCreateRequest requisicao) {
        var convite = new NotificacaoDeConviteDeInquilino.Convite(
                vinculo.getId(),
                destino.nomeDoCondominio(),
                destino.unidade().getIdentificacao(),
                vinculo.getInicio(),
                vinculo.getFim(),
                pessoa.senhaTemporaria());

        notificacaoDeConvite.enviar(pessoa.usuario(), convite);
    }

    // ---------- apoio ----------

    private PessoaLocalizada criarUsuario(VinculoUnidade vinculo, InquilinoCreateRequest requisicao) {
        String email = normalizarEmail(requisicao.email());
        if (email == null) {
            throw new RegraDeNegocioException(
                    "Informe o e-mail do inquilino: ele ainda nao tem cadastro e o convite de acesso e enviado por e-mail.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("Ja existe um usuario com esse e-mail.");
        }

        String senhaTemporaria = geradorDeSenha.gerar();

        Usuario usuario = new Usuario();
        usuario.setNome(vinculo.getPessoaNome());
        usuario.setCpf(vinculo.getPessoaCpf());
        usuario.setEmail(email);
        usuario.setTelefone(vinculo.getPessoaTelefone());
        // A senha vira hash BCrypt; o texto so existe no convite enviado ao inquilino.
        usuario.setSenhaHash(codificadorDeSenha.encode(senhaTemporaria));
        usuario.setAtivo(true);
        // O perfil MORADOR da unidade e acrescentado pelo passo comum "liberar acesso no app".
        usuario.setVinculos(new ArrayList<>());

        return PessoaLocalizada.criadaAgora(usuarioRepository.save(usuario), senhaTemporaria);
    }

    private String normalizarEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
