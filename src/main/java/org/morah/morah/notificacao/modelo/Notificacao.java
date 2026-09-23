package org.morah.morah.notificacao.modelo;

import java.time.Instant;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/** Colecao "notificacoes" (schema Notificacao do contrato). */
@Getter
@Setter
@Document(collection = "notificacoes")
public class Notificacao extends EntidadeBase {

    @Indexed
    private Long destinatarioId;

    private TipoNotificacao tipo;
    private String titulo;
    private String mensagem;

    /** Id do registro que originou a notificacao (aviso, encomenda, reserva...). */
    private Long referenciaId;

    private CanalNotificacao canal;
    private Instant enviadaEm;
    private Instant lidaEm;
}
