package org.morah.morah.aviso.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Quem deve ver o aviso (schema PublicoAlvo do contrato). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicoAlvo {

    public enum Tipo {

        CONDOMINIO("condominio"),
        BLOCO("bloco"),
        UNIDADE("unidade");

        private final String valor;

        Tipo(String valor) {
            this.valor = valor;
        }

        @JsonValue
        public String getValor() {
            return valor;
        }

        @JsonCreator
        public static Tipo de(String valor) {
            return valueOf(valor.toUpperCase());
        }
    }

    @NotNull(message = "informe o tipo do publico alvo")
    private Tipo tipo;

    /** Obrigatorio quando o tipo e "bloco". */
    private Long blocoId;

    /** Obrigatorio quando o tipo e "unidade". */
    private Long unidadeId;
}
