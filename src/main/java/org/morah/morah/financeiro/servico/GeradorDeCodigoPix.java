package org.morah.morah.financeiro.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Monta o codigo PIX "copia e cola" no formato BR Code (padrao EMV-QRCPS do Banco Central).
 *
 * <p><b>SIMULADO:</b> o codigo tem o formato correto de um PIX ESTATICO (qualquer leitor de BR
 * Code consegue interpreta-lo), mas nao existe nenhum banco ou PSP por tras: a cobranca nao e
 * registrada em lugar nenhum e a confirmacao do pagamento chega pelo nosso webhook simulado.
 * Em producao, este passo seria trocado pela chamada a API do PSP (que devolve um PIX dinamico).
 *
 * <p>O payload e uma sequencia de campos "ID + TAMANHO (2 digitos) + VALOR":
 * <pre>
 * 00 02 01                      versao do payload
 * 26 .. 00 14 br.gov.bcb.pix    dados da conta: identificador do arranjo PIX
 *       01 .. chave             ... e a chave PIX do recebedor
 * 52 04 0000                    categoria do comerciante (0000 = nao informada)
 * 53 03 986                     moeda (986 = real)
 * 54 .. 512.50                  valor (opcional)
 * 58 02 BR                      pais
 * 59 .. nome do recebedor       ate 25 caracteres
 * 60 .. cidade do recebedor     ate 15 caracteres
 * 62 .. 05 .. txid              dados adicionais: identificador da transacao
 * 63 04 XXXX                    CRC16 de tudo que veio antes (incluindo o proprio "6304")
 * </pre>
 */
@Component
public class GeradorDeCodigoPix {

    private static final String ARRANJO_PIX = "br.gov.bcb.pix";
    private static final String MOEDA_REAL = "986";

    /**
     * @param valor nulo = PIX sem valor definido (quem paga digita o valor)
     * @return o codigo copia-e-cola, ja com o CRC no final
     */
    public String gerar(String chavePix, BigDecimal valor, String nomeRecebedor, String cidade, String txid) {
        StringBuilder payload = new StringBuilder()
                .append(campo("00", "01"))
                .append(campo("26", campo("00", ARRANJO_PIX) + campo("01", chavePix)))
                .append(campo("52", "0000"))
                .append(campo("53", MOEDA_REAL));

        if (valor != null) {
            payload.append(campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }

        payload.append(campo("58", "BR"))
                .append(campo("59", nomeRecebedor))
                .append(campo("60", cidade))
                .append(campo("62", campo("05", txid)))
                .append("6304"); // o CRC entra no proprio calculo do CRC

        return payload + crc16(payload.toString());
    }

    /**
     * CRC16-CCITT (variante "FALSE", exigida pelo BR Code): polinomio 0x1021, valor inicial
     * 0xFFFF, sem reflexao e sem XOR final. Devolve 4 digitos hexadecimais maiusculos.
     * O CRC e o "digito verificador" do codigo: se um caractere for alterado, o app do banco recusa.
     */
    public static String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format(Locale.ROOT, "%04X", crc);
    }

    /**
     * Deixa um texto aceitavel para os campos de nome/cidade do BR Code: sem acentos, so letras,
     * numeros e espacos, cortado no tamanho maximo ("Residencial Moráh" -> "Residencial Morah").
     */
    public static String textoSimples(String texto, int tamanhoMaximo) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9 ]", "")
                .trim();
        return semAcento.length() <= tamanhoMaximo ? semAcento : semAcento.substring(0, tamanhoMaximo).trim();
    }

    private static String campo(String id, String valor) {
        if (valor.length() > 99) {
            throw new IllegalArgumentException("Campo " + id + " do BR Code passa de 99 caracteres");
        }
        return id + String.format(Locale.ROOT, "%02d", valor.length()) + valor;
    }
}
