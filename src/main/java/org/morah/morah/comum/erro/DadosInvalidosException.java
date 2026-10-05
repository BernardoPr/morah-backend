package org.morah.morah.comum.erro;

/**
 * Vira HTTP 400. Para regras de preenchimento que as anotacoes ({@code @NotBlank}...) nao
 * conseguem expressar sozinhas. Ex.: publico alvo "bloco" sem o {@code blocoId}.
 */
public class DadosInvalidosException extends RuntimeException {

    public DadosInvalidosException(String mensagem) {
        super(mensagem);
    }
}
