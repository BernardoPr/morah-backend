package org.morah.morah.portaria.servico;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.notificacao.template.NotificacaoDeDecisaoDeVisita;
import org.morah.morah.notificacao.template.NotificacaoDeVisitanteAguardando;
import org.morah.morah.portaria.dto.AutorizacaoVisitaCreateRequest;
import org.morah.morah.portaria.dto.AutorizacaoVisitaResponse;
import org.morah.morah.portaria.dto.DecisaoAutorizacaoRequest;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.DadosDoVisitante;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.morah.morah.portaria.modelo.Visitante;
import org.morah.morah.portaria.repositorio.AutorizacaoVisitaRepository;
import org.morah.morah.portaria.state.EstadoDaAutorizacao;
import org.morah.morah.portaria.state.SeletorDeEstadoDaAutorizacao;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Regras das autorizacoes de visita (registro do visitante, fila da guarita, decisao do morador,
 * reenvio e liberacao da entrada).
 *
 * <p>Repare que nenhum metodo pergunta "qual e o status?": as transicoes sao delegadas ao objeto
 * {@link EstadoDaAutorizacao} devolvido pelo {@link SeletorDeEstadoDaAutorizacao} (padrao STATE).
 * As notificacoes saem pelos templates {@code NotificacaoDe...} (TEMPLATE METHOD + STRATEGY).
 *
 * <p>Por que nao estende o {@code ServicoCrudTemplate}? O registro depende do porteiro logado e
 * cria/atualiza o visitante antes da autorizacao, e os metodos prontos {@code listar} e
 * {@code buscarPorId} do template nao filtram por condominio - herdar deixaria publicos metodos
 * que ignoram o isolamento entre condominios.
 *
 * <p>Os metodos recebem o {@link UsuarioAutenticado} por parametro (o controller pega do token):
 * fica explicito de onde vem o condominio/unidade e os testes nao precisam montar contexto de
 * seguranca.
 */
@Service
@RequiredArgsConstructor
public class AutorizacaoVisitaService {

    private static final Sort MAIS_RECENTES_PRIMEIRO = Sort.by(Sort.Direction.DESC, "solicitadoEm");

    private final AutorizacaoVisitaRepository autorizacaoRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final VisitanteService visitanteService;
    private final SeletorDeEstadoDaAutorizacao seletorDeEstado;
    private final NotificacaoDeVisitanteAguardando notificacaoDeVisitanteAguardando;
    private final NotificacaoDeDecisaoDeVisita notificacaoDeDecisaoDeVisita;

    // ---------- POST /portaria/visitantes ----------

    /** Registra o visitante, abre a autorizacao PENDENTE e avisa a unidade. */
    public AutorizacaoVisitaResponse registrarVisitante(UsuarioAutenticado porteiro,
                                                        AutorizacaoVisitaCreateRequest requisicao) {
        Long condominioId = porteiro.condominioId();
        Unidade unidade = unidadeRepository.findByIdAndCondominioId(requisicao.unidadeId(), condominioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", requisicao.unidadeId()));

        Visitante visitante = visitanteService.registrar(condominioId, requisicao.visitante());
        Instant agora = Instant.now();

        AutorizacaoVisita autorizacao = new AutorizacaoVisita();
        autorizacao.setCondominioId(condominioId);
        autorizacao.setUnidadeId(unidade.getId());
        autorizacao.setUnidadeDescricao(descrever(unidade));
        autorizacao.setVisitanteId(visitante.getId());
        autorizacao.setVisitante(DadosDoVisitante.de(visitante));
        autorizacao.setMotivo(requisicao.motivo().trim());
        autorizacao.setFotoUrl(requisicao.fotoUrl());
        autorizacao.setStatus(StatusAutorizacaoVisita.PENDENTE);
        autorizacao.setSolicitadoEm(agora);
        autorizacao.abrirPrazoDeResposta(agora);
        autorizacao.setRegistradoPorId(porteiro.id());
        autorizacao.setRegistradoPorNome(porteiro.nome());

        AutorizacaoVisita salva = autorizacaoRepository.save(autorizacao);
        notificarUnidade(salva);
        return AutorizacaoVisitaResponse.de(salva, agora);
    }

    // ---------- GET /portaria/autorizacoes e /{id} ----------

    /**
     * Portaria ve a fila do condominio inteiro; morador/proprietario ve so a propria unidade.
     * O filtro {@code status} considera o status efetivo (pendente vencida conta como expirada).
     */
    public PaginaResponse<AutorizacaoVisitaResponse> listar(UsuarioAutenticado usuario,
                                                            StatusAutorizacaoVisita status,
                                                            Pageable paginacao) {
        Instant agora = Instant.now();
        Page<AutorizacaoVisita> pagina = enxergaOCondominioInteiro(usuario)
                ? buscarDoCondominio(usuario.condominioId(), status, agora, paginacao)
                : buscarDaUnidade(unidadeDoToken(usuario), status, agora, paginacao);

        return PaginaResponse.de(pagina, autorizacao -> AutorizacaoVisitaResponse.de(autorizacao, agora));
    }

    public AutorizacaoVisitaResponse detalhar(UsuarioAutenticado usuario, Long autorizacaoId) {
        AutorizacaoVisita autorizacao = buscarNoCondominio(autorizacaoId, usuario.condominioId());
        if (!enxergaOCondominioInteiro(usuario)) {
            garantirMesmaUnidade(autorizacao, usuario);
        }
        return AutorizacaoVisitaResponse.de(autorizacao);
    }

    // ---------- PATCH /portaria/autorizacoes/{id}/decisao ----------

    /** Resposta do morador/proprietario da unidade visitada. Avisa os porteiros do condominio. */
    public AutorizacaoVisitaResponse decidir(UsuarioAutenticado morador, Long autorizacaoId,
                                             DecisaoAutorizacaoRequest requisicao) {
        AutorizacaoVisita autorizacao = buscarNoCondominio(autorizacaoId, morador.condominioId());
        garantirMesmaUnidade(autorizacao, morador);

        Instant agora = Instant.now();
        estadoAtual(autorizacao, agora).decidir(autorizacao, requisicao.decisao(), agora); // 409 se nao pendente

        autorizacao.setJustificativa(requisicao.justificativa());
        autorizacao.setRespondidoPorId(morador.id());
        autorizacao.setRespondidoPorNome(morador.nome());
        AutorizacaoVisita salva = autorizacaoRepository.save(autorizacao);

        usuarioRepository.listarPorPerfilNoCondominio(salva.getCondominioId(), Perfil.PORTARIA)
                .forEach(porteiro -> notificacaoDeDecisaoDeVisita.enviar(porteiro, salva));

        return AutorizacaoVisitaResponse.de(salva, agora);
    }

    // ---------- POST /portaria/autorizacoes/{id}/reenviar ----------

    /** Renova o prazo de resposta e manda a notificacao de novo para a unidade. */
    public void reenviar(UsuarioAutenticado porteiro, Long autorizacaoId) {
        AutorizacaoVisita autorizacao = buscarNoCondominio(autorizacaoId, porteiro.condominioId());

        Instant agora = Instant.now();
        estadoAtual(autorizacao, agora).reenviar(autorizacao, agora); // 409 se nao pendente

        notificarUnidade(autorizacaoRepository.save(autorizacao));
    }

    // ---------- usado pelo AcessoService (POST /portaria/acessos, tipo entrada) ----------

    /**
     * Confere e "consome" a autorizacao que libera a entrada do visitante.
     *
     * <p>Sem {@code autorizacaoId}, usa a autorizacao concedida mais recente do visitante que ainda
     * nao foi usada. Quem decide se a entrada pode acontecer e o estado da autorizacao.
     *
     * @return a autorizacao ja marcada como usada e gravada
     */
    public AutorizacaoVisita liberarEntrada(Long condominioId, Long visitanteId, Long autorizacaoId, Instant agora) {
        AutorizacaoVisita autorizacao = autorizacaoId != null
                ? buscarNoCondominio(autorizacaoId, condominioId)
                : autorizacaoRepository
                        .findFirstByCondominioIdAndVisitanteIdAndStatusAndEntradaEmIsNullOrderByRespondidoEmDesc(
                                condominioId, visitanteId, StatusAutorizacaoVisita.AUTORIZADA)
                        .orElseThrow(() -> new RegraDeNegocioException(
                                "Este visitante nao tem autorizacao de entrada. Registre a visita e aguarde a resposta da unidade."));

        if (!Objects.equals(autorizacao.getVisitanteId(), visitanteId)) {
            throw new RegraDeNegocioException("A autorizacao informada pertence a outro visitante.");
        }

        estadoAtual(autorizacao, agora).registrarEntrada(autorizacao, agora); // 409 se nao autorizada
        return autorizacaoRepository.save(autorizacao);
    }

    // ---------- dashboard (sem contexto de seguranca: tudo por parametro) ----------

    /** Pendentes ainda no prazo da unidade, mais recentes primeiro (dashboard do morador). */
    public List<AutorizacaoVisitaResponse> pendentesDaUnidade(Long unidadeId, int limite) {
        if (limite <= 0) {
            return List.of();
        }
        Instant agora = Instant.now();
        return autorizacaoRepository
                .findByUnidadeIdAndStatusAndExpiraEmAfter(unidadeId, StatusAutorizacaoVisita.PENDENTE, agora,
                        PageRequest.of(0, limite, MAIS_RECENTES_PRIMEIRO))
                .getContent().stream()
                .map(autorizacao -> AutorizacaoVisitaResponse.de(autorizacao, agora))
                .toList();
    }

    /** Pendentes ainda no prazo do condominio, mais recentes primeiro (dashboard da portaria). */
    public List<AutorizacaoVisitaResponse> pendentesDoCondominio(Long condominioId, int limite) {
        if (limite <= 0) {
            return List.of();
        }
        Instant agora = Instant.now();
        return autorizacaoRepository
                .findByCondominioIdAndStatusAndExpiraEmAfter(condominioId, StatusAutorizacaoVisita.PENDENTE, agora,
                        PageRequest.of(0, limite, MAIS_RECENTES_PRIMEIRO))
                .getContent().stream()
                .map(autorizacao -> AutorizacaoVisitaResponse.de(autorizacao, agora))
                .toList();
    }

    // ---------- apoio ----------

    /**
     * Estado atual da autorizacao, ja aplicando a expiracao preguicosa: se a pendente venceu,
     * o proprio estado PENDENTE faz a transicao para EXPIRADA e ela e gravada antes de seguir.
     * A operacao seguinte (decidir, reenviar...) recebe entao o estado EXPIRADA e devolve o 409.
     */
    private EstadoDaAutorizacao estadoAtual(AutorizacaoVisita autorizacao, Instant agora) {
        if (seletorDeEstado.estadoDe(autorizacao).expirarSeVencida(autorizacao, agora)) {
            autorizacaoRepository.save(autorizacao);
        }
        return seletorDeEstado.estadoDe(autorizacao);
    }

    private AutorizacaoVisita buscarNoCondominio(Long autorizacaoId, Long condominioId) {
        return autorizacaoRepository.findByIdAndCondominioId(autorizacaoId, condominioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Autorizacao de visita", autorizacaoId));
    }

    private static boolean enxergaOCondominioInteiro(UsuarioAutenticado usuario) {
        return usuario.perfil() == Perfil.PORTARIA;
    }

    private static Long unidadeDoToken(UsuarioAutenticado usuario) {
        if (usuario.unidadeId() == null) {
            throw new AcessoNegadoException("O contexto ativo nao tem unidade vinculada.");
        }
        return usuario.unidadeId();
    }

    /** Morador/proprietario so mexe nas autorizacoes da propria unidade (outra unidade = 403). */
    private static void garantirMesmaUnidade(AutorizacaoVisita autorizacao, UsuarioAutenticado usuario) {
        if (!Objects.equals(autorizacao.getUnidadeId(), unidadeDoToken(usuario))) {
            throw new AcessoNegadoException("Esta autorizacao de visita e de outra unidade.");
        }
    }

    private Page<AutorizacaoVisita> buscarDoCondominio(Long condominioId, StatusAutorizacaoVisita status,
                                                       Instant agora, Pageable paginacao) {
        if (status == null) {
            return autorizacaoRepository.findByCondominioId(condominioId, paginacao);
        }
        return switch (status) {
            case PENDENTE -> autorizacaoRepository.findByCondominioIdAndStatusAndExpiraEmAfter(
                    condominioId, StatusAutorizacaoVisita.PENDENTE, agora, paginacao);
            case EXPIRADA -> autorizacaoRepository.buscarExpiradasDoCondominio(condominioId, agora, paginacao);
            default -> autorizacaoRepository.findByCondominioIdAndStatus(condominioId, status, paginacao);
        };
    }

    private Page<AutorizacaoVisita> buscarDaUnidade(Long unidadeId, StatusAutorizacaoVisita status,
                                                    Instant agora, Pageable paginacao) {
        if (status == null) {
            return autorizacaoRepository.findByUnidadeId(unidadeId, paginacao);
        }
        return switch (status) {
            case PENDENTE -> autorizacaoRepository.findByUnidadeIdAndStatusAndExpiraEmAfter(
                    unidadeId, StatusAutorizacaoVisita.PENDENTE, agora, paginacao);
            case EXPIRADA -> autorizacaoRepository.buscarExpiradasDaUnidade(unidadeId, agora, paginacao);
            default -> autorizacaoRepository.findByUnidadeIdAndStatus(unidadeId, status, paginacao);
        };
    }

    /** Todos os usuarios ativos da unidade (moradores, proprietario, inquilino) recebem o pedido. */
    private void notificarUnidade(AutorizacaoVisita autorizacao) {
        usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(autorizacao.getUnidadeId())
                .forEach(morador -> notificacaoDeVisitanteAguardando.enviar(morador, autorizacao));
    }

    /** "Bloco A - Apto 101" (ou so "Apto 101" quando a unidade nao tem bloco). */
    private static String descrever(Unidade unidade) {
        String identificacao = unidade.getIdentificacao() != null
                ? unidade.getIdentificacao()
                : String.valueOf(unidade.getId());
        return unidade.getBlocoNome() == null ? identificacao : unidade.getBlocoNome() + " - " + identificacao;
    }
}
