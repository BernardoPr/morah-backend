package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.morah.morah.financeiro.DadosDeTeste.cobranca;
import static org.morah.morah.financeiro.DadosDeTeste.morador;
import static org.morah.morah.financeiro.DadosDeTeste.sindico;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.dto.BaixaManualRequest;
import org.morah.morah.financeiro.dto.CobrancaResponse;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.financeiro.servico.CalculadoraDeEncargos;
import org.morah.morah.financeiro.servico.CobrancaService;
import org.morah.morah.notificacao.template.NotificacaoDePagamentoConfirmado;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/** Regras do CobrancaService: quem ve o que, baixa manual (409) e filtros da listagem. */
@ExtendWith(MockitoExtension.class)
class CobrancaServiceTest {

    @Mock
    private CobrancaRepository cobrancaRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacaoDePagamentoConfirmado notificacao;

    private CobrancaService service;

    @BeforeEach
    void montar() {
        service = new CobrancaService(cobrancaRepository, mongoTemplate, usuarioRepository,
                new CalculadoraDeEncargos(DadosDeTeste.propriedades()), notificacao);
    }

    private BaixaManualRequest baixa(String valor) {
        return new BaixaManualRequest(" Transferencia ", new BigDecimal(valor), Instant.now(), "pago na administracao");
    }

    // ---------- baixa manual ----------

    @Test
    @DisplayName("baixa manual em cobranca ja paga: 409")
    void baixaEmCobrancaPaga() {
        Cobranca paga = cobranca(7L, 101L, "500.00", Datas.hoje());
        paga.setStatus(StatusCobranca.PAGO);
        given(cobrancaRepository.findByIdAndCondominioId(7L, 1L)).willReturn(Optional.of(paga));

        assertThatThrownBy(() -> service.baixarManualmente(sindico(), 7L, baixa("500.00")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja esta paga");
        verify(cobrancaRepository, never()).save(any());
        verifyNoInteractions(notificacao);
    }

    @Test
    @DisplayName("baixa manual em cobranca cancelada: 409")
    void baixaEmCobrancaCancelada() {
        Cobranca cancelada = cobranca(7L, 101L, "500.00", Datas.hoje());
        cancelada.setStatus(StatusCobranca.CANCELADO);
        given(cobrancaRepository.findByIdAndCondominioId(7L, 1L)).willReturn(Optional.of(cancelada));

        assertThatThrownBy(() -> service.baixarManualmente(sindico(), 7L, baixa("500.00")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    @DisplayName("baixa manual de cobranca de outro condominio: 404")
    void baixaDeOutroCondominio() {
        given(cobrancaRepository.findByIdAndCondominioId(7L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.baixarManualmente(sindico(), 7L, baixa("500.00")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("baixa manual de cobranca atrasada: fica paga, congela os encargos e avisa a unidade")
    void baixaManualComSucesso() {
        Cobranca atrasada = cobranca(7L, 101L, "500.00", Datas.hoje().minusDays(15));
        given(cobrancaRepository.findByIdAndCondominioId(7L, 1L)).willReturn(Optional.of(atrasada));
        given(cobrancaRepository.save(any(Cobranca.class))).willAnswer(chamada -> chamada.getArgument(0));
        Usuario ana = DadosDeTeste.usuario(1L, "Ana Souza");
        Usuario bruno = DadosDeTeste.usuario(4L, "Bruno Alves");
        given(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(101L)).willReturn(List.of(ana, bruno));

        CobrancaResponse resposta = service.baixarManualmente(sindico(), 7L, baixa("512.50"));

        assertThat(resposta.status()).isEqualTo(StatusCobranca.PAGO);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("512.50");
        assertThat(resposta.multa()).isEqualByComparingTo("10.00");
        assertThat(resposta.juros()).isEqualByComparingTo("2.50");

        assertThat(atrasada.getStatus()).isEqualTo(StatusCobranca.PAGO);
        assertThat(atrasada.getMeio()).isEqualTo("transferencia");
        assertThat(atrasada.getBaixadoPorId()).isEqualTo(2L);
        assertThat(atrasada.getTransacaoId()).isNull();
        verify(notificacao).enviar(ana, atrasada);
        verify(notificacao).enviar(bruno, atrasada);
    }

    // ---------- quem ve o que ----------

    @Test
    @DisplayName("morador nao ve cobranca de outra unidade do mesmo condominio: 403")
    void moradorDeOutraUnidade() {
        given(cobrancaRepository.findByIdAndCondominioId(7L, 1L))
                .willReturn(Optional.of(cobranca(7L, 202L, "540.00", Datas.hoje())));

        assertThatThrownBy(() -> service.detalhar(morador(101L), 7L)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("morador ve a propria cobranca; vencida aparece como 'atrasado'")
    void moradorVeAPropria() {
        given(cobrancaRepository.findByIdAndCondominioId(7L, 1L))
                .willReturn(Optional.of(cobranca(7L, 101L, "500.00", Datas.hoje().minusDays(15))));

        CobrancaResponse resposta = service.detalhar(morador(101L), 7L);

        assertThat(resposta.status()).isEqualTo(StatusCobranca.ATRASADO);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("512.50");
    }

    @Test
    void sindicoVeQualquerUnidadeDoCondominio() {
        given(cobrancaRepository.findByIdAndCondominioId(7L, 1L))
                .willReturn(Optional.of(cobranca(7L, 202L, "540.00", Datas.hoje())));

        assertThat(service.detalhar(sindico(), 7L).unidadeId()).isEqualTo(202L);
    }

    // ---------- listagem com filtros ----------

    private Document consultaUsadaNaListagem() {
        ArgumentCaptor<Query> consulta = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).count(consulta.capture(), eq(Cobranca.class));
        return consulta.getValue().getQueryObject();
    }

    @Test
    @DisplayName("morador: o unidadeId da URL e ignorado e trocado pela unidade do token")
    void moradorSoListaAPropriaUnidade() {
        Pageable paginacao = PageRequest.of(0, 20);
        given(mongoTemplate.find(any(Query.class), eq(Cobranca.class))).willReturn(List.of());

        service.listar(morador(101L), null, "2026-10", 202L, paginacao);

        Document filtro = consultaUsadaNaListagem();
        assertThat(filtro.get("condominioId")).isEqualTo(1L);
        assertThat(filtro.get("unidadeId")).isEqualTo(101L);
        assertThat(filtro.get("competencia")).isEqualTo("2026-10");
    }

    @Test
    @DisplayName("sindico: ?status=atrasado vira 'PENDENTE com vencimento antes de hoje'")
    void filtroDeAtrasadas() {
        given(mongoTemplate.find(any(Query.class), eq(Cobranca.class))).willReturn(List.of());

        service.listar(sindico(), StatusCobranca.ATRASADO, null, null, PageRequest.of(0, 20));

        Document filtro = consultaUsadaNaListagem();
        assertThat(filtro).doesNotContainKey("unidadeId");
        assertThat(filtro.get("status")).isEqualTo(StatusCobranca.PENDENTE);
        assertThat(filtro.get("vencimento")).isEqualTo(new Document("$lt", Datas.hoje()));
    }

    @Test
    @DisplayName("?status=pendente so traz as que ainda nao venceram")
    void filtroDePendentes() {
        given(mongoTemplate.find(any(Query.class), eq(Cobranca.class))).willReturn(List.of());

        service.listar(sindico(), StatusCobranca.PENDENTE, null, 202L, PageRequest.of(0, 20));

        Document filtro = consultaUsadaNaListagem();
        assertThat(filtro.get("unidadeId")).isEqualTo(202L);
        assertThat(filtro.get("vencimento")).isEqualTo(new Document("$gte", Datas.hoje()));
    }

    // ---------- dashboard ----------

    @Test
    @DisplayName("proximasDaUnidade: pendentes (inclui atrasadas) em ordem de vencimento")
    void proximasDaUnidade() {
        LocalDate hoje = Datas.hoje();
        given(cobrancaRepository.findByUnidadeIdAndStatusOrderByVencimentoAsc(
                101L, StatusCobranca.PENDENTE, PageRequest.of(0, 3)))
                .willReturn(List.of(cobranca(1L, 101L, "460.00", hoje.minusDays(20)),
                        cobranca(2L, 101L, "460.00", hoje.plusDays(10))));

        List<CobrancaResponse> proximas = service.proximasDaUnidade(101L, 3);

        assertThat(proximas).extracting(CobrancaResponse::status)
                .containsExactly(StatusCobranca.ATRASADO, StatusCobranca.PENDENTE);
        verify(cobrancaRepository, times(1))
                .findByUnidadeIdAndStatusOrderByVencimentoAsc(101L, StatusCobranca.PENDENTE, PageRequest.of(0, 3));
    }

    @Test
    void proximasComLimiteZeroNaoConsultaOBanco() {
        assertThat(service.proximasDaUnidade(101L, 0)).isEmpty();
        verifyNoInteractions(cobrancaRepository);
    }
}
