package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.morah.morah.financeiro.config.PropriedadesFinanceiro;
import org.morah.morah.financeiro.servico.VerificadorDeAssinaturaHmac;

/** Assinatura HMAC-SHA256 do webhook de pagamento. */
class VerificadorDeAssinaturaHmacTest {

    private static final String CORPO = """
            {"transacaoId":"tx-1","cobrancaId":7,"status":"confirmado"}""";

    private final VerificadorDeAssinaturaHmac verificador = new VerificadorDeAssinaturaHmac(DadosDeTeste.propriedades());

    @Test
    @DisplayName("confere com o vetor de teste classico do HMAC-SHA256")
    void vetorDeTesteConhecido() {
        var comChaveKey = new VerificadorDeAssinaturaHmac(new PropriedadesFinanceiro(
                "key", "chave", 30, BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(comChaveKey.assinar("The quick brown fox jumps over the lazy dog"))
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void assinaturaValidaEhAceita() {
        String assinatura = verificador.assinar(CORPO);

        assertThatCode(() -> verificador.validar(CORPO, assinatura)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("aceita o prefixo sha256= e hexadecimal maiusculo")
    void prefixoEMaiusculas() {
        String assinatura = "sha256=" + verificador.assinar(CORPO).toUpperCase();

        assertThatCode(() -> verificador.validar(CORPO, assinatura)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("corpo alterado depois de assinado: 401")
    void corpoAlterado() {
        String assinatura = verificador.assinar(CORPO);

        assertThatThrownBy(() -> verificador.validar(CORPO.replace("7", "8"), assinatura))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("assinatura feita com outro segredo: 401")
    void outroSegredo() {
        var doAtacante = new VerificadorDeAssinaturaHmac(new PropriedadesFinanceiro(
                "chute", "chave", 30, BigDecimal.ZERO, BigDecimal.ZERO));

        assertThatThrownBy(() -> verificador.validar(CORPO, doAtacante.assinar(CORPO)))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("cabecalho ausente, vazio ou que nem e hexadecimal: 401")
    void assinaturaAusenteOuMalFormada() {
        assertThatThrownBy(() -> verificador.validar(CORPO, null)).isInstanceOf(CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> verificador.validar(CORPO, "  ")).isInstanceOf(CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> verificador.validar(CORPO, "sha256=xyz")).isInstanceOf(CredenciaisInvalidasException.class);
    }
}
