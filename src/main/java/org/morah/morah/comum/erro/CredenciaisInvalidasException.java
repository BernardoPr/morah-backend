package org.morah.morah.comum.erro;

/** Vira HTTP 401. Usada no login e na renovacao de token. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}
