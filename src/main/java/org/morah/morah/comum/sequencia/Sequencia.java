package org.morah.morah.comum.sequencia;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

/**
 * Uma linha da colecao "sequencias": guarda o ultimo id usado por colecao.
 * Ex.: { "_id": "usuarios", "valor": 7 }
 */
@Data
@Document(collection = "sequencias")
public class Sequencia {

    /** Nome da colecao (ex.: "usuarios"). */
    @Id
    private String id;

    private long valor;
}
