package org.morah.morah.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.BigDecimalRepresentation;

/**
 * Configuracoes do MongoDB.
 *
 * <ul>
 *   <li>Liga a auditoria do Spring Data: os campos {@code criadoEm} e {@code atualizadoEm}
 *       da {@code EntidadeBase} passam a ser preenchidos sozinhos.</li>
 *   <li>Define como datas e valores sao gravados (veja {@link #mongoCustomConversions()}).</li>
 * </ul>
 */
@Configuration
@EnableMongoAuditing
public class ConfiguracaoMongo {

    /**
     * <b>Datas ({@code LocalDate}, {@code LocalTime}...):</b> usamos os codecs do proprio driver,
     * que gravam sempre em UTC. O padrao do Spring Data converte pelo fuso da maquina: um
     * vencimento gravado pelo servidor do Azure (UTC) apareceria um dia antes para quem roda a
     * API no Brasil (UTC-3) apontando para o mesmo banco do Atlas.
     *
     * <p><b>Valores ({@code BigDecimal}):</b> gravados como Decimal128, o tipo decimal exato do
     * MongoDB (e nao como texto), para que somas e comparacoes no banco funcionem com dinheiro.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(conversoes -> conversoes
                .useNativeDriverJavaTimeCodecs()
                .bigDecimal(BigDecimalRepresentation.DECIMAL128));
    }
}
