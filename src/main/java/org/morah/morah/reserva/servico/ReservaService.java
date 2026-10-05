package org.morah.morah.reserva.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.servico.ServicoCrudTemplate;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.template.NotificacaoDeDanoEmReserva;
import org.morah.morah.notificacao.template.NotificacaoDeReservaConfirmada;
import org.morah.morah.ocorrencia.dto.NovaOcorrencia;
import org.morah.morah.ocorrencia.modelo.OrigemOcorrencia;
import org.morah.morah.ocorrencia.servico.OcorrenciaService;
import org.morah.morah.reserva.dto.ReservaCreateRequest;
import org.morah.morah.reserva.dto.ReservaResponse;
import org.morah.morah.reserva.dto.VistoriaRequest;
import org.morah.morah.reserva.modelo.AreaComum;
import org.morah.morah.reserva.modelo.MomentoVistoria;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.modelo.StatusReserva;
import org.morah.morah.reserva.modelo.Vistoria;
import org.morah.morah.reserva.repositorio.ReservaRepository;
import org.morah.morah.reserva.strategy.PedidoDeReserva;
import org.morah.morah.reserva.strategy.RegraDeConflitoDeHorario;
import org.morah.morah.reserva.strategy.RegraDeReservaStrategy;
import org.morah.morah.seguranca.jwt.ContextoDeSeguranca;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Regras das reservas de areas comuns.
 *
 * <p><b>Dois padroes de projeto trabalhando juntos na criacao (POST /reservas):</b>
 * <ul>
 *   <li><b>TEMPLATE METHOD</b> - esta classe estende {@link ServicoCrudTemplate}, entao o
 *       {@code criar} herdado segue sempre o mesmo roteiro: {@code validarCriacao} ->
 *       {@code converterParaEntidade} -> {@code antesDeSalvar} -> salvar -> {@code depoisDeSalvar}
 *       -> {@code converterParaResposta}. Aqui so preenchemos os passos;</li>
 *   <li><b>STRATEGY</b> - o passo {@code validarCriacao} aplica a lista de
 *       {@link RegraDeReservaStrategy} que o Spring injeta (uma classe por regra).</li>
 * </ul>
 *
 * <p>Os metodos {@code listar}, {@code buscarPorId} e {@code remover} herdados do template
 * <b>nao</b> sao usados pelo controller: eles nao filtram por condominio. No lugar deles existem
 * {@link #listarDoContextoAtivo}, {@link #detalhar} e {@link #cancelar} (que nao apaga: so muda o status).
 */
@Service
@RequiredArgsConstructor
public class ReservaService extends ServicoCrudTemplate<Reserva, ReservaCreateRequest, ReservaResponse> {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 100;
    private static final Sort INICIO_CRESCENTE = Sort.by(Sort.Direction.ASC, "inicio");
    private static final Sort INICIO_DECRESCENTE = Sort.by(Sort.Direction.DESC, "inicio");

    private final ReservaRepository reservaRepository;
    private final AreaComumService areaComumService;
    private final UsuarioRepository usuarioRepository;
    private final OcorrenciaService ocorrenciaService;
    private final NotificacaoDeReservaConfirmada notificacaoDeReservaConfirmada;
    private final NotificacaoDeDanoEmReserva notificacaoDeDanoEmReserva;

    /** STRATEGY: todas as regras de reserva, ja na ordem do {@code @Order} de cada uma. */
    private final List<RegraDeReservaStrategy> regras;

    // ---------- passos obrigatorios do template ----------

    @Override
    protected MongoRepository<Reserva, Long> repositorio() {
        return reservaRepository;
    }

    @Override
    protected String nomeDoRecurso() {
        return "Reserva";
    }

    @Override
    protected Reserva converterParaEntidade(ReservaCreateRequest requisicao) {
        Reserva reserva = new Reserva();
        reserva.setAreaComumId(requisicao.areaComumId());
        reserva.setInicio(requisicao.inicio());
        reserva.setFim(requisicao.fim());
        reserva.setStatus(StatusReserva.CONFIRMADA);
        return reserva;
    }

    @Override
    protected ReservaResponse converterParaResposta(Reserva reserva) {
        return ReservaResponse.de(reserva);
    }

    // ---------- hooks do template ----------

    /**
     * Hook 1: confere o pedido com TODAS as regras (Strategy). A primeira que falhar lanca 409
     * e o roteiro do template para aqui - nada e gravado.
     */
    @Override
    protected void validarCriacao(ReservaCreateRequest requisicao) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        exigirUnidade(logado);

        AreaComum area = areaComumService.buscarDoCondominio(requisicao.areaComumId(), logado.condominioId());
        PedidoDeReserva pedido = new PedidoDeReserva(area, requisicao.inicio(), requisicao.fim(), Instant.now());

        regras.forEach(regra -> regra.validar(pedido));
    }

    /**
     * Hook 2: completa a reserva com o que nao vem no corpo - unidade e solicitante (do token),
     * nome da area e valor (a taxa da area neste momento).
     *
     * <p>A area e lida de novo aqui porque o template so repassa a requisicao entre os passos.
     * Preferimos uma busca por id (barata) a guardar a area num atributo: o service e um
     * singleton que atende varias requisicoes ao mesmo tempo.
     */
    @Override
    protected void antesDeSalvar(Reserva reserva) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        AreaComum area = areaComumService.buscarDoCondominio(reserva.getAreaComumId(), logado.condominioId());

        reserva.setCondominioId(logado.condominioId());
        reserva.setUnidadeId(logado.unidadeId());
        reserva.setAreaComumNome(area.getNome());
        reserva.setValor(taxaDe(area));
        reserva.setSolicitanteId(logado.id());
        reserva.setSolicitanteNome(logado.nome());
        reserva.setSolicitanteTelefone(usuarioRepository.findById(logado.id())
                .map(Usuario::getTelefone)
                .orElse(null));
    }

    /** Hook 3: confere a corrida com outra reserva simultanea e, se ganhou, avisa a unidade. */
    @Override
    protected void depoisDeSalvar(Reserva salva) {
        garantirQueGanhouAConcorrencia(salva);
        notificarUnidade(salva.getUnidadeId(), morador -> notificacaoDeReservaConfirmada.enviar(morador, salva));
    }

    /**
     * PROTECAO CONTRA RESERVAS SIMULTANEAS - "quem gravou primeiro vence".
     *
     * <p><b>Problema:</b> a {@link RegraDeConflitoDeHorario} consulta o banco e so depois a reserva
     * e gravada. Se dois moradores pedem o mesmo horario no mesmo instante, as duas consultas
     * acontecem antes das duas gravacoes, nenhuma ve conflito e o salao fica reservado duas vezes.
     *
     * <p><b>Solucao:</b> depois de gravar, consultamos de novo. Se aparecer outra reserva CONFIRMADA
     * sobreposta com id <b>menor</b>, ela foi gravada antes (os ids sao sequenciais, gerados por um
     * {@code $inc} atomico no {@code GeradorDeIdSequencial}) e vence: desfazemos a nossa e
     * devolvemos 409 "Horario indisponivel", como o contrato preve. A de id menor, ao fazer a mesma
     * conferencia, so enxerga ids maiores e e mantida - das duas, exatamente uma sobrevive.
     *
     * <p><b>Por que apagar e nao marcar como CANCELADA?</b> Para o cliente essa reserva nunca
     * existiu (ele recebeu 409). Um documento CANCELADA apareceria na lista dele como se ele
     * mesmo tivesse cancelado.
     *
     * <p><b>Limite conhecido:</b> o id e gerado um instante antes da gravacao. Se a requisicao de id
     * menor demorar a gravar e a de id maior gravar e conferir nesse intervalo, nenhuma desiste.
     * A janela e de milissegundos; elimina-la de vez exigiria transacao (MongoDB em replica set) ou
     * uma trava por area com {@code findAndModify} - complexidade que nao compensa neste projeto.
     */
    private void garantirQueGanhouAConcorrencia(Reserva salva) {
        boolean outraGravouAntes = reservaRepository
                .listarConfirmadasQueSobrepoem(salva.getAreaComumId(), salva.getInicio(), salva.getFim())
                .stream()
                .anyMatch(outra -> outra.getId() < salva.getId());

        if (outraGravouAntes) {
            reservaRepository.delete(salva);
            throw new RegraDeNegocioException(RegraDeConflitoDeHorario.HORARIO_INDISPONIVEL);
        }
    }

    // ---------- operacoes especificas da reserva ----------

    /**
     * GET /reservas: o que cada perfil enxerga.
     * <ul>
     *   <li>morador/proprietario: so as reservas da propria unidade (filtra o dia se vier {@code data});</li>
     *   <li>portaria: as do condominio no dia {@code data} - sem data, o dia de hoje (a agenda da guarita);</li>
     *   <li>sindico: todas as do condominio (filtra o dia se vier {@code data}).</li>
     * </ul>
     *
     * <p>Ordenacao: com um dia escolhido, por inicio crescente (a agenda do dia, na ordem em que
     * acontece); sem dia, por inicio decrescente (as mais distantes no futuro primeiro, depois as
     * passadas, da mais recente para a mais antiga).
     */
    public PaginaResponse<ReservaResponse> listarDoContextoAtivo(LocalDate data, int pagina, int tamanho) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();

        LocalDate dia = (data == null && logado.perfil() == Perfil.PORTARIA) ? Datas.hoje() : data;
        Range<Instant> periodo = dia == null ? null : Range.rightOpen(Datas.inicioDoDia(dia), Datas.fimDoDia(dia));
        Pageable paginacao = PageRequest.of(
                Math.max(pagina, 0),
                Math.clamp(tamanho, 1, TAMANHO_MAXIMO_DA_PAGINA),
                dia == null ? INICIO_DECRESCENTE : INICIO_CRESCENTE);

        Page<Reserva> encontradas = switch (logado.perfil()) {
            case MORADOR, PROPRIETARIO -> {
                Long unidadeId = exigirUnidade(logado);
                yield periodo == null
                        ? reservaRepository.findByUnidadeId(unidadeId, paginacao)
                        : reservaRepository.findByUnidadeIdAndInicioBetween(unidadeId, periodo, paginacao);
            }
            case PORTARIA, SINDICO -> periodo == null
                    ? reservaRepository.findByCondominioId(logado.condominioId(), paginacao)
                    : reservaRepository.findByCondominioIdAndInicioBetween(logado.condominioId(), periodo, paginacao);
        };

        return PaginaResponse.de(encontradas, ReservaResponse::de);
    }

    /** GET /reservas/{id}: portaria e sindico veem qualquer uma do condominio; morador, so as da unidade. */
    public ReservaResponse detalhar(Long reservaId) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        Reserva reserva = buscarDoCondominio(reservaId, logado.condominioId());

        if (ehMoradorOuProprietario(logado)) {
            exigirMesmaUnidade(logado, reserva);
        }
        return ReservaResponse.de(reserva);
    }

    /**
     * DELETE /reservas/{id}: o morador desiste da reserva. So vale para reserva CONFIRMADA que
     * ainda nao comecou. O documento nao e apagado: vira CANCELADA (e libera o horario, porque
     * as consultas de conflito so olham as confirmadas).
     */
    public void cancelar(Long reservaId) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        Reserva reserva = buscarDoCondominio(reservaId, logado.condominioId());
        exigirMesmaUnidade(logado, reserva);

        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new RegraDeNegocioException(
                    "Somente reservas confirmadas podem ser canceladas (status atual: " + reserva.getStatus().getValor() + ").");
        }

        Instant agora = Instant.now();
        if (reserva.jaComecou(agora)) {
            throw new RegraDeNegocioException("A reserva ja comecou e nao pode mais ser cancelada.");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setCanceladaEm(agora);
        reservaRepository.save(reserva);
    }

    /**
     * POST /reservas/{id}/vistoria: a portaria registra a vistoria de entrada ou de saida.
     * <ul>
     *   <li>so em reserva CONFIRMADA, e uma vistoria de cada momento;</li>
     *   <li>a de saida encerra a reserva (CONCLUIDA);</li>
     *   <li>com dano: abre uma ocorrencia (que avisa o sindico) e avisa a unidade.</li>
     * </ul>
     */
    public ReservaResponse registrarVistoria(Long reservaId, VistoriaRequest requisicao) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        Reserva reserva = buscarDoCondominio(reservaId, logado.condominioId());

        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new RegraDeNegocioException(
                    "Vistoria so pode ser registrada em reserva confirmada (status atual: " + reserva.getStatus().getValor() + ").");
        }
        if (reserva.temVistoria(requisicao.momento())) {
            throw new RegraDeNegocioException(
                    "A vistoria de " + requisicao.momento().getValor() + " desta reserva ja foi registrada.");
        }

        Vistoria vistoria = new Vistoria(
                requisicao.momento(),
                requisicao.observacao(),
                requisicao.houveDano(),
                requisicao.fotosUrl() == null ? new ArrayList<>() : new ArrayList<>(requisicao.fotosUrl()),
                Instant.now(),
                logado.id(),
                logado.nome());
        reserva.getVistorias().add(vistoria);

        if (requisicao.momento() == MomentoVistoria.SAIDA) {
            reserva.setStatus(StatusReserva.CONCLUIDA);
        }

        Reserva salva = reservaRepository.save(reserva);

        if (vistoria.isDano()) {
            registrarDano(salva, vistoria, logado);
        }
        return ReservaResponse.de(salva);
    }

    // ---------- consultas usadas pelo dashboard (sem contexto de seguranca) ----------

    /** Reservas CONFIRMADAS da unidade que ainda nao terminaram, da mais proxima para a mais distante. */
    public List<ReservaResponse> proximasDaUnidade(Long unidadeId, int limite) {
        if (limite <= 0) {
            return List.of();
        }
        return reservaRepository
                .findByUnidadeIdAndStatusAndFimGreaterThan(unidadeId, StatusReserva.CONFIRMADA, Instant.now(),
                        PageRequest.of(0, limite, INICIO_CRESCENTE))
                .stream()
                .map(ReservaResponse::de)
                .toList();
    }

    /** Reservas nao canceladas do condominio no dia (agenda da portaria), por inicio crescente. */
    public List<ReservaResponse> doDia(Long condominioId, LocalDate data) {
        return reservaRepository
                .findByCondominioIdAndStatusNotAndInicioBetween(condominioId, StatusReserva.CANCELADA,
                        Range.rightOpen(Datas.inicioDoDia(data), Datas.fimDoDia(data)), INICIO_CRESCENTE)
                .stream()
                .map(ReservaResponse::de)
                .toList();
    }

    // ---------- apoio ----------

    private void registrarDano(Reserva reserva, Vistoria vistoria, UsuarioAutenticado porteiro) {
        String descricao = vistoria.getObservacao() == null || vistoria.getObservacao().isBlank()
                ? "Dano registrado na vistoria de " + vistoria.getMomento().getValor() + "."
                : vistoria.getObservacao();

        ocorrenciaService.abrir(new NovaOcorrencia(
                reserva.getCondominioId(),
                reserva.getUnidadeId(),
                OrigemOcorrencia.RESERVA,
                reserva.getId(),
                "Dano em " + reserva.getAreaComumNome(),
                descricao,
                porteiro.id(),
                porteiro.nome()));

        notificarUnidade(reserva.getUnidadeId(), morador -> notificacaoDeDanoEmReserva.enviar(morador, reserva));
    }

    /** Reserva de outro condominio (ou inexistente) vira 404 - nao revelamos que ela existe. */
    private Reserva buscarDoCondominio(Long reservaId, Long condominioId) {
        return reservaRepository.findByIdAndCondominioId(reservaId, condominioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(nomeDoRecurso(), reservaId));
    }

    private void notificarUnidade(Long unidadeId, Consumer<Usuario> envio) {
        usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(unidadeId).forEach(envio);
    }

    private static boolean ehMoradorOuProprietario(UsuarioAutenticado logado) {
        return logado.perfil() == Perfil.MORADOR || logado.perfil() == Perfil.PROPRIETARIO;
    }

    private static Long exigirUnidade(UsuarioAutenticado logado) {
        if (logado.unidadeId() == null) {
            throw new AcessoNegadoException("O perfil ativo nao esta vinculado a uma unidade.");
        }
        return logado.unidadeId();
    }

    /** Morador do mesmo condominio tentando mexer na reserva de outra unidade: 403. */
    private static void exigirMesmaUnidade(UsuarioAutenticado logado, Reserva reserva) {
        if (!Objects.equals(reserva.getUnidadeId(), exigirUnidade(logado))) {
            throw new AcessoNegadoException("Esta reserva pertence a outra unidade.");
        }
    }

    private static BigDecimal taxaDe(AreaComum area) {
        BigDecimal taxa = area.getTaxa() == null ? BigDecimal.ZERO : area.getTaxa();
        return taxa.setScale(2, RoundingMode.HALF_UP);
    }
}
