package org.morah.morah.reserva.modelo;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Schema StatusReserva do contrato.
 *
 * <p>Ciclo de vida: toda reserva nasce CONFIRMADA; o morador pode CANCELAR antes de comecar;
 * a vistoria de saida da portaria a marca como CONCLUIDA. NAO_COMPARECEU existe no contrato,
 * mas nenhuma rota atual chega nele (ficaria para uma rotina agendada).
 */
public enum StatusReserva {

    CONFIRMADA("confirmada"),
    CANCELADA("cancelada"),
    CONCLUIDA("concluida"),
    NAO_COMPARECEU("nao_compareceu");

    private final String valor;

    StatusReserva(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusReserva de(String valor) {
        return valueOf(valor.toUpperCase(Locale.ROOT));
    }
}
