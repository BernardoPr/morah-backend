package org.morah.morah.comum.erro;

/** Vira HTTP 409 (conflito). Use quando a operacao e valida, mas nao pode acontecer agora. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
