package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.morah.morah.financeiro.DadosDeTeste.cobranca;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.morah.morah.comum.erro.TratadorGlobalDeErros.ErroDeCampo;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.dto.NovoPagamento;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.financeiro.servico.CobrancaService;
import org.morah.morah.financeiro.servico.PayloadDeWebhookInvalidoException;
import org.morah.morah.financeiro.servico.VerificadorDeAssinaturaHmac;
import org.morah.morah.financeiro.servico.WebhookPagamentoService;
import org.springframework.http.HttpStatus;

import tools.jackson.databind.json.JsonMapper;

/** Webhook do gateway: assinatura primeiro, depois JSON, e idempotencia no processamento. */
@ExtendWith(MockitoExtension.class)
class WebhookPagamentoServiceTest {

    private static final String CONFIRMADO = """
            {"transacaoId":"tx-123","cobrancaId":7,"valor":460.00,"status":"confirmado",
             "pagoEm":"2026-10-05T13:00:00Z","campoQueOGatewayInventou":true}""";

    @Mock
    private CobrancaRepository cobrancaRepository;
    @Mock
    private CobrancaService cobrancaService;

    private final VerificadorDeAssinaturaHmac verificador = new VerificadorDeAssinaturaHmac(DadosDeTeste.propriedades());
    private WebhookPagamentoService service;

    @BeforeEach
    void montar() {
        service = new WebhookPagamentoService(verificador, JsonMapper.builder().build(),
                cobrancaRepository, cobrancaService);
    }

    private void receberAssinado(String corpo) {
        service.receber(corpo, "sha256=" + verificador.assinar(corpo));
    }

    @Test
    @DisplayName("assinatura invalida: 401 e o corpo nem e lido")
    void assinaturaInvalida() {
        assertThatThrownBy(() -> service.receber(CONFIRMADO, "sha256=" + "0".repeat(64)))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verifyNoInteractions(cobrancaRepository, cobrancaService);
    }

    @Test
    @DisplayName("confirmado: registra o pagamento com os dados do gateway")
    void confirmadoQuitaACobranca() {
        Cobranca pendente = cobranca(7L, 101L, "460.00", Datas.hoje());
        given(cobrancaRepository.findById(7L)).willReturn(Optional.of(pendente));
        given(cobrancaRepository.existsByTransacaoId("tx-123")).willReturn(false);

        receberAssinado(CONFIRMADO);

        ArgumentCaptor<NovoPagamento> pagamento = ArgumentCaptor.forClass(NovoPagamento.class);
        verify(cobrancaService).registrarPagamento(eq(pendente), pagamento.capture());
        assertThat(pagamento.getValue().transacaoId()).isEqualTo("tx-123");
        assertThat(pagamento.getValue().valor()).isEqualByComparingTo("460.00");
        assertThat(pagamento.getValue().pagoEm()).isEqualTo(Instant.parse("2026-10-05T13:00:00Z"));
        assertThat(pagamento.getValue().meio()).isEqualTo("pix");
        assertThat(pagamento.getValue().baixadoPorId()).isNull();
    }

    @Test
    @DisplayName("aviso repetido (cobranca ja paga pela mesma transacao): ignorado")
    void avisoRepetidoEhIgnorado() {
        Cobranca paga = cobranca(7L, 101L, "460.00", Datas.hoje());
        paga.setStatus(StatusCobranca.PAGO);
        paga.setTransacaoId("tx-123");
        given(cobrancaRepository.findById(7L)).willReturn(Optional.of(paga));

        assertThatCode(() -> receberAssinado(CONFIRMADO)).doesNotThrowAnyException();
        verify(cobrancaService, never()).registrarPagamento(any(), any());
    }

    @Test
    @DisplayName("transacao que ja quitou outra cobranca: ignorada")
    void transacaoJaUsada() {
        given(cobrancaRepository.findById(7L)).willReturn(Optional.of(cobranca(7L, 101L, "460.00", Datas.hoje())));
        given(cobrancaRepository.existsByTransacaoId("tx-123")).willReturn(true);

        receberAssinado(CONFIRMADO);

        verify(cobrancaService, never()).registrarPagamento(any(), any());
    }

    @Test
    @DisplayName("falhou: so registra no log, a cobranca continua em aberto")
    void pagamentoQueFalhou() {
        given(cobrancaRepository.findById(7L)).willReturn(Optional.of(cobranca(7L, 101L, "460.00", Datas.hoje())));

        receberAssinado("""
                {"transacaoId":"tx-9","cobrancaId":7,"status":"falhou"}""");

        verify(cobrancaService, never()).registrarPagamento(any(), any());
    }

    @Test
    @DisplayName("cobranca inexistente: aceita (202) sem erro, para o gateway nao reenviar para sempre")
    void cobrancaInexistente() {
        given(cobrancaRepository.findById(7L)).willReturn(Optional.empty());

        assertThatCode(() -> receberAssinado(CONFIRMADO)).doesNotThrowAnyException();
        verify(cobrancaService, never()).registrarPagamento(any(), any());
    }

    @Test
    @DisplayName("assinatura valida mas sem campos obrigatorios: 400 com a lista de erros")
    void camposObrigatorios() {
        assertThatThrownBy(() -> receberAssinado("{\"valor\": 10}"))
                .isInstanceOfSatisfying(PayloadDeWebhookInvalidoException.class, erro -> {
                    assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    Map<String, Object> propriedades = erro.getBody().getProperties();
                    assertThat(propriedades).isNotNull();
                    @SuppressWarnings("unchecked")
                    List<ErroDeCampo> erros = (List<ErroDeCampo>) propriedades.get("erros");
                    assertThat(erros).extracting(ErroDeCampo::campo)
                            .containsExactly("transacaoId", "cobrancaId", "status");
                });
        verifyNoInteractions(cobrancaRepository);
    }

    @Test
    @DisplayName("JSON quebrado ou status fora do enum: 400")
    void jsonInvalido() {
        assertThatThrownBy(() -> receberAssinado("{nao e json"))
                .isInstanceOf(PayloadDeWebhookInvalidoException.class);
        assertThatThrownBy(() -> receberAssinado("""
                {"transacaoId":"tx-1","cobrancaId":7,"status":"talvez"}"""))
                .isInstanceOf(PayloadDeWebhookInvalidoException.class);
    }
}
