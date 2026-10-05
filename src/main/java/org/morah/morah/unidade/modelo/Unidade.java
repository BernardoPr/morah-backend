package org.morah.morah.unidade.modelo;

import java.math.BigDecimal;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "unidades" (schema Unidade do contrato): apartamentos, casas ou salas do condominio.
 *
 * <p>E usada por varios modulos: o financeiro rateia as taxas entre as unidades, a portaria e
 * as encomendas avisam os moradores da unidade, as reservas guardam qual unidade reservou.
 *
 * <p>O bloco fica embutido aqui ({@code blocoId}/{@code blocoNome}): o contrato nao tem
 * endpoints proprios de bloco, entao nao vale uma colecao so para ele.
 */
@Getter
@Setter
@Document(collection = "unidades")
public class Unidade extends EntidadeBase {

    @Indexed
    private Long condominioId;

    private Long blocoId;
    private String blocoNome;

    /** Ex.: "Apto 302". */
    private String identificacao;

    /** Ex.: "apartamento", "casa", "sala". */
    private String tipo;

    private BigDecimal areaM2;

    /** Parte da unidade no condominio (0 a 1). Usada no rateio por fracao ideal. */
    private BigDecimal fracaoIdeal;

    /** Ex.: "ativa", "inativa". */
    private String status = "ativa";
}
