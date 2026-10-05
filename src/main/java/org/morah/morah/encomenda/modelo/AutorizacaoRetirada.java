package org.morah.morah.encomenda.modelo;

import java.time.Instant;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "autorizacoes_retirada" (schema AutorizacaoRetirada do contrato).
 *
 * <p>O morador que nao pode ir a portaria autoriza outra pessoa (vizinho, parente, diarista)
 * a retirar a encomenda. A autorizacao tem um codigo proprio, diferente do codigo do morador,
 * e uma validade em horas: assim o morador nunca precisa repassar o proprio codigo.
 */
@Getter
@Setter
@Document(collection = "autorizacoes_retirada")
public class AutorizacaoRetirada extends EntidadeBase {

    @Indexed
    private Long condominioId;

    private Long unidadeId;

    @Indexed
    private Long encomendaId;

    private String nomeTerceiro;
    private String documento;

    /** Codigo de 6 digitos que o terceiro apresenta na portaria. */
    private String codigo;

    /** Ate quando o codigo vale. */
    private Instant validade;

    private StatusAutorizacaoRetirada status = StatusAutorizacaoRetirada.ATIVA;

    /** Morador/proprietario que criou a autorizacao. */
    private Long criadaPorId;

    private Instant utilizadaEm;

    /** A autorizacao ainda pode ser usada? (ativa e dentro da validade) */
    public boolean valeEm(Instant agora) {
        return status == StatusAutorizacaoRetirada.ATIVA && validade != null && agora.isBefore(validade);
    }

    /**
     * Status que o usuario deve ver: uma autorizacao "ativa" com a validade vencida ja e
     * expirada, mesmo que ninguem tenha atualizado o banco.
     */
    public StatusAutorizacaoRetirada statusEm(Instant agora) {
        if (status == StatusAutorizacaoRetirada.ATIVA && !valeEm(agora)) {
            return StatusAutorizacaoRetirada.EXPIRADA;
        }
        return status;
    }
}
