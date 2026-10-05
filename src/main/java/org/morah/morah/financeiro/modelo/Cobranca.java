package org.morah.morah.financeiro.modelo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "cobrancas" (schema Cobranca do contrato): o "boleto" de uma unidade para uma taxa.
 *
 * <p>Repare no que NAO esta gravado: multa, juros, valor total e o status "atrasado". Tudo isso
 * muda com o passar dos dias, entao e calculado na leitura pela {@code CalculadoraDeEncargos}.
 * Aqui fica so o que e fato: quanto custava, quando vence e, se ja foi paga, como foi paga.
 *
 * <p>O indice composto unico (taxaId + unidadeId) garante, no proprio banco, que uma unidade
 * nunca recebe duas cobrancas da mesma taxa - mesmo se o sindico clicar duas vezes em "gerar".
 */
@Getter
@Setter
@Document(collection = "cobrancas")
@CompoundIndex(name = "uma_cobranca_por_taxa_e_unidade", def = "{'taxaId': 1, 'unidadeId': 1}", unique = true)
public class Cobranca extends EntidadeBase {

    @Indexed
    private Long condominioId;

    @Indexed
    private Long unidadeId;

    private Long taxaId;

    /** Copiada da taxa para a listagem nao precisar consultar outra colecao. */
    private String taxaDescricao;

    private String competencia;
    private LocalDate vencimento;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorOriginal;

    /** Somente PENDENTE, PAGO ou CANCELADO (ATRASADO e derivado, nunca gravado). */
    private StatusCobranca status = StatusCobranca.PENDENTE;

    // ---------- dados do pagamento (preenchidos na baixa manual ou pelo webhook) ----------

    private Instant pagoEm;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal valorPago;

    /** Multa devida no dia do pagamento, "congelada" para o historico nao mudar depois. */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal multaPaga;

    /** Juros devidos no dia do pagamento, "congelados" para o historico nao mudar depois. */
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal jurosPagos;

    /** Ex.: "pix", "transferencia", "dinheiro". */
    private String meio;
    private String observacao;

    /**
     * Id da transacao no gateway. Unico (e esparso: so entra no indice quem tem o campo), o que
     * impede no banco que a mesma transacao quite duas cobrancas.
     */
    @Indexed(unique = true, sparse = true)
    private String transacaoId;

    /** Sindico que registrou a baixa manual (nulo quando veio do gateway). */
    private Long baixadoPorId;

    // ---------- PIX copia-e-cola ----------

    private String codigoPix;
    private Instant pixExpiraEm;
}
