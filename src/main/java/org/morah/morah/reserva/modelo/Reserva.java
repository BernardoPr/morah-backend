package org.morah.morah.reserva.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "reservas" (schema Reserva do contrato).
 *
 * <p>Alguns dados sao copiados no momento da reserva, de proposito:
 * <ul>
 *   <li>{@code areaComumNome} e o solicitante: a listagem nao precisa consultar outras colecoes;</li>
 *   <li>{@code valor}: e a taxa da area NAQUELE momento - se o sindico mudar a taxa depois,
 *       as reservas ja feitas continuam com o preco combinado.</li>
 * </ul>
 *
 * <p>Cancelar nao apaga o documento: o status vira CANCELADA e {@code canceladaEm} e preenchido
 * (o historico continua disponivel para o sindico).
 */
@Getter
@Setter
@Document(collection = "reservas")
public class Reserva extends EntidadeBase {

    @Indexed
    private Long condominioId;

    @Indexed
    private Long areaComumId;
    private String areaComumNome;

    @Indexed
    private Long unidadeId;

    private Long solicitanteId;
    private String solicitanteNome;
    private String solicitanteTelefone;

    private Instant inicio;
    private Instant fim;

    private StatusReserva status = StatusReserva.CONFIRMADA;

    private BigDecimal valor;

    private Instant canceladaEm;

    private List<Vistoria> vistorias = new ArrayList<>();

    /** O inicio ja chegou (ou passou)? Depois disso a reserva nao pode mais ser cancelada. */
    public boolean jaComecou(Instant agora) {
        return !inicio.isAfter(agora);
    }

    public boolean temVistoria(MomentoVistoria momento) {
        return vistorias.stream().anyMatch(vistoria -> vistoria.getMomento() == momento);
    }
}
