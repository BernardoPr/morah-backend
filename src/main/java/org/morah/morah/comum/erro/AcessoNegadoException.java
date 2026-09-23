package org.morah.morah.comum.erro;

/** Vira HTTP 403. Ex.: o usuario nao tem o vinculo (condominio, perfil) pedido. */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
