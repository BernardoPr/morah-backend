package org.morah.morah.unidade.servico;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.dto.InquilinoCreateRequest;
import org.morah.morah.unidade.dto.VinculoUnidadeResponse;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.unidade.template.ConcessaoPorCadastroDeInquilino;
import org.morah.morah.unidade.template.DestinoDoAcesso;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Vinculos da unidade do token: listagem e gestao de inquilinos pelo proprietario.
 *
 * <p>O cadastro do inquilino e delegado ao TEMPLATE METHOD {@link ConcessaoPorCadastroDeInquilino};
 * o encerramento faz o caminho inverso (encerra o vinculo e retira o acesso ao app).
 */
@Service
@RequiredArgsConstructor
public class VinculoUnidadeService {

    /** Principal primeiro; depois proprietarios, inquilinos e dependentes; por fim, nome. */
    private static final Comparator<VinculoUnidade> ORDEM_DA_LISTA =
            Comparator.comparing(VinculoUnidade::isPrincipal).reversed()
                    .thenComparing(VinculoUnidade::getTipoVinculo, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(VinculoUnidade::getPessoaNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final VinculoUnidadeRepository vinculoUnidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeService unidadeService;
    private final ConcessaoPorCadastroDeInquilino concessaoPorCadastroDeInquilino;

    /** GET /unidades/minha/vinculos: vinculos ativos e vigentes ({@code fim} nulo ou >= hoje). */
    public List<VinculoUnidadeResponse> listarDaMinhaUnidade(UsuarioAutenticado logado) {
        Unidade unidade = unidadeService.buscarMinhaEntidade(logado);
        LocalDate hoje = Datas.hoje();

        return vinculoUnidadeRepository
                .findByUnidadeIdAndCondominioIdAndAtivoTrue(unidade.getId(), unidade.getCondominioId())
                .stream()
                .filter(vinculo -> vinculo.estaVigenteEm(hoje))
                .sorted(ORDEM_DA_LISTA)
                .map(VinculoUnidadeResponse::de)
                .toList();
    }

    /** POST /unidades/minha/inquilinos (somente proprietario). */
    public VinculoUnidadeResponse cadastrarInquilino(UsuarioAutenticado proprietario, InquilinoCreateRequest requisicao) {
        Unidade unidade = unidadeService.buscarMinhaEntidade(proprietario);

        VinculoUnidade vinculo = concessaoPorCadastroDeInquilino.conceder(
                new DestinoDoAcesso(unidade, proprietario.condominioNome()), requisicao);

        return VinculoUnidadeResponse.de(vinculo);
    }

    /**
     * DELETE /unidades/minha/inquilinos/{vinculoId} (somente proprietario): encerra o contrato
     * antes do prazo.
     *
     * <ol>
     *   <li>o vinculo precisa existir no condominio (senao 404), ser da unidade do token (senao
     *       403), ser de INQUILINO e estar ativo (senao 409);</li>
     *   <li>ele fica {@code ativo = false} e {@code fim = hoje} - nao e apagado, vira historico;</li>
     *   <li>o {@code VinculoPerfil} MORADOR daquela unidade sai do usuario; se ele ficar sem
     *       nenhum vinculo, o usuario e desativado.</li>
     * </ol>
     *
     * <p><b>E o token que o inquilino ja tem?</b> Ele <i>nao</i> e invalidado na hora:
     * <ul>
     *   <li>o <b>access token</b> continua valendo ate expirar ({@code morah.jwt.minutos-token},
     *       60 min): o {@code FiltroAutenticacaoJwt} so confere assinatura, validade e a lista de
     *       revogados, sem ir ao banco. Tambem nao da para revoga-lo aqui, porque a lista de
     *       revogados ({@code RegistroDeTokensRevogadosSingleton}) trabalha com o id (jti) de cada
     *       token, e o servidor nao guarda quais tokens emitiu para cada usuario;</li>
     *   <li>o <b>refresh</b> ja corta o acesso: o {@code AutenticacaoPorRefreshToken} rele o
     *       usuario do banco a cada renovacao. Se ele foi desativado, o template de autenticacao
     *       responde 401 ("Usuario inativo"); se continua ativo mas sem perfil MORADOR nesse
     *       condominio, {@code escolherVinculo} responde 401 ("O vinculo do token nao existe
     *       mais"). Se ele for morador de outra unidade do mesmo condominio, o token renovado
     *       passa a apontar para essa outra unidade.</li>
     * </ul>
     * Ou seja: o acesso a unidade termina, no maximo, quando o access token atual expirar.
     */
    public void encerrarInquilino(UsuarioAutenticado proprietario, Long vinculoId) {
        Unidade unidade = unidadeService.buscarMinhaEntidade(proprietario);

        VinculoUnidade vinculo = vinculoUnidadeRepository.findByIdAndCondominioId(vinculoId, proprietario.condominioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vinculo", vinculoId));

        if (!unidade.getId().equals(vinculo.getUnidadeId())) {
            throw new AcessoNegadoException("Esse vinculo e de outra unidade.");
        }
        if (vinculo.getTipoVinculo() != TipoVinculo.INQUILINO) {
            throw new RegraDeNegocioException("Somente vinculos de inquilino podem ser encerrados por aqui.");
        }
        if (!vinculo.isAtivo()) {
            throw new RegraDeNegocioException("O contrato desse inquilino ja foi encerrado.");
        }

        LocalDate hoje = Datas.hoje();
        vinculo.setAtivo(false);
        // Se o contrato ja tinha vencido, mantem a data original; senao, termina hoje.
        if (vinculo.getFim() == null || vinculo.getFim().isAfter(hoje)) {
            vinculo.setFim(hoje);
        }
        vinculoUnidadeRepository.save(vinculo);

        retirarAcessoNoApp(vinculo);
    }

    // ---------- apoio ----------

    private void retirarAcessoNoApp(VinculoUnidade encerrado) {
        Optional<Usuario> encontrado = localizarUsuario(encerrado);
        if (encontrado.isEmpty() || continuaVinculadaAUnidade(encerrado)) {
            return;
        }

        Usuario usuario = encontrado.get();
        List<VinculoPerfil> atuais = usuario.getVinculos() == null ? List.of() : usuario.getVinculos();
        List<VinculoPerfil> restantes = atuais.stream()
                .filter(vinculo -> !ehMoradorDaUnidade(vinculo, encerrado))
                .collect(Collectors.toCollection(ArrayList::new));

        if (restantes.size() == atuais.size()) {
            return; // nao havia acesso a essa unidade para retirar
        }

        usuario.setVinculos(restantes);
        if (restantes.isEmpty()) {
            // Sem nenhum vinculo o login falharia de qualquer forma; desativar deixa isso explicito.
            usuario.setAtivo(false);
        }
        usuarioRepository.save(usuario);
    }

    private Optional<Usuario> localizarUsuario(VinculoUnidade vinculo) {
        if (vinculo.getPessoaId() != null) {
            return usuarioRepository.findById(vinculo.getPessoaId());
        }
        if (vinculo.getPessoaCpf() != null) {
            return usuarioRepository.findByCpf(vinculo.getPessoaCpf());
        }
        return Optional.empty();
    }

    /**
     * A pessoa pode ter outro vinculo vigente na mesma unidade (ex.: tambem e dependente).
     * Nesse caso o acesso ao app continua, porque ela ainda mora ali.
     */
    private boolean continuaVinculadaAUnidade(VinculoUnidade encerrado) {
        LocalDate hoje = Datas.hoje();
        return vinculoUnidadeRepository
                .findByUnidadeIdAndCondominioIdAndAtivoTrue(encerrado.getUnidadeId(), encerrado.getCondominioId())
                .stream()
                .filter(outro -> !outro.getId().equals(encerrado.getId()))
                .filter(outro -> outro.estaVigenteEm(hoje))
                .anyMatch(outro -> mesmaPessoa(outro, encerrado));
    }

    private boolean mesmaPessoa(VinculoUnidade um, VinculoUnidade outro) {
        if (um.getPessoaId() != null && um.getPessoaId().equals(outro.getPessoaId())) {
            return true;
        }
        return um.getPessoaCpf() != null && um.getPessoaCpf().equals(outro.getPessoaCpf());
    }

    private boolean ehMoradorDaUnidade(VinculoPerfil vinculo, VinculoUnidade unidade) {
        return vinculo.getPerfil() == Perfil.MORADOR
                && unidade.getUnidadeId().equals(vinculo.getUnidadeId())
                && unidade.getCondominioId().equals(vinculo.getCondominioId());
    }
}
