package org.morah.morah.unidade.servico;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.servico.ServicoCrudTemplate;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.template.NotificacaoDeDecisaoDeVinculo;
import org.morah.morah.notificacao.template.NotificacaoDeSolicitacaoDeVinculo;
import org.morah.morah.seguranca.jwt.ContextoDeSeguranca;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.dto.DecisaoSolicitacaoVinculoRequest;
import org.morah.morah.unidade.dto.SolicitacaoVinculoCreateRequest;
import org.morah.morah.unidade.dto.SolicitacaoVinculoResponse;
import org.morah.morah.unidade.modelo.Cpf;
import org.morah.morah.unidade.modelo.DecisaoSolicitacao;
import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.StatusSolicitacaoVinculo;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.SolicitacaoVinculoRepository;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.unidade.template.ConcessaoPorSolicitacaoAprovada;
import org.morah.morah.unidade.template.DestinoDoAcesso;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Solicitacoes de vinculo: o morador pede (POST), o sindico lista e decide (GET/PATCH).
 *
 * <p>A criacao reaproveita o TEMPLATE METHOD {@link ServicoCrudTemplate}: o fluxo
 * validar -> converter -> antesDeSalvar -> salvar -> depoisDeSalvar encaixa exatamente
 * (validar duplicidade, preencher dados do token, gravar, avisar os sindicos). Como o
 * {@code criar(requisicao)} da classe pai so recebe o corpo da requisicao, os hooks leem o
 * usuario logado pelo {@link ContextoDeSeguranca} - igual ao {@code AvisoService}. Os metodos
 * {@code listar}/{@code buscarPorId} herdados nao filtram por condominio e por isso nao sao
 * expostos no controller; as consultas abaixo sao as filtradas.
 *
 * <p>A aprovacao delega ao TEMPLATE METHOD {@link ConcessaoPorSolicitacaoAprovada}, que cria
 * o vinculo da unidade e libera o acesso no app.
 */
@Service
@RequiredArgsConstructor
public class SolicitacaoVinculoService
        extends ServicoCrudTemplate<SolicitacaoVinculo, SolicitacaoVinculoCreateRequest, SolicitacaoVinculoResponse> {

    private final SolicitacaoVinculoRepository solicitacaoVinculoRepository;
    private final VinculoUnidadeRepository vinculoUnidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeService unidadeService;
    private final ConcessaoPorSolicitacaoAprovada concessaoPorSolicitacaoAprovada;
    private final NotificacaoDeSolicitacaoDeVinculo notificacaoDeSolicitacao;
    private final NotificacaoDeDecisaoDeVinculo notificacaoDeDecisao;

    // ---------- passos obrigatorios do template ----------

    @Override
    protected MongoRepository<SolicitacaoVinculo, Long> repositorio() {
        return solicitacaoVinculoRepository;
    }

    @Override
    protected String nomeDoRecurso() {
        return "Solicitacao de vinculo";
    }

    @Override
    protected SolicitacaoVinculo converterParaEntidade(SolicitacaoVinculoCreateRequest requisicao) {
        SolicitacaoVinculo solicitacao = new SolicitacaoVinculo();
        solicitacao.setTipoVinculo(requisicao.tipoVinculo());
        solicitacao.setNomeSolicitado(requisicao.nome().trim());
        solicitacao.setCpf(Cpf.normalizar(requisicao.cpf()));
        solicitacao.setObservacao(requisicao.observacao() == null || requisicao.observacao().isBlank()
                ? null
                : requisicao.observacao().trim());
        return solicitacao;
    }

    @Override
    protected SolicitacaoVinculoResponse converterParaResposta(SolicitacaoVinculo solicitacao) {
        return SolicitacaoVinculoResponse.de(solicitacao);
    }

    // ---------- hooks da criacao (POST /unidades/minha/vinculos/solicitacoes) ----------

    /** 404 se o token nao tem unidade; 409 se o pedido repete um pendente ou alguem ja vinculado. */
    @Override
    protected void validarCriacao(SolicitacaoVinculoCreateRequest requisicao) {
        Unidade unidade = unidadeService.buscarMinhaEntidade(ContextoDeSeguranca.usuarioLogado());
        String cpf = Cpf.normalizar(requisicao.cpf());

        garantirQueNaoHaPedidoPendenteIgual(unidade, cpf, requisicao.nome());
        garantirQueAindaNaoEstaVinculada(unidade, cpf, requisicao.tipoVinculo(), requisicao.nome());
    }

    /** Unidade, solicitante e data saem do token - nunca do corpo da requisicao. */
    @Override
    protected void antesDeSalvar(SolicitacaoVinculo solicitacao) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        Unidade unidade = unidadeService.buscarMinhaEntidade(logado);

        solicitacao.setCondominioId(unidade.getCondominioId());
        solicitacao.setUnidadeId(unidade.getId());
        solicitacao.setUnidadeIdentificacao(unidade.getIdentificacao());
        solicitacao.setStatus(StatusSolicitacaoVinculo.PENDENTE);
        solicitacao.setSolicitadoEm(Instant.now());
        solicitacao.setSolicitanteId(logado.id());
        solicitacao.setSolicitanteNome(logado.nome());
    }

    /** Avisa todos os sindicos do condominio que ha um pedido esperando aprovacao. */
    @Override
    protected void depoisDeSalvar(SolicitacaoVinculo solicitacao) {
        usuarioRepository.listarPorPerfilNoCondominio(solicitacao.getCondominioId(), Perfil.SINDICO)
                .forEach(sindico -> notificacaoDeSolicitacao.enviar(sindico, solicitacao));
    }

    // ---------- operacoes do sindico ----------

    /** GET /unidades/{unidadeId}/vinculos/solicitacoes: pendentes da unidade, mais antigas primeiro. */
    public List<SolicitacaoVinculoResponse> listarPendentes(UsuarioAutenticado sindico, Long unidadeId) {
        unidadeService.buscarEntidadeDoCondominio(unidadeId, sindico.condominioId()); // 404 se for de outro condominio

        return solicitacaoVinculoRepository
                .findByUnidadeIdAndCondominioIdAndStatusOrderBySolicitadoEmAsc(
                        unidadeId, sindico.condominioId(), StatusSolicitacaoVinculo.PENDENTE)
                .stream()
                .map(SolicitacaoVinculoResponse::de)
                .toList();
    }

    /**
     * PATCH /unidades/{unidadeId}/vinculos/solicitacoes/{solicitacaoId}.
     *
     * <p>A ordem importa: a decisao e registrada em memoria, a concessao (que valida antes de
     * gravar qualquer coisa) roda, e so entao a solicitacao e salva. Se a concessao recusar
     * (ex.: a pessoa ja foi vinculada por outro caminho), a solicitacao continua pendente no
     * banco, em vez de ficar "aprovada" sem vinculo.
     */
    public SolicitacaoVinculoResponse decidir(UsuarioAutenticado sindico, Long unidadeId, Long solicitacaoId,
                                              DecisaoSolicitacaoVinculoRequest requisicao) {
        Unidade unidade = unidadeService.buscarEntidadeDoCondominio(unidadeId, sindico.condominioId());

        SolicitacaoVinculo solicitacao = solicitacaoVinculoRepository
                .findByIdAndUnidadeIdAndCondominioId(solicitacaoId, unidadeId, sindico.condominioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(nomeDoRecurso(), solicitacaoId));

        if (!solicitacao.estaPendente()) {
            throw new RegraDeNegocioException(
                    "Essa solicitacao ja foi " + solicitacao.getStatus().getValor() + ".");
        }

        registrarDecisao(solicitacao, sindico, requisicao);

        if (requisicao.decisao() == DecisaoSolicitacao.APROVADA) {
            // Cria o vinculo, libera o app e avisa quem pediu (ConcessaoDeAcessoTemplate).
            concessaoPorSolicitacaoAprovada.conceder(new DestinoDoAcesso(unidade, sindico.condominioNome()), solicitacao);
        }

        SolicitacaoVinculo salva = solicitacaoVinculoRepository.save(solicitacao);

        if (requisicao.decisao() == DecisaoSolicitacao.REJEITADA) {
            notificarSolicitante(salva);
        }
        return SolicitacaoVinculoResponse.de(salva);
    }

    /**
     * Usado pelo dashboard do sindico (campo solicitacoesVinculoPendentes).
     * Recebe o condominio por parametro: nao le o contexto de seguranca.
     */
    public long contarPendentes(Long condominioId) {
        if (condominioId == null) {
            return 0;
        }
        return solicitacaoVinculoRepository.countByCondominioIdAndStatus(condominioId, StatusSolicitacaoVinculo.PENDENTE);
    }

    // ---------- apoio ----------

    /** Mesmo CPF pendente na unidade; sem CPF, compara pelo nome (sem acento e sem diferenciar caixa). */
    private void garantirQueNaoHaPedidoPendenteIgual(Unidade unidade, String cpf, String nome) {
        boolean duplicada = solicitacaoVinculoRepository
                .findByUnidadeIdAndCondominioIdAndStatusOrderBySolicitadoEmAsc(
                        unidade.getId(), unidade.getCondominioId(), StatusSolicitacaoVinculo.PENDENTE)
                .stream()
                .anyMatch(pendente -> cpf != null
                        ? cpf.equals(pendente.getCpf())
                        : mesmoNome(nome, pendente.getNomeSolicitado()));

        if (duplicada) {
            throw new RegraDeNegocioException(
                    "Ja existe uma solicitacao pendente para essa pessoa nesta unidade. Aguarde a decisao do sindico.");
        }
    }

    /** Evita pedir de novo o vinculo de quem ja esta vinculado (mesmo CPF e mesmo tipo). */
    private void garantirQueAindaNaoEstaVinculada(Unidade unidade, String cpf, TipoVinculo tipo, String nome) {
        if (cpf == null) {
            return;
        }
        LocalDate hoje = Datas.hoje();
        boolean jaVinculada = vinculoUnidadeRepository
                .findByUnidadeIdAndCondominioIdAndAtivoTrue(unidade.getId(), unidade.getCondominioId())
                .stream()
                .anyMatch(vinculo -> vinculo.estaVigenteEm(hoje) && vinculo.ehDaPessoaComTipo(cpf, tipo));

        if (jaVinculada) {
            throw new RegraDeNegocioException("%s ja possui um vinculo ativo de %s nesta unidade."
                    .formatted(nome.trim(), tipo.getValor()));
        }
    }

    private void registrarDecisao(SolicitacaoVinculo solicitacao, UsuarioAutenticado sindico,
                                  DecisaoSolicitacaoVinculoRequest requisicao) {
        solicitacao.setStatus(requisicao.decisao().comoStatus());
        solicitacao.setDecididoEm(Instant.now());
        solicitacao.setDecididoPorId(sindico.id());
        solicitacao.setDecididoPorNome(sindico.nome());
        solicitacao.setJustificativa(requisicao.justificativa() == null || requisicao.justificativa().isBlank()
                ? null
                : requisicao.justificativa().trim());
    }

    private void notificarSolicitante(SolicitacaoVinculo solicitacao) {
        if (solicitacao.getSolicitanteId() == null) {
            return;
        }
        usuarioRepository.findById(solicitacao.getSolicitanteId())
                .ifPresent(solicitante -> notificacaoDeDecisao.enviar(solicitante, solicitacao));
    }

    private static boolean mesmoNome(String nome, String outro) {
        return outro != null && normalizarNome(nome).equals(normalizarNome(outro));
    }

    /** "  Lucas   LIMA " e "Lucas Lima" sao o mesmo nome; "Joao" e "João" tambem. */
    private static String normalizarNome(String nome) {
        String semAcento = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
