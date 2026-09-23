package org.morah.morah.comum.servico;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * PADRAO DE PROJETO: TEMPLATE METHOD (exemplo 1 de 3).
 *
 * <p><b>Problema:</b> todo CRUD da API repete os mesmos passos (validar -> converter ->
 * salvar -> responder). Copiar isso em cada service gera codigo duplicado.
 *
 * <p><b>Solucao:</b> esta classe abstrata fixa o <i>esqueleto</i> das operacoes nos metodos
 * {@code final} ({@link #criar}, {@link #listar}, {@link #buscarPorId}, {@link #remover}).
 * As filhas nao mudam a ordem dos passos: elas apenas preenchem os passos obrigatorios
 * ({@code abstract}) e, se quiserem, os passos opcionais ({@code protected} com corpo vazio,
 * chamados de <i>hooks</i>).
 *
 * <p><b>Quem usa:</b> {@code UsuarioService} e {@code AvisoService}.
 *
 * @param <T> entidade do banco (ex.: Aviso)
 * @param <C> DTO de entrada, o que o front envia (ex.: AvisoCreateRequest)
 * @param <R> DTO de saida, o que a API devolve (ex.: AvisoResponse)
 */
public abstract class ServicoCrudTemplate<T extends EntidadeBase, C, R> {

    // ---------- passos OBRIGATORIOS (cada service concreto implementa) ----------

    protected abstract MongoRepository<T, Long> repositorio();

    /** Nome amigavel do recurso, usado nas mensagens de erro (ex.: "Aviso"). */
    protected abstract String nomeDoRecurso();

    protected abstract T converterParaEntidade(C requisicao);

    protected abstract R converterParaResposta(T entidade);

    // ---------- TEMPLATE METHODS (o esqueleto; por isso sao final) ----------

    public final R criar(C requisicao) {
        validarCriacao(requisicao);                       // passo opcional
        T entidade = converterParaEntidade(requisicao);   // passo obrigatorio
        antesDeSalvar(entidade);                          // passo opcional
        T salva = repositorio().save(entidade);           // passo comum a todos
        depoisDeSalvar(salva);                            // passo opcional
        return converterParaResposta(salva);              // passo obrigatorio
    }

    public final PaginaResponse<R> listar(Pageable paginacao) {
        return PaginaResponse.de(repositorio().findAll(paginacao), this::converterParaResposta);
    }

    public final R buscarPorId(Long id) {
        return converterParaResposta(buscarEntidade(id));
    }

    public final void remover(Long id) {
        T entidade = buscarEntidade(id);
        antesDeRemover(entidade);
        repositorio().delete(entidade);
    }

    // ---------- apoio ----------

    /** Busca a entidade ou lanca 404. Disponivel para os services filhos. */
    protected T buscarEntidade(Long id) {
        return repositorio().findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(nomeDoRecurso(), id));
    }

    // ---------- HOOKS: passos opcionais, sobrescreva so se precisar ----------

    /** Regras de negocio antes de criar (ex.: checar CPF ja cadastrado). */
    protected void validarCriacao(C requisicao) {
        // por padrao, nao faz nada
    }

    /** Ajustes na entidade antes de gravar (ex.: definir data de publicacao). */
    protected void antesDeSalvar(T entidade) {
        // por padrao, nao faz nada
    }

    /** Efeitos colaterais depois de gravar (ex.: enviar notificacao). */
    protected void depoisDeSalvar(T entidade) {
        // por padrao, nao faz nada
    }

    /** Checagens antes de remover (ex.: nao remover registro em uso). */
    protected void antesDeRemover(T entidade) {
        // por padrao, nao faz nada
    }
}
