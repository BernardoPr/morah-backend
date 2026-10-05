package org.morah.morah.financeiro.servico;

import java.net.URI;
import java.util.List;

import org.morah.morah.comum.erro.TratadorGlobalDeErros.ErroDeCampo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Vira HTTP 400: o webhook tinha assinatura valida, mas o JSON nao pode ser lido ou faltam campos
 * obrigatorios.
 *
 * <p>Por que uma excecao so do modulo? O corpo do webhook chega como texto (para conferir a
 * assinatura), entao o {@code @Valid} e o conversor de JSON do Spring nao atuam e o
 * {@code TratadorGlobalDeErros} nao tem uma excecao "400" para usarmos. Estendendo
 * {@link ErrorResponseException}, o proprio tratador global (no metodo que atende qualquer
 * {@code ErrorResponse}) devolve o status e o RFC 9457 abaixo - no mesmo formato
 * ("Dados invalidos" + lista "erros") dos outros 400 da API.
 */
public class PayloadDeWebhookInvalidoException extends ErrorResponseException {

    public PayloadDeWebhookInvalidoException(String detalhe, List<ErroDeCampo> erros) {
        super(HttpStatus.BAD_REQUEST, problema(detalhe, erros), null);
    }

    private static ProblemDetail problema(String detalhe, List<ErroDeCampo> erros) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setTitle("Dados invalidos");
        problema.setType(URI.create("https://api.morah.com.br/erros/400"));
        if (!erros.isEmpty()) {
            problema.setProperty("erros", erros);
        }
        return problema;
    }
}
