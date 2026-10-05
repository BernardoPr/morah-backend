package org.morah.morah.financeiro.servico;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.morah.morah.comum.erro.CredenciaisInvalidasException;
import org.morah.morah.financeiro.config.PropriedadesFinanceiro;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Confere a assinatura do webhook de pagamento (cabecalho {@code X-Webhook-Signature}).
 *
 * <p><b>Por que assinatura e nao JWT?</b> Quem chama o webhook e o gateway de pagamento, nao um
 * usuario do app - ele nao tem login. O gateway e a API combinam um segredo
 * ({@code morah.financeiro.webhook-segredo}); a cada chamada o gateway calcula
 * {@code HMAC-SHA256(segredo, corpo)} e manda o resultado em hexadecimal no cabecalho. Nos
 * refazemos a conta: se bater, o corpo veio de quem conhece o segredo e nao foi alterado no caminho.
 *
 * <p>Aceita o prefixo opcional {@code sha256=} (formato usado por GitHub, Stripe e outros) e
 * hexadecimal maiusculo ou minusculo.
 *
 * <p><b>Comparacao em tempo constante:</b> um {@code equals} comum para no primeiro byte
 * diferente; medindo o tempo de resposta, um atacante descobriria a assinatura byte a byte.
 * {@link MessageDigest#isEqual} sempre percorre tudo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificadorDeAssinaturaHmac {

    private static final String ALGORITMO = "HmacSHA256";
    private static final String PREFIXO = "sha256=";

    private final PropriedadesFinanceiro propriedades;

    /** Lanca 401 ({@link CredenciaisInvalidasException}) se a assinatura faltar ou nao conferir. */
    public void validar(String corpo, String assinaturaRecebida) {
        if (assinaturaRecebida == null || assinaturaRecebida.isBlank()) {
            throw new CredenciaisInvalidasException("Assinatura do webhook ausente (cabecalho X-Webhook-Signature).");
        }

        byte[] recebida = decodificar(assinaturaRecebida);
        byte[] esperada = calcular(corpo == null ? "" : corpo);

        if (recebida == null || !MessageDigest.isEqual(esperada, recebida)) {
            throw new CredenciaisInvalidasException("Assinatura do webhook invalida.");
        }
    }

    /** Assinatura em hexadecimal minusculo, igual a que o gateway envia (util para testes). */
    public String assinar(String corpo) {
        return HexFormat.of().formatHex(calcular(corpo));
    }

    private byte[] calcular(String corpo) {
        String segredo = propriedades.webhookSegredo();
        if (segredo == null || segredo.isBlank()) {
            log.error("morah.financeiro.webhook-segredo nao configurado: todos os webhooks serao recusados");
            throw new CredenciaisInvalidasException("Webhook nao configurado no servidor.");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            return mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException excecao) {
            // HmacSHA256 faz parte de toda JVM; chegar aqui e erro de ambiente, nao do cliente.
            throw new IllegalStateException("HmacSHA256 indisponivel na JVM", excecao);
        }
    }

    /** "sha256=AB12..." ou "ab12..." -> bytes. Texto que nao e hexadecimal vira null (= invalida). */
    private byte[] decodificar(String assinatura) {
        String hexadecimal = assinatura.trim();
        if (hexadecimal.toLowerCase(Locale.ROOT).startsWith(PREFIXO)) {
            hexadecimal = hexadecimal.substring(PREFIXO.length());
        }
        try {
            return HexFormat.of().parseHex(hexadecimal.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException excecao) {
            return null;
        }
    }
}
