package org.morah.morah.financeiro.modelo;

import java.math.BigDecimal;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "taxas" (schema Taxa do contrato): um valor que o condominio precisa arrecadar em uma
 * competencia (taxa condominial, fundo de reserva, chamada extra...).
 *
 * <p>A taxa sozinha nao cobra ninguem: o sindico chama
 * {@code POST /financeiro/taxas/{taxaId}/gerar-cobrancas} e o valor e rateado entre as unidades,
 * gerando uma {@link Cobranca} para cada uma.
 */
@Getter
@Setter
@Document(collection = "taxas")
public class Taxa extends EntidadeBase {

    @Indexed
    private Long condominioId;

    private String descricao;

    /** Mes de referencia no formato AAAA-MM (veja {@link Competencia}). */
    private String competencia;

    /**
     * Valor TOTAL a ratear entre as unidades.
     * Gravado como Decimal128 (numero decimal exato do Mongo) e nao como texto, para permitir somas no banco.
     */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valor;

    private TipoRateio tipoRateio;

    /** Dia do vencimento (1 a 28, para existir em todos os meses). */
    private Integer vencimentoDia;
}
