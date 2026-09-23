package org.morah.morah.comum.sequencia;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Antes de salvar qualquer {@link EntidadeBase} sem id, pede o proximo numero
 * ao {@link GeradorDeIdSequencial}. Assim nenhum service precisa se preocupar com isso.
 */
@Component
@RequiredArgsConstructor
public class OuvinteDeIdSequencial extends AbstractMongoEventListener<EntidadeBase> {

    private final GeradorDeIdSequencial gerador;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<EntidadeBase> evento) {
        EntidadeBase entidade = evento.getSource();
        if (entidade.getId() == null) {
            entidade.setId(gerador.proximoId(evento.getCollectionName()));
        }
    }
}
