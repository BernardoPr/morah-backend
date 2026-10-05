package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.morah.morah.financeiro.DadosDeTeste.cobranca;
import static org.morah.morah.financeiro.DadosDeTeste.unidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.financeiro.dto.GerarCobrancasResponse;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.modelo.Taxa;
import org.morah.morah.financeiro.modelo.TipoRateio;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.financeiro.repositorio.TaxaRepository;
import org.morah.morah.financeiro.servico.TaxaService;
import org.morah.morah.financeiro.strategy.RateioIgualitarioStrategy;
import org.morah.morah.financeiro.strategy.RateioPorFracaoIdealStrategy;
import org.morah.morah.financeiro.strategy.SeletorDeRateio;
import org.morah.morah.notificacao.template.NotificacaoDeCobrancaGerada;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.repositorio.UsuarioRepository;

/** Geracao de cobrancas em lote: Strategy de rateio + relatorio de falhas por unidade. */
@ExtendWith(MockitoExtension.class)
class TaxaServiceTest {

    @Mock
    private TaxaRepository taxaRepository;
    @Mock
    private CobrancaRepository cobrancaRepository;
    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacaoDeCobrancaGerada notificacao;

    private TaxaService service;

    @BeforeEach
    void montar() {
        var seletor = new SeletorDeRateio(List.of(new RateioIgualitarioStrategy(), new RateioPorFracaoIdealStrategy()));
        service = new TaxaService(taxaRepository, cobrancaRepository, unidadeRepository, usuarioRepository,
                seletor, notificacao);
        lenient().when(cobrancaRepository.saveAll(anyList())).thenAnswer(chamada -> chamada.getArgument(0));
    }

    private static Taxa taxa(TipoRateio tipo, String valor) {
        Taxa taxa = new Taxa();
        taxa.setId(10L);
        taxa.setCondominioId(1L);
        taxa.setDescricao("Taxa condominial");
        taxa.setCompetencia("2026-10");
        taxa.setValor(new BigDecimal(valor));
        taxa.setTipoRateio(tipo);
        taxa.setVencimentoDia(10);
        return taxa;
    }

    @SuppressWarnings("unchecked")
    private List<Cobranca> cobrancasGravadas() {
        ArgumentCaptor<List<Cobranca>> gravadas = ArgumentCaptor.forClass(List.class);
        verify(cobrancaRepository).saveAll(gravadas.capture());
        return gravadas.getValue();
    }

    @Test
    @DisplayName("fracao ideal: gera para quem pode e explica cada unidade que ficou de fora")
    void geraERelataFalhas() {
        Unidade inativa = unidade(102L, "0.23");
        inativa.setStatus("inativa");
        given(unidadeRepository.findByCondominioIdOrderByIdentificacao(1L)).willReturn(List.of(
                unidade(101L, "0.23"), inativa, unidade(201L, "0.27"), unidade(202L, "0.27"), unidade(301L, null)));
        // A 201 ja recebeu o boleto desta taxa numa geracao anterior.
        given(cobrancaRepository.findByTaxaId(10L)).willReturn(List.of(cobranca(50L, 201L, "540.00", LocalDate.of(2026, 10, 10))));
        given(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(any())).willReturn(List.of(DadosDeTeste.usuario(1L, "Ana Souza")));

        GerarCobrancasResponse resposta = service.gerarCobrancas(taxa(TipoRateio.FRACAO_IDEAL, "2000.00"));

        assertThat(resposta.taxaId()).isEqualTo(10L);
        assertThat(resposta.totalGeradas()).isEqualTo(2);
        assertThat(resposta.falhas()).extracting(GerarCobrancasResponse.Falha::unidadeId).containsExactly(102L, 201L, 301L);
        assertThat(resposta.falhas()).extracting(GerarCobrancasResponse.Falha::motivo).containsExactly(
                "unidade inativa", "unidade ja possui cobranca desta taxa", "unidade sem fracao ideal cadastrada");

        List<Cobranca> gravadas = cobrancasGravadas();
        assertThat(gravadas).extracting(Cobranca::getUnidadeId).containsExactly(101L, 202L);
        assertThat(gravadas).extracting(Cobranca::getValorOriginal)
                .containsExactly(new BigDecimal("460.00"), new BigDecimal("540.00"));
        assertThat(gravadas).allSatisfy(cobranca -> {
            assertThat(cobranca.getVencimento()).isEqualTo(LocalDate.of(2026, 10, 10));
            assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.PENDENTE);
            assertThat(cobranca.getTaxaDescricao()).isEqualTo("Taxa condominial");
            assertThat(cobranca.getCompetencia()).isEqualTo("2026-10");
        });
        // Um aviso por usuario de cada unidade que recebeu boleto.
        verify(notificacao, times(2)).enviar(any(), any());
    }

    @Test
    @DisplayName("igualitario: unidade inativa nao entra na divisao e a soma bate com a taxa")
    void igualitarioIgnoraInativas() {
        Unidade inativa = unidade(202L, null);
        inativa.setStatus("inativa");
        given(unidadeRepository.findByCondominioIdOrderByIdentificacao(1L)).willReturn(List.of(
                unidade(101L, null), unidade(102L, null), unidade(201L, null), inativa));
        given(cobrancaRepository.findByTaxaId(10L)).willReturn(List.of());

        GerarCobrancasResponse resposta = service.gerarCobrancas(taxa(TipoRateio.IGUALITARIO, "100.00"));

        assertThat(resposta.totalGeradas()).isEqualTo(3);
        assertThat(cobrancasGravadas()).extracting(Cobranca::getValorOriginal).containsExactly(
                new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
    }

    @Test
    @DisplayName("taxa de outro condominio: 404")
    void taxaDeOutroCondominio() {
        given(taxaRepository.findByIdAndCondominioId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarCobrancas(DadosDeTeste.sindico(), 10L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
