package org.morah.morah.encomenda.modelo;

import java.time.Instant;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "encomendas" (schema Encomenda do contrato): pacote recebido pela portaria e
 * guardado ate alguem da unidade (ou um terceiro autorizado) retirar.
 *
 * <p>Alem dos campos do contrato, guardamos QUEM recebeu e QUEM entregou o pacote: se o
 * morador reclamar depois, o sindico sabe com qual porteiro falar.
 *
 * <p>O {@code codigoRetirada} e o "comprovante" do morador. Ele so aparece nas respostas para
 * a propria unidade (veja {@code EncomendaResponse.de}); a portaria nunca o ve, senao conferir o
 * codigo no balcao nao provaria nada.
 */
@Getter
@Setter
@Document(collection = "encomendas")
public class Encomenda extends EntidadeBase {

    @Indexed
    private Long condominioId;

    @Indexed
    private Long unidadeId;

    private String remetente;
    private String transportadora;
    private String codigoRastreio;
    private String fotoUrl;

    /** Codigo de 6 digitos que o morador apresenta na portaria para retirar. */
    private String codigoRetirada;

    private StatusEncomenda status = StatusEncomenda.AGUARDANDO_RETIRADA;

    // ---------- recebimento (portaria) ----------

    private Instant recebidaEm;
    private Long recebidaPorId;
    private String recebidaPorNome;

    // ---------- retirada ----------

    private Instant retiradaEm;
    private String retiradaPorNome;
    private String retiradaPorDocumento;

    /** Preenchido quando a retirada foi feita com o codigo de uma autorizacao de terceiro. */
    private Long autorizacaoRetiradaId;

    /** Porteiro que entregou o pacote. */
    private Long entreguePorId;
    private String entreguePorNome;

    // ---------- extravio ----------

    /** Ultima ocorrencia aberta para esta encomenda (extravio ou contestacao). */
    private Long ocorrenciaId;
}
