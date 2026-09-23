package org.morah.morah.comum.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Formato de lista paginada usado por TODOS os endpoints de listagem
 * (schemas *Page do contrato: { "content": [...], "page": {...} }).
 *
 * <p>Uso tipico no service:
 * <pre>{@code
 * return PaginaResponse.de(repositorio.findAll(paginacao), AvisoResponse::de);
 * }</pre>
 */
public record PaginaResponse<T>(List<T> content, PaginaMetadata page) {

    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PaginaResponse<>(
                pagina.getContent().stream().map(conversor).toList(),
                PaginaMetadata.de(pagina));
    }
}
