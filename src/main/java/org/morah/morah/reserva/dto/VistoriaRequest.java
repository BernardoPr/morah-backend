package org.morah.morah.reserva.dto;

import java.util.List;

import org.hibernate.validator.constraints.URL;
import org.morah.morah.reserva.modelo.MomentoVistoria;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo do POST /reservas/{reservaId}/vistoria (schema VistoriaRequest).
 *
 * <p>{@code dano} e {@code Boolean} (e nao {@code boolean}) para aceitar o campo ausente:
 * o contrato define {@code default: false}, tratado em {@link #houveDano()}.
 */
public record VistoriaRequest(
        @NotNull(message = "informe o momento da vistoria (entrada ou saida)") MomentoVistoria momento,
        String observacao,
        Boolean dano,
        List<@URL(message = "informe uma URL valida") String> fotosUrl) {

    public boolean houveDano() {
        return Boolean.TRUE.equals(dano);
    }
}
