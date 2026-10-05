package org.morah.morah.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.financeiro.servico.GeradorDeCodigoPix;

/** Codigo PIX copia-e-cola (BR Code) e o CRC16 que fecha o codigo. */
class GeradorDeCodigoPixTest {

    private final GeradorDeCodigoPix gerador = new GeradorDeCodigoPix();

    @Test
    @DisplayName("CRC16-CCITT-FALSE do vetor de teste padrao '123456789' e 29B1")
    void crcDoVetorPadrao() {
        assertThat(GeradorDeCodigoPix.crc16("123456789")).isEqualTo("29B1");
    }

    @Test
    @DisplayName("reproduz o exemplo de PIX estatico do manual do BR Code (CRC 1D3D)")
    void exemploDoBancoCentral() {
        String codigo = gerador.gerar("123e4567-e12b-12d1-a456-426655440000", null,
                "Fulano de Tal", "BRASILIA", "***");

        assertThat(codigo).isEqualTo("00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000"
                + "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***63041D3D");
    }

    @Test
    @DisplayName("codigo da cobranca: chave, valor com 2 casas, txid MORAH<id> e CRC valido no final")
    void codigoDaCobranca() {
        String codigo = gerador.gerar("financeiro@morah.com.br", new BigDecimal("512.5"),
                "Residencial Morah", "SAO PAULO", "MORAH42");

        assertThat(codigo)
                .startsWith("000201")
                .contains("0014br.gov.bcb.pix0123financeiro@morah.com.br")
                .contains("5406512.50")
                .contains("5917Residencial Morah")
                .contains("62110507MORAH42");

        // Os 4 ultimos caracteres sao o CRC de tudo que vem antes (incluindo o "6304").
        String semCrc = codigo.substring(0, codigo.length() - 4);
        assertThat(semCrc).endsWith("6304");
        assertThat(codigo).endsWith(GeradorDeCodigoPix.crc16(semCrc));
    }

    @Test
    void textoSimplesTiraAcentosESimbolosECorta() {
        assertThat(GeradorDeCodigoPix.textoSimples("Condomínio São João - Bloco Ç", 25))
                .isEqualTo("Condominio Sao Joao  Bloc");
        assertThat(GeradorDeCodigoPix.textoSimples(null, 25)).isEmpty();
    }
}
