package org.morah.morah.encomenda.servico;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.servico.ServicoCrudTemplate;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.encomenda.dto.AutorizacaoRetiradaCreateRequest;
import org.morah.morah.encomenda.dto.AutorizacaoRetiradaResponse;
import org.morah.morah.encomenda.dto.EncomendaCreateRequest;
import org.morah.morah.encomenda.dto.EncomendaResponse;
import org.morah.morah.encomenda.dto.OcorrenciaCreateRequest;
import org.morah.morah.encomenda.dto.RetiradaConfirmRequest;
import org.morah.morah.encomenda.modelo.AutorizacaoRetirada;
import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.encomenda.modelo.StatusAutorizacaoRetirada;
import org.morah.morah.encomenda.modelo.StatusEncomenda;
import org.morah.morah.encomenda.repositorio.AutorizacaoRetiradaRepository;
import org.morah.morah.encomenda.repositorio.EncomendaRepository;
import org.morah.morah.encomenda.strategy.RetiradaValidada;
import org.morah.morah.encomenda.strategy.ValidacaoDeRetiradaStrategy;
import org.morah.morah.notificacao.template.NotificacaoDeEncomendaRecebida;
import org.morah.morah.notificacao.template.NotificacaoDeEncomendaRetirada;
import org.morah.morah.notificacao.template.NotificacaoTemplate;
import org.morah.morah.ocorrencia.dto.NovaOcorrencia;
import org.morah.morah.ocorrencia.dto.OcorrenciaResponse;
import org.morah.morah.ocorrencia.modelo.OrigemOcorrencia;
import org.morah.morah.ocorrencia.modelo.StatusOcorrencia;
import org.morah.morah.ocorrencia.repositorio.OcorrenciaRepository;
import org.morah.morah.ocorrencia.servico.OcorrenciaService;
import org.morah.morah.seguranca.jwt.ContextoDeSeguranca;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Regras do modulo de encomendas (secao "Encomendas" do contrato).
 *
 * <p>Padroes de projeto usados aqui:
 * <ul>
 *   <li><b>Template Method</b> ({@link ServicoCrudTemplate}): o registro do recebimento segue o
 *       roteiro validar -> converter -> antesDeSalvar -> salvar -> depoisDeSalvar; so preenchemos
 *       os passos (unidade valida, codigo gerado, unidade notificada);</li>
 *   <li><b>Strategy</b> ({@link ValidacaoDeRetiradaStrategy}): na confirmacao da retirada, cada
 *       forma de liberar o pacote (codigo do morador, autorizacao de terceiro) e uma classe;</li>
 *   <li><b>Template Method</b> de novo nas notificacoes ({@link NotificacaoTemplate}).</li>
 * </ul>
 *
 * <p>Regra de seguranca que atravessa o modulo: o codigo de retirada so aparece para o
 * morador/proprietario da propria unidade (veja {@link #paraOLeitor}).
 *
 * <p>Cuidado: os metodos herdados {@code listar} e {@code buscarPorId} do template nao filtram
 * por condominio. Os endpoints usam {@link #listarDoContextoAtivo} e {@link #detalhar}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncomendaService extends ServicoCrudTemplate<Encomenda, EncomendaCreateRequest, EncomendaResponse> {

    private static final String CODIGO_INVALIDO = "Codigo de retirada invalido ou expirado";

    private final EncomendaRepository encomendaRepository;
    private final AutorizacaoRetiradaRepository autorizacaoRetiradaRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final OcorrenciaService ocorrenciaService;
    private final GeradorDeCodigoDeRetirada geradorDeCodigo;
    private final NotificacaoDeEncomendaRecebida notificacaoDeEncomendaRecebida;
    private final NotificacaoDeEncomendaRetirada notificacaoDeEncomendaRetirada;

    /**
     * STRATEGY: todas as formas de retirada. O Spring injeta a lista com todas as classes que
     * implementam a interface, ja na ordem do {@code @Order} de cada uma.
     */
    private final List<ValidacaoDeRetiradaStrategy> validacoesDeRetirada;

    // ---------- passos obrigatorios do template (POST /encomendas) ----------

    @Override
    protected MongoRepository<Encomenda, Long> repositorio() {
        return encomendaRepository;
    }

    @Override
    protected String nomeDoRecurso() {
        return "Encomenda";
    }

    @Override
    protected Encomenda converterParaEntidade(EncomendaCreateRequest requisicao) {
        Encomenda encomenda = new Encomenda();
        encomenda.setUnidadeId(requisicao.unidadeId());
        encomenda.setRemetente(requisicao.remetente());
        encomenda.setTransportadora(requisicao.transportadora().trim());
        encomenda.setCodigoRastreio(requisicao.codigoRastreio());
        encomenda.setFotoUrl(requisicao.fotoUrl());
        return encomenda;
    }

    /** Quem registra e a portaria: a resposta do POST nunca leva o codigo de retirada. */
    @Override
    protected EncomendaResponse converterParaResposta(Encomenda encomenda) {
        return EncomendaResponse.de(encomenda, false);
    }

    // ---------- hooks do template ----------

    /** A unidade informada pelo porteiro precisa existir no condominio do token (senao 404). */
    @Override
    protected void validarCriacao(EncomendaCreateRequest requisicao) {
        Long condominioId = ContextoDeSeguranca.usuarioLogado().condominioId();
        unidadeRepository.findByIdAndCondominioId(requisicao.unidadeId(), condominioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", requisicao.unidadeId()));
    }

    /** Preenche o que vem do token e gera o codigo de retirada do morador. */
    @Override
    protected void antesDeSalvar(Encomenda encomenda) {
        UsuarioAutenticado porteiro = ContextoDeSeguranca.usuarioLogado();
        encomenda.setCondominioId(porteiro.condominioId());
        encomenda.setRecebidaEm(Instant.now());
        encomenda.setRecebidaPorId(porteiro.id());
        encomenda.setRecebidaPorNome(porteiro.nome());
        encomenda.setStatus(StatusEncomenda.AGUARDANDO_RETIRADA);
        encomenda.setCodigoRetirada(geradorDeCodigo.gerar());
    }

    /** O codigo chega ao morador pela notificacao - e o unico caminho, ja que a portaria nao o ve. */
    @Override
    protected void depoisDeSalvar(Encomenda encomenda) {
        notificarUnidade(encomenda, notificacaoDeEncomendaRecebida);
    }

    // ---------- consultas (GET /encomendas e GET /encomendas/{id}) ----------

    /**
     * Morador/proprietario ve so a unidade do token; portaria e sindico veem o condominio todo.
     * A ordem (mais recentes primeiro) vem no {@code Pageable} montado pelo controller.
     */
    public PaginaResponse<EncomendaResponse> listarDoContextoAtivo(StatusEncomenda status, Pageable paginacao) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        Long condominioId = logado.condominioId();

        Page<Encomenda> pagina;
        if (ehMoradorOuProprietario(logado)) {
            Long unidadeId = unidadeDoToken(logado);
            pagina = status == null
                    ? encomendaRepository.findByCondominioIdAndUnidadeId(condominioId, unidadeId, paginacao)
                    : encomendaRepository.findByCondominioIdAndUnidadeIdAndStatus(condominioId, unidadeId, status, paginacao);
        } else {
            pagina = status == null
                    ? encomendaRepository.findByCondominioId(condominioId, paginacao)
                    : encomendaRepository.findByCondominioIdAndStatus(condominioId, status, paginacao);
        }

        return PaginaResponse.de(pagina, encomenda -> paraOLeitor(encomenda, logado));
    }

    /** Outro condominio -> 404; morador de outra unidade do mesmo condominio -> 403. */
    public EncomendaResponse detalhar(Long encomendaId) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        Encomenda encomenda = buscarNoCondominio(encomendaId, logado);

        if (ehMoradorOuProprietario(logado)) {
            conferirUnidade(encomenda, logado);
        }
        return paraOLeitor(encomenda, logado);
    }

    // ---------- autorizacao de terceiro (POST /encomendas/{id}/autorizacoes-retirada) ----------

    /**
     * O morador gera um codigo proprio para outra pessoa retirar. O codigo e sempre diferente do
     * codigo do morador e das outras autorizacoes ativas, para a portaria saber qual foi usada.
     */
    public AutorizacaoRetiradaResponse autorizarRetiradaDeTerceiro(Long encomendaId,
                                                                   AutorizacaoRetiradaCreateRequest requisicao) {
        UsuarioAutenticado morador = ContextoDeSeguranca.usuarioLogado();
        Encomenda encomenda = buscarDaPropriaUnidade(encomendaId, morador);
        exigirAguardandoRetirada(encomenda);

        Set<String> codigosEmUso = new HashSet<>();
        codigosEmUso.add(encomenda.getCodigoRetirada());
        autorizacaoRetiradaRepository
                .findByEncomendaIdAndStatus(encomenda.getId(), StatusAutorizacaoRetirada.ATIVA)
                .forEach(ativa -> codigosEmUso.add(ativa.getCodigo()));

        Instant agora = Instant.now();
        AutorizacaoRetirada autorizacao = new AutorizacaoRetirada();
        autorizacao.setCondominioId(encomenda.getCondominioId());
        autorizacao.setUnidadeId(encomenda.getUnidadeId());
        autorizacao.setEncomendaId(encomenda.getId());
        autorizacao.setNomeTerceiro(requisicao.nomeTerceiro().trim());
        autorizacao.setDocumento(requisicao.documento().trim());
        autorizacao.setCodigo(geradorDeCodigo.gerarDiferenteDe(codigosEmUso));
        autorizacao.setValidade(agora.plus(requisicao.validadeEmHoras(), ChronoUnit.HOURS));
        autorizacao.setStatus(StatusAutorizacaoRetirada.ATIVA);
        autorizacao.setCriadaPorId(morador.id());

        return AutorizacaoRetiradaResponse.de(autorizacaoRetiradaRepository.save(autorizacao), agora);
    }

    // ---------- baixa na portaria (POST /encomendas/{id}/retirada) ----------

    /**
     * Confirma a retirada. Aqui o STRATEGY trabalha: o service nao sabe de quem e o codigo;
     * pergunta a cada {@link ValidacaoDeRetiradaStrategy} e a primeira que aceitar decide.
     */
    public EncomendaResponse confirmarRetirada(Long encomendaId, RetiradaConfirmRequest requisicao) {
        UsuarioAutenticado porteiro = ContextoDeSeguranca.usuarioLogado();
        Encomenda encomenda = buscarNoCondominio(encomendaId, porteiro);
        exigirAguardandoRetirada(encomenda);

        Instant agora = Instant.now();
        CodigoAceito aceito = escolherFormaDeRetirada(encomenda, requisicao.codigo().trim(), agora);
        RetiradaValidada retirada = aceito.retirada();

        encomenda.setStatus(StatusEncomenda.RETIRADA);
        encomenda.setRetiradaEm(agora);
        // O que o porteiro digitou vale mais; se nao digitou, usamos o que a forma de retirada sabe.
        encomenda.setRetiradaPorNome(primeiroPreenchido(requisicao.retiradoPorNome(), retirada.nome()));
        encomenda.setRetiradaPorDocumento(primeiroPreenchido(requisicao.retiradoPorDocumento(), retirada.documento()));
        encomenda.setAutorizacaoRetiradaId(retirada.autorizacaoId());
        encomenda.setEntreguePorId(porteiro.id());
        encomenda.setEntreguePorNome(porteiro.nome());
        Encomenda salva = encomendaRepository.save(encomenda);

        aceito.estrategia().registrarUso(salva, retirada, agora); // hook da forma escolhida
        invalidarAutorizacoesRestantes(salva.getId(), retirada.autorizacaoId());
        notificarUnidade(salva, notificacaoDeEncomendaRetirada);

        log.info("Encomenda {} retirada por {} ({}), entregue por {}",
                salva.getId(), salva.getRetiradaPorNome(), retirada.forma(), porteiro.nome());
        return EncomendaResponse.de(salva, false);
    }

    /** Percorre as estrategias na ordem; nenhuma aceitou -> 409. */
    private CodigoAceito escolherFormaDeRetirada(Encomenda encomenda, String codigo, Instant agora) {
        for (ValidacaoDeRetiradaStrategy estrategia : validacoesDeRetirada) {
            Optional<RetiradaValidada> resultado = estrategia.validar(encomenda, codigo, agora);
            if (resultado.isPresent()) {
                return new CodigoAceito(estrategia, resultado.get());
            }
        }
        throw new RegraDeNegocioException(CODIGO_INVALIDO);
    }

    /** Depois da retirada, nenhum outro codigo de terceiro pode continuar valendo. */
    private void invalidarAutorizacoesRestantes(Long encomendaId, Long autorizacaoUsadaId) {
        List<AutorizacaoRetirada> restantes = autorizacaoRetiradaRepository
                .findByEncomendaIdAndStatus(encomendaId, StatusAutorizacaoRetirada.ATIVA)
                .stream()
                .filter(autorizacao -> !Objects.equals(autorizacao.getId(), autorizacaoUsadaId))
                .toList();

        if (!restantes.isEmpty()) {
            restantes.forEach(autorizacao -> autorizacao.setStatus(StatusAutorizacaoRetirada.EXPIRADA));
            autorizacaoRetiradaRepository.saveAll(restantes);
        }
    }

    /** Guarda qual estrategia aceitou, para chamar o hook {@code registrarUso} dela depois. */
    private record CodigoAceito(ValidacaoDeRetiradaStrategy estrategia, RetiradaValidada retirada) {
    }

    // ---------- extravio (POST /encomendas/{id}/ocorrencias) ----------

    /**
     * Abre uma ocorrencia vinculada a encomenda (o {@link OcorrenciaService} avisa os sindicos).
     *
     * <ul>
     *   <li>Aguardando retirada -> a encomenda passa a EXTRAVIADA (sumiu da portaria).</li>
     *   <li>Ja RETIRADA -> a ocorrencia e aceita como contestacao ("nao fui eu quem retirou",
     *       "veio violada"), mas o status continua RETIRADA: a entrega aconteceu e esta registrada.</li>
     *   <li>Ja EXTRAVIADA -> so e possivel abrir outra se a anterior foi encerrada.</li>
     * </ul>
     * Uma ocorrencia ainda aberta (ou em andamento) para a mesma encomenda -> 409.
     */
    public OcorrenciaResponse abrirOcorrencia(Long encomendaId, OcorrenciaCreateRequest requisicao) {
        UsuarioAutenticado morador = ContextoDeSeguranca.usuarioLogado();
        Encomenda encomenda = buscarDaPropriaUnidade(encomendaId, morador);

        boolean jaTemOcorrenciaAberta = ocorrenciaRepository
                .findByOrigemAndReferenciaId(OrigemOcorrencia.ENCOMENDA, encomenda.getId())
                .stream()
                .anyMatch(ocorrencia -> ocorrencia.getStatus() != StatusOcorrencia.ENCERRADA);
        if (jaTemOcorrenciaAberta) {
            throw new RegraDeNegocioException("Ja existe uma ocorrencia aberta para essa encomenda.");
        }

        OcorrenciaResponse ocorrencia = ocorrenciaService.abrir(new NovaOcorrencia(
                encomenda.getCondominioId(),
                encomenda.getUnidadeId(),
                OrigemOcorrencia.ENCOMENDA,
                encomenda.getId(),
                requisicao.titulo(),
                requisicao.descricao(),
                morador.id(),
                morador.nome()));

        if (encomenda.getStatus() == StatusEncomenda.AGUARDANDO_RETIRADA) {
            encomenda.setStatus(StatusEncomenda.EXTRAVIADA);
        }
        encomenda.setOcorrenciaId(ocorrencia.id());
        encomendaRepository.save(encomenda);

        return ocorrencia;
    }

    // ---------- dashboard (sem contexto de seguranca: tudo vem por parametro) ----------

    /**
     * Encomendas aguardando retirada da unidade, mais recentes primeiro, COM o codigo de
     * retirada (a tela inicial e do proprio morador).
     */
    public List<EncomendaResponse> pendentesDaUnidade(Long unidadeId, int limite) {
        if (limite <= 0) {
            return List.of();
        }
        var paginacao = PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "recebidaEm"));
        return encomendaRepository
                .findByUnidadeIdAndStatus(unidadeId, StatusEncomenda.AGUARDANDO_RETIRADA, paginacao)
                .stream()
                .map(encomenda -> EncomendaResponse.de(encomenda, true))
                .toList();
    }

    /** Encomendas que chegaram hoje (fuso do condominio) - tela inicial da portaria. */
    public long contarRecebidasHoje(Long condominioId) {
        LocalDate hoje = Datas.hoje();
        return encomendaRepository.contarRecebidasEntre(condominioId, Datas.inicioDoDia(hoje), Datas.fimDoDia(hoje));
    }

    // ---------- apoio ----------

    private static boolean ehMoradorOuProprietario(UsuarioAutenticado usuario) {
        return usuario.perfil() == Perfil.MORADOR || usuario.perfil() == Perfil.PROPRIETARIO;
    }

    /** Regra do codigo: so o morador/proprietario da propria unidade o recebe na resposta. */
    private static EncomendaResponse paraOLeitor(Encomenda encomenda, UsuarioAutenticado leitor) {
        boolean daPropriaUnidade = ehMoradorOuProprietario(leitor)
                && Objects.equals(encomenda.getUnidadeId(), leitor.unidadeId());
        return EncomendaResponse.de(encomenda, daPropriaUnidade);
    }

    private static Long unidadeDoToken(UsuarioAutenticado morador) {
        if (morador.unidadeId() == null) {
            throw new AcessoNegadoException("O contexto ativo nao tem uma unidade.");
        }
        return morador.unidadeId();
    }

    private Encomenda buscarNoCondominio(Long encomendaId, UsuarioAutenticado logado) {
        return encomendaRepository.findByIdAndCondominioId(encomendaId, logado.condominioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encomenda", encomendaId));
    }

    private Encomenda buscarDaPropriaUnidade(Long encomendaId, UsuarioAutenticado morador) {
        Encomenda encomenda = buscarNoCondominio(encomendaId, morador);
        conferirUnidade(encomenda, morador);
        return encomenda;
    }

    private static void conferirUnidade(Encomenda encomenda, UsuarioAutenticado morador) {
        if (!Objects.equals(encomenda.getUnidadeId(), morador.unidadeId())) {
            throw new AcessoNegadoException("Essa encomenda e de outra unidade.");
        }
    }

    private static void exigirAguardandoRetirada(Encomenda encomenda) {
        if (encomenda.getStatus() != StatusEncomenda.AGUARDANDO_RETIRADA) {
            throw new RegraDeNegocioException(
                    "A encomenda nao esta aguardando retirada (status atual: %s)."
                            .formatted(encomenda.getStatus().getValor()));
        }
    }

    private void notificarUnidade(Encomenda encomenda, NotificacaoTemplate<Encomenda> notificacao) {
        usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(encomenda.getUnidadeId())
                .forEach(usuario -> notificacao.enviar(usuario, encomenda));
    }

    private static String primeiroPreenchido(String informado, String alternativo) {
        return informado != null && !informado.isBlank() ? informado.trim() : alternativo;
    }
}
