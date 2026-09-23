package org.morah.morah.comum.erro;

/** Vira HTTP 404. Ex.: {@code throw new RecursoNaoEncontradoException("Aviso", id);} */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " nao encontrado(a): " + id);
    }
}
