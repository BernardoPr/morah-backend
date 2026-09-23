package org.morah.morah.comum.dto;

import org.springframework.data.domain.Page;

/** Bloco "page" das respostas paginadas (schema PageMetadata do contrato). */
public record PaginaMetadata(int page, int size, long totalElements, int totalPages) {

    public static PaginaMetadata de(Page<?> pagina) {
        return new PaginaMetadata(
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
