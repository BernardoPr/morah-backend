package org.morah.morah.portaria.modelo;

import java.time.Duration;
import java.time.Instant;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "autorizacoes_visita" (schema AutorizacaoVisita do contrato).
 *
 * <p>Nasce quando o porteiro registra um visitante: fica PENDENTE ate o morador responder
 * (AUTORIZADA ou RECUSADA) ou ate o prazo acabar (EXPIRADA). As regras de "o que pode acontecer
 * em cada status" ficam nas classes do pacote {@code portaria.state} (padrao State).
 *
 * <p><b>Expiracao preguicosa:</b> nao existe um agendador que varre o banco marcando as vencidas.
 * Uma PENDENTE com {@code expiraEm} no passado ja e tratada como EXPIRADA nas respostas
 * ({@link #statusEfetivo}) e so e gravada como EXPIRADA quando alguem tenta decidir, reenviar ou
 * usar a autorizacao. Menos peca rodando, mesmo resultado para quem consulta.
 */
@Getter
@Setter
@Document(collection = "autorizacoes_visita")
public class AutorizacaoVisita extends EntidadeBase {

    /** Quanto tempo o morador tem para responder (contado do registro ou do ultimo reenvio). */
    public static final Duration PRAZO_DE_RESPOSTA = Duration.ofMinutes(30);

    @Indexed
    private Long condominioId;

    @Indexed
    private Long unidadeId;

    /** Ex.: "Bloco A - Apto 101". Copiado para montar as notificacoes sem consultar a unidade. */
    private String unidadeDescricao;

    @Indexed
    private Long visitanteId;

    /** Copia dos dados do visitante no momento do registro (ver {@link DadosDoVisitante}). */
    private DadosDoVisitante visitante;

    private String motivo;
    private String fotoUrl;

    private StatusAutorizacaoVisita status = StatusAutorizacaoVisita.PENDENTE;

    private Instant solicitadoEm;
    private Instant expiraEm;
    private Instant respondidoEm;

    /** Texto opcional do morador ao autorizar/recusar. */
    private String justificativa;

    private Long respondidoPorId;
    private String respondidoPorNome;

    /** Porteiro que registrou o visitante. */
    private Long registradoPorId;
    private String registradoPorNome;

    /** Quando o visitante entrou usando esta autorizacao (cada autorizacao vale para uma entrada). */
    private Instant entradaEm;

    /** Abre (ou renova, no reenvio) a janela de resposta do morador. */
    public void abrirPrazoDeResposta(Instant agora) {
        this.expiraEm = agora.plus(PRAZO_DE_RESPOSTA);
    }

    /** Pendente cujo prazo de resposta ja passou (ainda nao gravada como EXPIRADA). */
    public boolean estaVencida(Instant agora) {
        return status == StatusAutorizacaoVisita.PENDENTE
                && expiraEm != null
                && !agora.isBefore(expiraEm);
    }

    /** Status que a API mostra: a pendente vencida ja aparece como EXPIRADA. */
    public StatusAutorizacaoVisita statusEfetivo(Instant agora) {
        return estaVencida(agora) ? StatusAutorizacaoVisita.EXPIRADA : status;
    }
}
