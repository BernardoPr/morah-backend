package org.morah.morah.comum.modelo;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Getter;
import lombok.Setter;

/**
 * Campos que TODA colecao do banco tem.
 *
 * <p>Basta a sua entidade estender esta classe para ganhar:
 * <ul>
 *   <li>{@code id} numerico e sequencial (1, 2, 3...), gerado pelo
 *       {@link org.morah.morah.comum.sequencia.GeradorDeIdSequencial};</li>
 *   <li>{@code criadoEm} e {@code atualizadoEm} preenchidos automaticamente
 *       (auditoria ligada em {@code ConfiguracaoMongo}).</li>
 * </ul>
 *
 * <p>Usamos id numerico (e nao o ObjectId do Mongo) porque o contrato com o front
 * (Documentos/morah-api.yaml) declara todos os ids como {@code integer/int64}.
 */
@Getter
@Setter
public abstract class EntidadeBase {

    @Id
    private Long id;

    @CreatedDate
    private Instant criadoEm;

    @LastModifiedDate
    private Instant atualizadoEm;
}
