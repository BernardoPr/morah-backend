package org.morah.morah.ocorrencia.modelo;

import java.time.Instant;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "ocorrencias" (schema Ocorrencia do contrato).
 *
 * <p>Compartilhada por dois modulos: Encomendas (extravio) e Reservas (dano na vistoria).
 * O sindico ve o total de abertas na tela inicial.
 */
@Getter
@Setter
@Document(collection = "ocorrencias")
public class Ocorrencia extends EntidadeBase {

    @Indexed
    private Long condominioId;

    private Long unidadeId;

    private OrigemOcorrencia origem;

    /** Id da encomenda ou da reserva que originou a ocorrencia. */
    private Long referenciaId;

    private String titulo;
    private String descricao;
    private StatusOcorrencia status = StatusOcorrencia.ABERTA;

    private Long abertaPorId;
    private String abertaPorNome;

    private Instant abertaEm;
    private Instant encerradaEm;
}
