package org.morah.morah.reserva.modelo;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "areas_comuns" (schema AreaComum do contrato): salao de festas, churrasqueira, quadra...
 *
 * <p>Alem dos campos do contrato, a area guarda a sua "agenda": o horario de funcionamento e o
 * tamanho de cada horario reservavel ({@code duracaoSlotMinutos}). E com esses tres campos que a
 * API monta a disponibilidade do dia e confere se uma reserva cabe no funcionamento.
 *
 * <p><b>Por que as horas sao texto ("10:00") e nao {@code LocalTime}?</b> O MongoDB nao tem um
 * tipo "so hora": o Spring Data gravaria o {@code LocalTime} como uma data completa, convertida
 * pelo fuso da maquina que gravou. Se a carga inicial rodasse num computador em -03:00 e a API
 * lesse o banco no Azure (UTC), as 10:00 virariam 13:00. Texto no formato ISO ({@code HH:mm})
 * nao depende de fuso e ainda fica legivel no Atlas. Os metodos {@link #abertura()} e
 * {@link #fechamento()} devolvem o valor ja convertido para {@code LocalTime}.
 */
@Getter
@Setter
@Document(collection = "areas_comuns")
public class AreaComum extends EntidadeBase {

    public static final String ATIVA = "ativa";
    public static final String EM_MANUTENCAO = "manutencao";

    @Indexed
    private Long condominioId;

    private String nome;
    private String descricao;
    private Integer capacidade;

    /** Valor cobrado por reserva (copiado para a reserva no momento em que ela e feita). */
    private BigDecimal taxa = BigDecimal.ZERO;

    /** {@code false} para areas de uso livre (ex.: academia): aparecem na lista, mas nao sao reservaveis. */
    private boolean requerReserva = true;

    /** "ativa" ou "manutencao" (o contrato declara o campo apenas como string). */
    private String status = ATIVA;

    /** Hora local (fuso do condominio) em que a area abre, no formato HH:mm. */
    private String horaAbertura;

    /** Hora local (fuso do condominio) em que a area fecha, no formato HH:mm. */
    private String horaFechamento;

    /** Tamanho de cada horario oferecido na disponibilidade (ex.: 240 = blocos de 4 horas). */
    private Integer duracaoSlotMinutos;

    // ---------- regras simples da propria area ----------

    public boolean estaAtiva() {
        return ATIVA.equals(status);
    }

    /** So aceita reserva a area que esta ativa e que exige reserva. */
    public boolean aceitaReservas() {
        return estaAtiva() && requerReserva;
    }

    public LocalTime abertura() {
        return horaAbertura == null ? null : LocalTime.parse(horaAbertura);
    }

    public LocalTime fechamento() {
        return horaFechamento == null ? null : LocalTime.parse(horaFechamento);
    }

    /** Horario de funcionamento preenchido e coerente (abre e fecha no mesmo dia). */
    public boolean temHorarioDeFuncionamento() {
        LocalTime abertura = abertura();
        LocalTime fechamento = fechamento();
        return abertura != null && fechamento != null && fechamento.isAfter(abertura);
    }
}
