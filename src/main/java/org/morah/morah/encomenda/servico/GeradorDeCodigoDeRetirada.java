package org.morah.morah.encomenda.servico;

import java.security.SecureRandom;
import java.util.Collection;

import org.springframework.stereotype.Component;

/**
 * Gera os codigos de 6 digitos usados na retirada de encomendas (do morador e dos terceiros).
 *
 * <p>Usamos {@link SecureRandom} e nao {@code java.util.Random}: o {@code Random} comum e
 * previsivel (quem ve alguns codigos consegue adivinhar os proximos), e o codigo e justamente a
 * prova de que quem esta no balcao pode levar o pacote.
 */
@Component
public class GeradorDeCodigoDeRetirada {

    private static final int QUANTIDADE_DE_CODIGOS = 1_000_000; // 000000 a 999999

    private final SecureRandom aleatorio = new SecureRandom();

    /** Codigo com exatamente 6 digitos (zeros a esquerda incluidos, ex.: "004217"). */
    public String gerar() {
        return "%06d".formatted(aleatorio.nextInt(QUANTIDADE_DE_CODIGOS));
    }

    /**
     * Codigo que nao colide com os informados. Usado para que o codigo de um terceiro nunca
     * seja igual ao do morador (nem ao de outra autorizacao ativa da mesma encomenda): senao a
     * portaria nao saberia qual autorizacao foi usada.
     */
    public String gerarDiferenteDe(Collection<String> codigosEmUso) {
        String codigo;
        do {
            codigo = gerar();
        } while (codigosEmUso.contains(codigo));
        return codigo;
    }
}
