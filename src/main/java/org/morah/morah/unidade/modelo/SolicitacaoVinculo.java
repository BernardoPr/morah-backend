package org.morah.morah.unidade.modelo;

import java.time.Instant;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "solicitacoes_vinculo" (schema SolicitacaoVinculo do contrato).
 *
 * <p>O morador nao inclui ninguem na unidade sozinho: ele pede, e o sindico aprova ou rejeita.
 * So depois da aprovacao nasce o {@link VinculoUnidade}. A solicitacao continua gravada depois
 * de decidida, para ficar o registro de quem pediu, quem decidiu e por que.
 */
@Getter
@Setter
@Document(collection = "solicitacoes_vinculo")
public class SolicitacaoVinculo extends EntidadeBase {

    @Indexed
    private Long condominioId;

    @Indexed
    private Long unidadeId;

    /** Copiado da unidade para montar as notificacoes sem outra consulta (ex.: "Apto 202"). */
    private String unidadeIdentificacao;

    private TipoVinculo tipoVinculo;
    private String nomeSolicitado;

    /** Somente digitos; opcional (ex.: dependente crianca sem CPF). */
    private String cpf;
    private String observacao;

    private StatusSolicitacaoVinculo status = StatusSolicitacaoVinculo.PENDENTE;
    private Instant solicitadoEm;

    private Long solicitanteId;
    private String solicitanteNome;

    private Instant decididoEm;
    private Long decididoPorId;
    private String decididoPorNome;
    private String justificativa;

    public boolean estaPendente() {
        return status == StatusSolicitacaoVinculo.PENDENTE;
    }
}
