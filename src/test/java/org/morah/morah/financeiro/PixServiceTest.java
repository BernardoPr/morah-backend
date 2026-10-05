package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.morah.morah.financeiro.DadosDeTeste.cobranca;
import static org.morah.morah.financeiro.DadosDeTeste.morador;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.dto.PixResponse;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.financeiro.servico.CalculadoraDeEncargos;
import org.morah.morah.financeiro.servico.CobrancaService;
import org.morah.morah.financeiro.servico.GeradorDeCodigoPix;
import org.morah.morah.financeiro.servico.PixService;
import org.morah.morah.notificacao.template.NotificacaoDePagamentoConfirmado;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.mongodb.core.MongoTemplate;

/** PIX copia-e-cola: 409 para paga/cancelada, valor com encargos e reaproveitamento do codigo. */
@ExtendWith(MockitoExtension.class)
class PixServiceTest {

    @Mock
    private CobrancaRepository cobrancaRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacaoDePagamentoConfirmado notificacao;

    private PixService service;

    @BeforeEach
    void montar() {
        var propriedades = DadosDeTeste.propriedades();
        var calculadora = new CalculadoraDeEncargos(propriedades);
        var cobrancaService = new CobrancaService(cobrancaRepository, mongoTemplate, usuarioRepository,
                calculadora, notificacao);
        service = new PixService(cobrancaService, cobrancaRepository, calculadora, new GeradorDeCodigoPix(),
                propriedades);
    }

    private void existe(Cobranca cobranca) {
        given(cobrancaRepository.findByIdAndCondominioId(cobranca.getId(), 1L)).willReturn(Optional.of(cobranca));
    }

    @Test
    @DisplayName("cobranca paga: 409")
    void cobrancaPaga() {
        Cobranca paga = cobranca(7L, 101L, "460.00", Datas.hoje());
        paga.setStatus(StatusCobranca.PAGO);
        existe(paga);

        assertThatThrownBy(() -> service.obterPix(morador(101L), 7L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja esta paga");
    }

    @Test
    @DisplayName("cobranca cancelada: 409")
    void cobrancaCancelada() {
        Cobranca cancelada = cobranca(7L, 101L, "460.00", Datas.hoje());
        cancelada.setStatus(StatusCobranca.CANCELADO);
        existe(cancelada);

        assertThatThrownBy(() -> service.obterPix(morador(101L), 7L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    @DisplayName("cobranca de outra unidade: 403")
    void outraUnidade() {
        existe(cobranca(7L, 202L, "540.00", Datas.hoje()));

        assertThatThrownBy(() -> service.obterPix(morador(101L), 7L)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    @DisplayName("atrasada: o PIX sai com multa e juros, txid MORAH<id> e validade de 30 minutos")
    void geraCodigoComEncargos() {
        Cobranca atrasada = cobranca(7L, 101L, "500.00", Datas.hoje().minusDays(15));
        existe(atrasada);

        PixResponse pix = service.obterPix(morador(101L), 7L);

        assertThat(pix.codigoPix())
                .contains("0123financeiro@morah.com.br")
                .contains("5406512.50")
                .contains("5917Residencial Morah")
                .contains("0506MORAH7");
        assertThat(pix.codigoPix()).endsWith(
                GeradorDeCodigoPix.crc16(pix.codigoPix().substring(0, pix.codigoPix().length() - 4)));
        assertThat(pix.expiraEm()).isCloseTo(Instant.now().plus(30, ChronoUnit.MINUTES), within(5, ChronoUnit.SECONDS));
        verify(cobrancaRepository).save(atrasada);
        assertThat(atrasada.getCodigoPix()).isEqualTo(pix.codigoPix());
    }

    @Test
    @DisplayName("codigo ainda valido e reaproveitado (nada e gravado)")
    void reaproveitaCodigoValido() {
        Cobranca cobranca = cobranca(7L, 101L, "460.00", Datas.hoje().plusDays(5));
        Instant expiraEm = Instant.now().plus(10, ChronoUnit.MINUTES);
        cobranca.setCodigoPix("CODIGO-ANTERIOR");
        cobranca.setPixExpiraEm(expiraEm);
        existe(cobranca);

        PixResponse pix = service.obterPix(morador(101L), 7L);

        assertThat(pix.codigoPix()).isEqualTo("CODIGO-ANTERIOR");
        assertThat(pix.expiraEm()).isEqualTo(expiraEm);
        verify(cobrancaRepository, never()).save(any());
    }

    @Test
    @DisplayName("codigo expirado: gera outro")
    void geraNovoQuandoExpirado() {
        Cobranca cobranca = cobranca(7L, 101L, "460.00", Datas.hoje().plusDays(5));
        cobranca.setCodigoPix("CODIGO-VENCIDO");
        cobranca.setPixExpiraEm(Instant.now().minus(1, ChronoUnit.MINUTES));
        existe(cobranca);

        PixResponse pix = service.obterPix(morador(101L), 7L);

        assertThat(pix.codigoPix()).isNotEqualTo("CODIGO-VENCIDO").contains("5406460.00");
        verify(cobrancaRepository).save(cobranca);
    }
}
