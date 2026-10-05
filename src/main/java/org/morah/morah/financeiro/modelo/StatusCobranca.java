package org.morah.morah.financeiro.modelo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Schema StatusCobranca do contrato.
 *
 * <p><b>Atencao:</b> {@link #ATRASADO} nunca e gravado no banco. "Atrasado" depende do dia de
 * hoje, entao e <i>derivado</i> na leitura: uma cobranca {@link #PENDENTE} com vencimento antes
 * de hoje e exibida como atrasada (veja {@code CalculadoraDeEncargos}). Se gravassemos o atraso,
 * precisariamos de um job rodando todo dia so para trocar o status - e ele poderia falhar.
 */
public enum StatusCobranca {

    PENDENTE("pendente"),
    PAGO("pago"),
    /** Somente exibicao: nunca e gravado (veja o Javadoc da classe). */
    ATRASADO("atrasado"),
    CANCELADO("cancelado");

    private final String valor;

    StatusCobranca(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static StatusCobranca de(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
