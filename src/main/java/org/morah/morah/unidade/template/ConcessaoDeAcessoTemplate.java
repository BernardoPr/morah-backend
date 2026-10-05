package org.morah.morah.unidade.template;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;

/**
 * PADRAO DE PROJETO: TEMPLATE METHOD (exemplo 4 de 4) - concessao de acesso a uma unidade.
 *
 * <p><b>Problema:</b> existem duas formas de uma pessoa passar a fazer parte de uma unidade:
 * <ul>
 *   <li>o sindico <i>aprova uma solicitacao de vinculo</i> feita por um morador (ex.: dependente);</li>
 *   <li>o proprietario <i>cadastra um inquilino</i>.</li>
 * </ul>
 * As duas seguem o mesmo roteiro - montar o vinculo, conferir se a pessoa ja nao esta vinculada,
 * localizar (ou criar) o usuario, gravar o vinculo, liberar o perfil MORADOR no app e notificar.
 * Escritas separadamente, era questao de tempo uma delas esquecer um passo (ex.: gravar o vinculo
 * da unidade mas nao liberar o {@code VinculoPerfil} - a pessoa aparece na lista e nao consegue
 * entrar no app; ou nao reativar um usuario que estava desativado).
 *
 * <p><b>Solucao:</b> {@link #conceder} e {@code final} e guarda o roteiro. As filhas so preenchem
 * o que muda: de onde vem os dados do vinculo, como a pessoa e localizada e quem e avisado.
 * Os passos "conferir duplicidade", "gravar" e "liberar o acesso no app" sao comuns e ficam aqui,
 * escritos uma unica vez.
 *
 * <p><b>Quem usa:</b> {@link ConcessaoPorSolicitacaoAprovada} (chamada pelo
 * {@code SolicitacaoVinculoService}) e {@link ConcessaoPorCadastroDeInquilino} (chamada pelo
 * {@code VinculoUnidadeService}).
 *
 * <p>Mesmo formato dos outros Template Methods do projeto ({@code AutenticacaoTemplate},
 * {@code NotificacaoTemplate}): metodo principal {@code final}, passos obrigatorios
 * {@code abstract} e passos opcionais (<i>hooks</i>) com uma implementacao padrao.
 *
 * @param <E> dados de entrada do fluxo (a SolicitacaoVinculo aprovada, o InquilinoCreateRequest...)
 */
public abstract class ConcessaoDeAcessoTemplate<E> {

    protected final VinculoUnidadeRepository vinculoUnidadeRepository;
    protected final UsuarioRepository usuarioRepository;

    protected ConcessaoDeAcessoTemplate(VinculoUnidadeRepository vinculoUnidadeRepository,
                                        UsuarioRepository usuarioRepository) {
        this.vinculoUnidadeRepository = vinculoUnidadeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** TEMPLATE METHOD: o roteiro para colocar uma pessoa na unidade, igual para os dois fluxos. */
    public final VinculoUnidade conceder(DestinoDoAcesso destino, E dados) {
        VinculoUnidade vinculo = montarVinculo(destino.unidade(), dados); // passo obrigatorio
        garantirQueAindaNaoEstaVinculada(vinculo);                        // passo comum

        PessoaLocalizada pessoa = localizarPessoa(vinculo, dados);        // hook
        associarPessoa(vinculo, pessoa);                                  // passo comum

        VinculoUnidade salvo = vinculoUnidadeRepository.save(vinculo);    // passo comum
        if (pessoa.temAcessoAoApp()) {
            liberarAcessoNoApp(pessoa.usuario(), destino);                // passo comum
        }

        notificar(destino, pessoa, salvo, dados);                         // passo obrigatorio
        return salvo;
    }

    // ---------- passos obrigatorios ----------

    /**
     * Monta (sem gravar) o vinculo da unidade com os dados da pessoa, o tipo e o periodo.
     * O CPF deve chegar aqui ja normalizado (so digitos).
     */
    protected abstract VinculoUnidade montarVinculo(Unidade unidade, E dados);

    /** Avisa quem precisa saber (quem pediu o vinculo, o proprio inquilino...). */
    protected abstract void notificar(DestinoDoAcesso destino, PessoaLocalizada pessoa,
                                      VinculoUnidade vinculo, E dados);

    // ---------- hook ----------

    /**
     * Procura o usuario do app pelo CPF. Padrao: se nao existir, a pessoa entra so no cadastro
     * da unidade, sem acesso ao app (ex.: dependente que nao usa o aplicativo).
     * O cadastro de inquilino sobrescreve para criar o usuario.
     */
    protected PessoaLocalizada localizarPessoa(VinculoUnidade vinculo, E dados) {
        if (vinculo.getPessoaCpf() == null) {
            return PessoaLocalizada.semAcessoAoApp();
        }
        return usuarioRepository.findByCpf(vinculo.getPessoaCpf())
                .map(PessoaLocalizada::existente)
                .orElseGet(PessoaLocalizada::semAcessoAoApp);
    }

    // ---------- passos comuns ----------

    /** A mesma pessoa (CPF) nao pode ter dois vinculos vigentes do mesmo tipo na unidade. */
    private void garantirQueAindaNaoEstaVinculada(VinculoUnidade novo) {
        if (novo.getPessoaCpf() == null) {
            return; // sem CPF nao ha como identificar a pessoa com seguranca
        }
        LocalDate hoje = Datas.hoje();
        boolean jaVinculada = vinculoUnidadeRepository
                .findByUnidadeIdAndCondominioIdAndAtivoTrue(novo.getUnidadeId(), novo.getCondominioId())
                .stream()
                .anyMatch(existente -> existente.estaVigenteEm(hoje)
                        && existente.ehDaPessoaComTipo(novo.getPessoaCpf(), novo.getTipoVinculo()));

        if (jaVinculada) {
            throw new RegraDeNegocioException("%s ja possui um vinculo ativo de %s nesta unidade."
                    .formatted(novo.getPessoaNome(), novo.getTipoVinculo().getValor()));
        }
    }

    /** Liga o vinculo ao usuario encontrado; o telefone do cadastro completa o que faltar. */
    private void associarPessoa(VinculoUnidade vinculo, PessoaLocalizada pessoa) {
        if (!pessoa.temAcessoAoApp()) {
            return;
        }
        vinculo.setPessoaId(pessoa.usuario().getId());
        if (vinculo.getPessoaTelefone() == null) {
            vinculo.setPessoaTelefone(pessoa.usuario().getTelefone());
        }
    }

    /**
     * Da a pessoa o perfil MORADOR da unidade (o {@code VinculoPerfil} que o login consulta) e
     * reativa o usuario, se ele estava desativado - por exemplo, um ex-inquilino que perdeu todos
     * os vinculos e agora volta para o condominio.
     *
     * <p>Se ele ja tem acesso a unidade (como morador ou proprietario), nada e duplicado.
     */
    private void liberarAcessoNoApp(Usuario usuario, DestinoDoAcesso destino) {
        Unidade unidade = destino.unidade();
        boolean alterado = false;

        if (!temAcessoAUnidade(usuario, unidade)) {
            List<VinculoPerfil> vinculos = usuario.getVinculos() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(usuario.getVinculos()); // copia: a lista original pode ser imutavel
            vinculos.add(new VinculoPerfil(Perfil.MORADOR, unidade.getCondominioId(),
                    destino.nomeDoCondominio(), unidade.getId(), unidade.getIdentificacao()));
            usuario.setVinculos(vinculos);
            alterado = true;
        }
        if (!usuario.isAtivo()) {
            usuario.setAtivo(true);
            alterado = true;
        }

        if (alterado) {
            usuarioRepository.save(usuario);
        }
    }

    private boolean temAcessoAUnidade(Usuario usuario, Unidade unidade) {
        return usuario.getVinculos() != null && usuario.getVinculos().stream()
                .anyMatch(vinculo -> unidade.getId().equals(vinculo.getUnidadeId())
                        && unidade.getCondominioId().equals(vinculo.getCondominioId())
                        && (vinculo.getPerfil() == Perfil.MORADOR || vinculo.getPerfil() == Perfil.PROPRIETARIO));
    }
}
