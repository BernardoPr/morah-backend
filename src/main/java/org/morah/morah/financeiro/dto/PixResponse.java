package org.morah.morah.financeiro.dto;

import java.time.Instant;

/** Resposta do contrato (schema PixResponse): o codigo copia-e-cola e ate quando ele vale. */
public record PixResponse(String codigoPix, Instant expiraEm) {
}
