package org.morah.morah.portaria.dto;

import java.time.Instant;

import org.morah.morah.portaria.modelo.Acesso;
import org.morah.morah.portaria.modelo.StatusAcesso;
import org.morah.morah.portaria.modelo.TipoAcesso;

/**
 * Resposta do contrato (schema Acesso).
 *
 * <p>{@code tipo} = ultimo movimento registrado ("entrada" ou "saida");
 * {@code status} = "em_andamento" enquanto o visitante esta dentro, "encerrado" depois da saida.
 */
public record AcessoResponse(
        Long id,
        Long autorizacaoId,
        VisitanteResponse visitante,
        Instant entrada,
        Instant saida,
        TipoAcesso tipo,
        StatusAcesso status) {

    public static AcessoResponse de(Acesso acesso) {
        return new AcessoResponse(
                acesso.getId(),
                acesso.getAutorizacaoId(),
                VisitanteResponse.de(acesso.getVisitanteId(), acesso.getVisitante()),
                acesso.getEntrada(),
                acesso.getSaida(),
                acesso.getTipo(),
                acesso.getStatus());
    }
}
