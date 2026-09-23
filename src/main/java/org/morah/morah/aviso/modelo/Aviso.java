package org.morah.morah.aviso.modelo;

import java.time.Instant;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "avisos" (mural do condominio).
 *
 * <p>Segundo modelo de banco do projeto - serve de molde para as demais entidades
 * (cobrancas, reservas, encomendas...).
 */
@Getter
@Setter
@Document(collection = "avisos")
public class Aviso extends EntidadeBase {

    @Indexed
    private Long condominioId;

    private String titulo;
    private String conteudo;
    private PrioridadeAviso prioridade = PrioridadeAviso.NORMAL;
    private PublicoAlvo publicoAlvo;

    private Long autorId;
    private String autorNome;

    private Instant publicadoEm;
    private Instant expiraEm;
}
