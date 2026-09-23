package org.morah.morah.comum.erro;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

/**
 * Transforma qualquer excecao em uma resposta padrao RFC 9457
 * (application/problem+json), exatamente como o contrato define no schema "Problem".
 *
 * <p>Com isso os controllers e services ficam limpos: eles apenas lancam a excecao
 * (ex.: {@code throw new RecursoNaoEncontradoException(...)}) e quem monta o JSON de erro
 * e esta classe.
 */
@Slf4j
@RestControllerAdvice
public class TratadorGlobalDeErros {

    /** Item do array "erros" do contrato (erro de validacao campo a campo). */
    public record ErroDeCampo(String campo, String mensagem) {
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException excecao) {
        return montar(HttpStatus.NOT_FOUND, "Recurso nao encontrado", excecao.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraDeNegocioException excecao) {
        return montar(HttpStatus.CONFLICT, "Operacao nao permitida", excecao.getMessage());
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail naoAutenticado(CredenciaisInvalidasException excecao) {
        return montar(HttpStatus.UNAUTHORIZED, "Nao autenticado", excecao.getMessage());
    }

    @ExceptionHandler({ AcessoNegadoException.class, AccessDeniedException.class })
    public ProblemDetail acessoNegado(Exception excecao) {
        return montar(HttpStatus.FORBIDDEN, "Acesso negado", excecao.getMessage());
    }

    /** Erros de @Valid nos DTOs de entrada. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail dadosInvalidos(MethodArgumentNotValidException excecao) {
        List<ErroDeCampo> erros = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroDeCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();

        ProblemDetail problema = montar(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "Verifique os campos enviados.");
        problema.setProperty("erros", erros);
        return problema;
    }

    /**
     * Rede de seguranca: qualquer erro nao previsto cai aqui.
     *
     * <p>Os erros que o proprio Spring ja sabe descrever (rota inexistente, metodo HTTP errado,
     * corpo JSON invalido...) implementam {@link ErrorResponse} e sao devolvidos com o status
     * correto. O resto vira 500, sem vazar detalhes internos para o cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> erroInesperado(Exception excecao) {
        if (excecao instanceof ErrorResponse erroConhecidoPeloSpring) {
            return ResponseEntity.status(erroConhecidoPeloSpring.getStatusCode())
                    .body(erroConhecidoPeloSpring.getBody());
        }

        log.error("Erro inesperado", excecao);
        return ResponseEntity.internalServerError()
                .body(montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                        "Ocorreu um erro inesperado. Tente novamente mais tarde."));
    }

    private ProblemDetail montar(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://api.morah.com.br/erros/" + status.value()));
        return problema;
    }
}
