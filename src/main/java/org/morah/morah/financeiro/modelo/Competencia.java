package org.morah.morah.financeiro.modelo;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A "competencia" e o mes de referencia de uma taxa ou cobranca, no formato {@code AAAA-MM}
 * (ex.: "2026-09"). Fica gravada como texto porque e assim que o contrato a troca com o front.
 *
 * <p>Classe utilitaria para nao espalhar a mesma regex e o mesmo calculo de vencimento.
 */
public final class Competencia {

    /**
     * Formato aceito. E um pouco mais rigoroso que o do contrato ({@code ^\d{4}-\d{2}$}): recusa
     * meses como "13", que passariam pela regex do contrato e quebrariam o calculo do vencimento.
     */
    public static final String FORMATO = "^\\d{4}-(0[1-9]|1[0-2])$";

    public static final String MENSAGEM_DE_FORMATO = "informe a competencia no formato AAAA-MM (ex.: 2026-09)";

    private Competencia() {
    }

    /** Competencia de um mes (ex.: outubro de 2026 vira "2026-10"). */
    public static String de(YearMonth mes) {
        return mes.toString();
    }

    /** Dia {@code dia} do mes da competencia (ex.: "2026-10" + 10 = 10/10/2026). */
    public static LocalDate vencimento(String competencia, int dia) {
        return YearMonth.parse(competencia).atDay(dia);
    }
}
