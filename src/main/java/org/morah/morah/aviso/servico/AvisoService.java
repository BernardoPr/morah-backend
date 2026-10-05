package org.morah.morah.aviso.servico;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.morah.morah.aviso.dto.AvisoCreateRequest;
import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.aviso.dto.AvisoUpdateRequest;
import org.morah.morah.aviso.modelo.Aviso;
import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.morah.morah.aviso.modelo.PublicoAlvo;
import org.morah.morah.aviso.repositorio.AvisoRepository;
import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.DadosInvalidosException;
import org.morah.morah.comum.erro.RecursoExpiradoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.servico.ServicoCrudTemplate;
import org.morah.morah.notificacao.template.NotificacaoDeAvisoPublicado;
import org.morah.morah.seguranca.jwt.ContextoDeSeguranca;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Exemplo de service que reaproveita o TEMPLATE METHOD {@link ServicoCrudTemplate}.
 *
 * <p>Os metodos {@code criar} e {@code remover} vem prontos da classe pai. Aqui so escrevemos
 * o que e especifico do aviso:
 * <ul>
 *   <li><b>publico alvo</b>: morador e proprietario so enxergam avisos do condominio inteiro,
 *       do proprio bloco ou da propria unidade; sindico e portaria enxergam todos;</li>
 *   <li><b>vigencia</b>: aviso com {@code expiraEm} no passado some da lista e o detalhe
 *       responde 410, como pede o contrato;</li>
 *   <li><b>leitura</b>: abrir o detalhe marca o aviso como lido para quem abriu.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AvisoService extends ServicoCrudTemplate<Aviso, AvisoCreateRequest, AvisoResponse> {

    private final AvisoRepository avisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final MongoOperations mongo;
    private final NotificacaoDeAvisoPublicado notificacaoDeAvisoPublicado;

    // ---------- passos obrigatorios do template ----------

    @Override
    protected MongoRepository<Aviso, Long> repositorio() {
        return avisoRepository;
    }

    @Override
    protected String nomeDoRecurso() {
        return "Aviso";
    }

    @Override
    protected Aviso converterParaEntidade(AvisoCreateRequest requisicao) {
        Aviso aviso = new Aviso();
        aviso.setTitulo(requisicao.titulo());
        aviso.setConteudo(requisicao.conteudo());
        aviso.setPrioridade(requisicao.prioridade() == null ? PrioridadeAviso.NORMAL : requisicao.prioridade());
        aviso.setPublicoAlvo(somenteOCampoDoTipo(requisicao.publicoAlvo()));
        aviso.setExpiraEm(requisicao.expiraEm());
        return aviso;
    }

    @Override
    protected AvisoResponse converterParaResposta(Aviso aviso) {
        return AvisoResponse.de(aviso, ContextoDeSeguranca.usuarioLogado().id());
    }

    // ---------- hooks ----------

    /** O publico alvo precisa apontar para um bloco/unidade que exista neste condominio. */
    @Override
    protected void validarCriacao(AvisoCreateRequest requisicao) {
        PublicoAlvo alvo = requisicao.publicoAlvo();
        Long condominioId = ContextoDeSeguranca.usuarioLogado().condominioId();

        switch (alvo.getTipo()) {
            case CONDOMINIO -> {
                // nada a conferir
            }
            case BLOCO -> {
                if (alvo.getBlocoId() == null) {
                    throw new DadosInvalidosException("Informe o blocoId quando o publico alvo for um bloco.");
                }
                if (unidadeRepository.findByCondominioIdAndBlocoId(condominioId, alvo.getBlocoId()).isEmpty()) {
                    throw new RecursoNaoEncontradoException("Bloco", alvo.getBlocoId());
                }
            }
            case UNIDADE -> {
                if (alvo.getUnidadeId() == null) {
                    throw new DadosInvalidosException("Informe o unidadeId quando o publico alvo for uma unidade.");
                }
                unidadeRepository.findByIdAndCondominioId(alvo.getUnidadeId(), condominioId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", alvo.getUnidadeId()));
            }
        }
    }

    /** Preenche autor, condominio e data de publicacao a partir do token do sindico. */
    @Override
    protected void antesDeSalvar(Aviso aviso) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        aviso.setAutorId(logado.id());
        aviso.setAutorNome(logado.nome());
        aviso.setCondominioId(logado.condominioId());
        aviso.setPublicadoEm(Instant.now());
    }

    /** Depois de publicar, notifica o publico alvo (Template Method + Strategy trabalhando juntos). */
    @Override
    protected void depoisDeSalvar(Aviso aviso) {
        destinatarios(aviso).stream()
                .filter(usuario -> !usuario.getId().equals(aviso.getAutorId()))
                .forEach(usuario -> notificacaoDeAvisoPublicado.enviar(usuario, aviso));
    }

    /**
     * Alem de achar o aviso, garante que ele e do condominio do token. Como o {@code remover}
     * do template usa este metodo, o sindico de outro condominio tambem nao consegue apagar.
     */
    @Override
    protected Aviso buscarEntidade(Long id) {
        Aviso aviso = super.buscarEntidade(id);
        if (!aviso.getCondominioId().equals(ContextoDeSeguranca.usuarioLogado().condominioId())) {
            throw new RecursoNaoEncontradoException(nomeDoRecurso(), id);
        }
        return aviso;
    }

    // ---------- operacoes especificas do aviso ----------

    /** GET /avisos: avisos vigentes que o usuario pode ver, mais recentes primeiro. */
    public PaginaResponse<AvisoResponse> listarVisiveis(UsuarioAutenticado usuario, PrioridadeAviso prioridade,
                                                        Pageable paginacao) {
        Query consulta = new Query(criteriosDeVisibilidade(usuario, prioridade)).with(paginacao);
        List<Aviso> avisos = mongo.find(consulta, Aviso.class);

        Page<Aviso> pagina = PageableExecutionUtils.getPage(avisos, paginacao,
                () -> mongo.count(Query.of(consulta).limit(-1).skip(-1), Aviso.class));

        return PaginaResponse.de(pagina, aviso -> AvisoResponse.de(aviso, usuario.id()));
    }

    /** GET /avisos/{id}: devolve o aviso e o marca como lido para quem abriu. */
    public AvisoResponse detalhar(Long id, UsuarioAutenticado usuario) {
        Aviso aviso = buscarEntidade(id);

        if (!visivelPara(aviso, usuario)) {
            // 404 (e nao 403) para nao revelar que existe um aviso para outra unidade.
            throw new RecursoNaoEncontradoException(nomeDoRecurso(), id);
        }
        if (aviso.expirouEm(Instant.now())) {
            throw new RecursoExpiradoException("Este aviso saiu de vigencia em " + aviso.getExpiraEm() + ".");
        }

        marcarComoLido(aviso, usuario.id());
        return AvisoResponse.de(aviso, usuario.id());
    }

    /** PATCH /avisos/{id}: altera somente os campos enviados. */
    public AvisoResponse atualizar(Long id, AvisoUpdateRequest requisicao) {
        Aviso aviso = buscarEntidade(id);

        if (requisicao.titulo() != null) {
            aviso.setTitulo(requisicao.titulo());
        }
        if (requisicao.conteudo() != null) {
            aviso.setConteudo(requisicao.conteudo());
        }
        if (requisicao.prioridade() != null) {
            aviso.setPrioridade(requisicao.prioridade());
        }
        if (requisicao.expiraEm() != null) {
            aviso.setExpiraEm(requisicao.expiraEm());
        }

        return converterParaResposta(avisoRepository.save(aviso));
    }

    // ---------- apoio ----------

    /** Morador e proprietario veem por publico alvo; sindico e portaria veem tudo do condominio. */
    private boolean filtraPorPublicoAlvo(UsuarioAutenticado usuario) {
        return usuario.unidadeId() != null;
    }

    private Criteria criteriosDeVisibilidade(UsuarioAutenticado usuario, PrioridadeAviso prioridade) {
        List<Criteria> regras = new ArrayList<>();
        regras.add(Criteria.where("condominioId").is(usuario.condominioId()));
        regras.add(new Criteria().orOperator(
                Criteria.where("expiraEm").is(null),
                Criteria.where("expiraEm").gt(Instant.now())));

        if (prioridade != null) {
            regras.add(Criteria.where("prioridade").is(prioridade));
        }
        if (filtraPorPublicoAlvo(usuario)) {
            regras.add(publicoAlvoDaUnidade(usuario.unidadeId()));
        }
        return new Criteria().andOperator(regras);
    }

    /** Condominio inteiro OU o bloco da unidade OU a propria unidade. */
    private Criteria publicoAlvoDaUnidade(Long unidadeId) {
        List<Criteria> alvos = new ArrayList<>();
        alvos.add(Criteria.where("publicoAlvo.tipo").is(PublicoAlvo.Tipo.CONDOMINIO));
        alvos.add(Criteria.where("publicoAlvo.tipo").is(PublicoAlvo.Tipo.UNIDADE)
                .and("publicoAlvo.unidadeId").is(unidadeId));

        Long blocoId = blocoDa(unidadeId);
        if (blocoId != null) {
            alvos.add(Criteria.where("publicoAlvo.tipo").is(PublicoAlvo.Tipo.BLOCO)
                    .and("publicoAlvo.blocoId").is(blocoId));
        }
        return new Criteria().orOperator(alvos);
    }

    /** A mesma regra de {@link #publicoAlvoDaUnidade}, aplicada a um aviso ja carregado. */
    private boolean visivelPara(Aviso aviso, UsuarioAutenticado usuario) {
        PublicoAlvo alvo = aviso.getPublicoAlvo();
        if (!filtraPorPublicoAlvo(usuario) || alvo == null || alvo.getTipo() == null) {
            return true;
        }
        return switch (alvo.getTipo()) {
            case CONDOMINIO -> true;
            case UNIDADE -> usuario.unidadeId().equals(alvo.getUnidadeId());
            case BLOCO -> Objects.equals(blocoDa(usuario.unidadeId()), alvo.getBlocoId());
        };
    }

    private Long blocoDa(Long unidadeId) {
        return unidadeRepository.findById(unidadeId).map(Unidade::getBlocoId).orElse(null);
    }

    /**
     * Grava a leitura com {@code $addToSet}: operacao atomica no banco, entao duas pessoas
     * abrindo o aviso ao mesmo tempo nao apagam a leitura uma da outra (o que aconteceria
     * com "carregar, adicionar na lista e salvar o documento inteiro").
     */
    private void marcarComoLido(Aviso aviso, Long usuarioId) {
        if (aviso.getLidoPor() == null) {
            aviso.setLidoPor(new HashSet<>());
        }
        if (aviso.getLidoPor().add(usuarioId)) {
            mongo.updateFirst(
                    Query.query(Criteria.where("_id").is(aviso.getId())),
                    new Update().addToSet("lidoPor", usuarioId),
                    Aviso.class);
        }
    }

    /** Guarda so o identificador que faz sentido para o tipo (ex.: aviso do condominio nao tem blocoId). */
    private PublicoAlvo somenteOCampoDoTipo(PublicoAlvo alvo) {
        return switch (alvo.getTipo()) {
            case CONDOMINIO -> new PublicoAlvo(PublicoAlvo.Tipo.CONDOMINIO, null, null);
            case BLOCO -> new PublicoAlvo(PublicoAlvo.Tipo.BLOCO, alvo.getBlocoId(), null);
            case UNIDADE -> new PublicoAlvo(PublicoAlvo.Tipo.UNIDADE, null, alvo.getUnidadeId());
        };
    }

    /** Quem recebe a notificacao, sem repetir a mesma pessoa (ex.: dona de duas unidades do bloco). */
    private Collection<Usuario> destinatarios(Aviso aviso) {
        PublicoAlvo alvo = aviso.getPublicoAlvo();

        List<Usuario> encontrados = switch (alvo.getTipo()) {
            case CONDOMINIO -> usuarioRepository.findByVinculosCondominioIdAndAtivoTrue(aviso.getCondominioId());
            case BLOCO -> unidadeRepository.findByCondominioIdAndBlocoId(aviso.getCondominioId(), alvo.getBlocoId())
                    .stream()
                    .flatMap(unidade -> usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(unidade.getId()).stream())
                    .toList();
            case UNIDADE -> usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(alvo.getUnidadeId());
        };

        Map<Long, Usuario> semRepeticao = new LinkedHashMap<>();
        encontrados.forEach(usuario -> semRepeticao.putIfAbsent(usuario.getId(), usuario));
        return semRepeticao.values();
    }
}
