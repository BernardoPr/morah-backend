package org.morah.morah.comum.sequencia;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Gera ids numericos sequenciais (1, 2, 3...) para as colecoes.
 *
 * <p>O MongoDB nao tem "auto increment", entao guardamos um contador por colecao
 * e incrementamos com uma unica operacao atomica ($inc), o que evita ids repetidos
 * mesmo com varias requisicoes ao mesmo tempo.
 */
@Component
@RequiredArgsConstructor
public class GeradorDeIdSequencial {

    private final MongoOperations mongo;

    public long proximoId(String colecao) {
        Sequencia sequencia = mongo.findAndModify(
                new Query(Criteria.where("_id").is(colecao)),
                new Update().inc("valor", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                Sequencia.class);

        return sequencia == null ? 1L : sequencia.getValor();
    }
}
