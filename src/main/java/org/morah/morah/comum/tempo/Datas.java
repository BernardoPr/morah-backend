package org.morah.morah.comum.tempo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Datas no fuso do condominio.
 *
 * <p>O servidor no Azure roda em UTC; sem isso, depois das 21h o "hoje" do servidor ja seria
 * o dia seguinte para quem esta no Brasil. Use estes metodos sempre que a regra falar em
 * "hoje", "reservas do dia", "recebidas hoje"...
 */
public final class Datas {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Datas() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    /** Primeiro instante do dia (inclusivo). */
    public static Instant inicioDoDia(LocalDate data) {
        return data.atStartOfDay(FUSO).toInstant();
    }

    /** Primeiro instante do dia seguinte (exclusivo): use {@code >= inicio} e {@code < fim}. */
    public static Instant fimDoDia(LocalDate data) {
        return data.plusDays(1).atStartOfDay(FUSO).toInstant();
    }

    /** Dia (no fuso do condominio) em que o instante aconteceu. */
    public static LocalDate dataDe(Instant instante) {
        return LocalDate.ofInstant(instante, FUSO);
    }
}
