package org.morah.morah.portaria.modelo;

import java.time.Instant;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "acessos" (schema Acesso do contrato): o livro de entradas e saidas da portaria.
 *
 * <p>Um documento por visita: o registro de ENTRADA cria o documento e o de SAIDA completa o
 * mesmo documento (preenche {@code saida}). Assim o historico mostra, em uma linha, quanto tempo
 * o visitante ficou, e "quem esta dentro agora" e simplesmente {@code saida == null}.
 */
@Getter
@Setter
@Document(collection = "acessos")
public class Acesso extends EntidadeBase {

    @Indexed
    private Long condominioId;

    private Long unidadeId;

    /** Autorizacao usada na entrada. */
    private Long autorizacaoId;

    @Indexed
    private Long visitanteId;

    /** Copia dos dados do visitante no dia da visita. */
    private DadosDoVisitante visitante;

    private Instant entrada;
    private Instant saida;

    /** Ultimo movimento registrado neste acesso. */
    private TipoAcesso tipo;

    private StatusAcesso status;

    private String fotoEntradaUrl;
    private String fotoSaidaUrl;

    /** Porteiros que registraram cada movimento. */
    private Long entradaRegistradaPorId;
    private String entradaRegistradaPorNome;
    private Long saidaRegistradaPorId;
    private String saidaRegistradaPorNome;
}
