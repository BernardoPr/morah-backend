package org.morah.morah.comum.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Perfis do contrato (morah-api.yaml -> schema "Perfil").
 *
 * <p>O JSON usa o texto em minusculo ("morador"), enquanto o Java usa a constante
 * em maiusculo (MORADOR). O {@link JsonValue}/{@link JsonCreator} fazem essa traducao.
 */
public enum Perfil {

    MORADOR("morador"),
    PROPRIETARIO("proprietario"),
    SINDICO("sindico"),
    PORTARIA("portaria");

    private final String valor;

    Perfil(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    /** Nome usado pelo Spring Security (ex.: ROLE_SINDICO). */
    public String comoRole() {
        return "ROLE_" + name();
    }

    @JsonCreator
    public static Perfil de(String valor) {
        for (Perfil perfil : values()) {
            if (perfil.valor.equalsIgnoreCase(valor) || perfil.name().equalsIgnoreCase(valor)) {
                return perfil;
            }
        }
        throw new IllegalArgumentException("Perfil invalido: " + valor);
    }
}
