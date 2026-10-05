package org.morah.morah.financeiro.servico;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.servico.ServicoCrudTemplate;
import org.morah.morah.financeiro.dto.GerarCobrancasResponse;
import org.morah.morah.financeiro.dto.TaxaCreateRequest;
import org.morah.morah.financeiro.dto.TaxaResponse;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.Competencia;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.modelo.Taxa;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.financeiro.repositorio.TaxaRepository;
import org.morah.morah.financeiro.strategy.ParcelaDoRateio;
import org.morah.morah.financeiro.strategy.RateioStrategy;
import org.morah.morah.financeiro.strategy.SeletorDeRateio;
import org.morah.morah.notificacao.template.NotificacaoDeCobrancaGerada;
import org.morah.morah.seguranca.jwt.ContextoDeSeguranca;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Taxas do condominio e a geracao das cobrancas em lote.
 *
 * <p>Dois padroes trabalhando juntos aqui:
 * <ul>
 *   <li><b>Template Method</b> ({@link ServicoCrudTemplate}): o cadastro da taxa segue o roteiro
 *       pronto (converter -> antesDeSalvar -> salvar -> responder); so escrevemos os passos.</li>
 *   <li><b>Strategy</b> ({@link RateioStrategy}): na geracao das cobrancas, a divisao do valor
 *       entre as unidades e delegada a estrategia do tipo da taxa, escolhida pelo
 *       {@link SeletorDeRateio}. Este service nao tem nenhum {@code if} sobre tipo de rateio.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TaxaService extends ServicoCrudTemplate<Taxa, TaxaCreateRequest, TaxaResponse> {

    static final String UNIDADE_INATIVA = "unidade inativa";
    static final String JA_POSSUI_COBRANCA = "unidade ja possui cobranca desta taxa";
    static final String VALOR_ZERADO = "valor rateado para a unidade ficou zerado";

    private final TaxaRepository taxaRepository;
    private final CobrancaRepository cobrancaRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final SeletorDeRateio seletorDeRateio;
    private final NotificacaoDeCobrancaGerada notificacaoDeCobrancaGerada;

    // ---------- passos obrigatorios do template (POST /financeiro/taxas) ----------

    @Override
    protected MongoRepository<Taxa, Long> repositorio() {
        return taxaRepository;
    }

    @Override
    protected String nomeDoRecurso() {
        return "Taxa";
    }

    @Override
    protected Taxa converterParaEntidade(TaxaCreateRequest requisicao) {
        Taxa taxa = new Taxa();
        taxa.setDescricao(requisicao.descricao().trim());
        taxa.setCompetencia(requisicao.competencia());
        taxa.setValor(requisicao.valor().setScale(2, RoundingMode.HALF_UP));
        taxa.setTipoRateio(requisicao.tipoRateio());
        taxa.setVencimentoDia(requisicao.vencimentoDia());
        return taxa;
    }

    @Override
    protected TaxaResponse converterParaResposta(Taxa taxa) {
        return TaxaResponse.de(taxa);
    }

    /** Hook: a taxa e do condominio do sindico logado. */
    @Override
    protected void antesDeSalvar(Taxa taxa) {
        taxa.setCondominioId(ContextoDeSeguranca.usuarioLogado().condominioId());
    }

    // ---------- geracao das cobrancas ----------

    /** POST /financeiro/taxas/{taxaId}/gerar-cobrancas (somente sindico). */
    public GerarCobrancasResponse gerarCobrancas(UsuarioAutenticado sindico, Long taxaId) {
        Taxa taxa = taxaRepository.findByIdAndCondominioId(taxaId, sindico.condominioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Taxa", taxaId));
        return gerarCobrancas(taxa);
    }

    /**
     * Gera uma cobranca por unidade do condominio. Publico tambem para a carga inicial.
     *
     * <p>Quem participa do rateio sao as unidades ATIVAS - inclusive as que ja tem cobranca desta
     * taxa. Assim, se o sindico rodar a geracao de novo (ex.: depois de ativar uma unidade), as
     * parcelas continuam com o mesmo valor e so as unidades que faltavam recebem o boleto.
     *
     * <p>Unidade que fica de fora entra em {@code falhas} com o motivo: inativa, ja cobrada,
     * recusada pela estrategia (ex.: sem fracao ideal) ou com valor zerado.
     */
    public GerarCobrancasResponse gerarCobrancas(Taxa taxa) {
        List<Unidade> unidades = unidadeRepository.findByCondominioIdOrderByIdentificacao(taxa.getCondominioId());
        List<Unidade> ativas = unidades.stream().filter(TaxaService::estaAtiva).toList();

        // STRATEGY: a regra de divisao vem do tipo da taxa; aqui ninguem sabe qual e.
        RateioStrategy rateio = seletorDeRateio.obter(taxa.getTipoRateio());
        Map<Long, ParcelaDoRateio> parcelas = rateio.ratear(taxa.getValor(), ativas).stream()
                .collect(Collectors.toMap(ParcelaDoRateio::unidadeId, Function.identity()));

        Set<Long> jaCobradas = cobrancaRepository.findByTaxaId(taxa.getId()).stream()
                .map(Cobranca::getUnidadeId)
                .collect(Collectors.toSet());

        LocalDate vencimento = Competencia.vencimento(taxa.getCompetencia(), taxa.getVencimentoDia());
        List<Cobranca> novas = new ArrayList<>();
        List<GerarCobrancasResponse.Falha> falhas = new ArrayList<>();

        for (Unidade unidade : unidades) {
            ParcelaDoRateio parcela = parcelas.get(unidade.getId());
            String motivo = motivoParaNaoCobrar(unidade, parcela, jaCobradas);
            if (motivo != null) {
                falhas.add(new GerarCobrancasResponse.Falha(unidade.getId(), motivo));
            } else {
                novas.add(novaCobranca(taxa, unidade, parcela, vencimento));
            }
        }

        List<Cobranca> geradas = novas.isEmpty() ? List.of() : cobrancaRepository.saveAll(novas);
        geradas.forEach(this::notificarUnidade);

        return new GerarCobrancasResponse(taxa.getId(), geradas.size(), falhas);
    }

    private static String motivoParaNaoCobrar(Unidade unidade, ParcelaDoRateio parcela, Set<Long> jaCobradas) {
        if (!estaAtiva(unidade)) {
            return UNIDADE_INATIVA;
        }
        if (jaCobradas.contains(unidade.getId())) {
            return JA_POSSUI_COBRANCA;
        }
        if (parcela == null) {
            return "unidade nao entrou no rateio";
        }
        if (parcela.falhou()) {
            return parcela.motivoDaFalha();
        }
        if (parcela.valor().signum() <= 0) {
            return VALOR_ZERADO;
        }
        return null;
    }

    private static Cobranca novaCobranca(Taxa taxa, Unidade unidade, ParcelaDoRateio parcela, LocalDate vencimento) {
        Cobranca cobranca = new Cobranca();
        cobranca.setCondominioId(taxa.getCondominioId());
        cobranca.setUnidadeId(unidade.getId());
        cobranca.setTaxaId(taxa.getId());
        cobranca.setTaxaDescricao(taxa.getDescricao());
        cobranca.setCompetencia(taxa.getCompetencia());
        cobranca.setVencimento(vencimento);
        cobranca.setValorOriginal(parcela.valor());
        cobranca.setStatus(StatusCobranca.PENDENTE);
        return cobranca;
    }

    private void notificarUnidade(Cobranca cobranca) {
        usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(cobranca.getUnidadeId())
                .forEach(usuario -> notificacaoDeCobrancaGerada.enviar(usuario, cobranca));
    }

    /** Unidade sem status gravado conta como ativa (o padrao da entidade e "ativa"). */
    private static boolean estaAtiva(Unidade unidade) {
        return unidade.getStatus() == null || "ativa".equalsIgnoreCase(unidade.getStatus());
    }
}
