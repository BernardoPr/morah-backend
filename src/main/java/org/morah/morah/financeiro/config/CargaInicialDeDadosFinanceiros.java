package org.morah.morah.financeiro.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Set;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.dto.GerarCobrancasResponse;
import org.morah.morah.financeiro.dto.NovoPagamento;
import org.morah.morah.financeiro.modelo.Competencia;
import org.morah.morah.financeiro.modelo.DocumentoFinanceiro;
import org.morah.morah.financeiro.modelo.Taxa;
import org.morah.morah.financeiro.modelo.TipoRateio;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.financeiro.repositorio.DocumentoFinanceiroRepository;
import org.morah.morah.financeiro.repositorio.TaxaRepository;
import org.morah.morah.financeiro.servico.CobrancaService;
import org.morah.morah.financeiro.servico.TaxaService;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dados financeiros de demonstracao (roda depois das unidades e usuarios: {@code @Order(4)}).
 *
 * <ul>
 *   <li><b>Mes anterior:</b> "Taxa condominial" de R$ 2.000,00 rateada por fracao ideal
 *       (101/102 = R$ 460,00; 201/202 = R$ 540,00), vencida no dia 10. As unidades 102, 201 e 202
 *       ja pagaram; a <b>101 nao pagou</b> - aparece como "atrasado" com multa e juros e deixa a
 *       inadimplencia do sindico em 25% (1 de 4 unidades).</li>
 *   <li><b>Mes atual:</b> a mesma taxa, com as 4 cobrancas pendentes (vencimento dia 10).</li>
 *   <li>Um documento de prestacao de contas do mes anterior (URL ficticia).</li>
 * </ul>
 *
 * <p>As cobrancas sao geradas pelo mesmo {@code TaxaService.gerarCobrancas} do endpoint, ou seja,
 * a carga tambem passa pela STRATEGY de rateio. Idempotente: confere taxa/documento pelo nome
 * antes de criar.
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "morah.carga-inicial", havingValue = "true")
public class CargaInicialDeDadosFinanceiros implements CommandLineRunner {

    private static final Long CONDOMINIO = 1L;
    private static final String TAXA_CONDOMINIAL = "Taxa condominial";
    private static final BigDecimal VALOR_DA_TAXA = new BigDecimal("2000.00");
    private static final int DIA_DO_VENCIMENTO = 10;
    private static final String CPF_DO_SINDICO = "22222222222";

    /** No mes anterior, so a 101 (Ana/Bruno) ficou devendo. */
    private static final Set<Long> PAGARAM_O_MES_ANTERIOR = Set.of(102L, 201L, 202L);

    private final TaxaRepository taxaRepository;
    private final CobrancaRepository cobrancaRepository;
    private final DocumentoFinanceiroRepository documentoFinanceiroRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final TaxaService taxaService;
    private final CobrancaService cobrancaService;

    @Override
    public void run(String... args) {
        if (unidadeRepository.countByCondominioId(CONDOMINIO) == 0) {
            log.warn("Carga inicial financeira: nenhuma unidade no condominio {}, nada a fazer", CONDOMINIO);
            return;
        }

        YearMonth mesAtual = YearMonth.from(Datas.hoje());
        YearMonth mesAnterior = mesAtual.minusMonths(1);

        criarTaxaDoMesAnterior(mesAnterior);
        criarTaxaComCobrancas(mesAtual);
        criarPrestacaoDeContas(mesAnterior);
    }

    private void criarTaxaDoMesAnterior(YearMonth mes) {
        Taxa taxa = criarTaxaComCobrancas(mes);
        if (taxa == null) {
            return; // ja existia
        }
        cobrancaRepository.findByTaxaId(taxa.getId()).stream()
                .filter(cobranca -> PAGARAM_O_MES_ANTERIOR.contains(cobranca.getUnidadeId()))
                .forEach(cobranca -> cobrancaService.registrarPagamento(cobranca, new NovoPagamento(
                        // pago dois dias antes do vencimento, as 10h
                        cobranca.getVencimento().minusDays(2).atTime(LocalTime.of(10, 0)).atZone(Datas.FUSO).toInstant(),
                        cobranca.getValorOriginal(),
                        "pix",
                        "Carga inicial: pagamento de demonstracao",
                        "DEMO-" + cobranca.getCompetencia() + "-" + cobranca.getUnidadeId(),
                        null)));
        log.info("Carga inicial: taxa de {} criada; unidades {} pagas e 101 em atraso",
                Competencia.de(mes), PAGARAM_O_MES_ANTERIOR);
    }

    /** Cria a taxa condominial do mes e gera as cobrancas. Devolve null se a taxa ja existia. */
    private Taxa criarTaxaComCobrancas(YearMonth mes) {
        String competencia = Competencia.de(mes);
        if (taxaRepository.findFirstByCondominioIdAndCompetenciaAndDescricao(
                CONDOMINIO, competencia, TAXA_CONDOMINIAL).isPresent()) {
            return null;
        }

        Taxa taxa = new Taxa();
        taxa.setCondominioId(CONDOMINIO);
        taxa.setDescricao(TAXA_CONDOMINIAL);
        taxa.setCompetencia(competencia);
        taxa.setValor(VALOR_DA_TAXA);
        taxa.setTipoRateio(TipoRateio.FRACAO_IDEAL);
        taxa.setVencimentoDia(DIA_DO_VENCIMENTO);
        Taxa salva = taxaRepository.save(taxa);

        GerarCobrancasResponse resultado = taxaService.gerarCobrancas(salva);
        log.info("Carga inicial: taxa {} ({}) com {} cobrancas geradas e falhas {}",
                salva.getId(), competencia, resultado.totalGeradas(), resultado.falhas());
        return salva;
    }

    private void criarPrestacaoDeContas(YearMonth mes) {
        String nomeDoMes = mes.getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"));
        String titulo = "Prestacao de contas - " + nomeDoMes + "/" + mes.getYear();
        if (documentoFinanceiroRepository.existsByCondominioIdAndTitulo(CONDOMINIO, titulo)) {
            return;
        }

        DocumentoFinanceiro documento = new DocumentoFinanceiro();
        documento.setCondominioId(CONDOMINIO);
        documento.setTipo("prestacao_contas");
        documento.setTitulo(titulo);
        // URL ficticia (dominio reservado para exemplos): nao ha arquivo de verdade.
        documento.setArquivoUrl("https://example.com/morah/prestacao-de-contas-" + Competencia.de(mes) + ".pdf");
        documento.setCompetencia(mes.atDay(1));
        documento.setPublicadoEm(Instant.now());
        documento.setPublicadoPorId(usuarioRepository.findByCpf(CPF_DO_SINDICO).map(Usuario::getId).orElse(null));
        documentoFinanceiroRepository.save(documento);
        log.info("Carga inicial: documento financeiro \"{}\" publicado", titulo);
    }
}
