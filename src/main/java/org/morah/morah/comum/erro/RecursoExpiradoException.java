package org.morah.morah.comum.erro;

/** Vira HTTP 410 (gone). Ex.: aviso que ja saiu de vigencia. */
public class RecursoExpiradoException extends RuntimeException {

    public RecursoExpiradoException(String mensagem) {
        super(mensagem);
    }
}
