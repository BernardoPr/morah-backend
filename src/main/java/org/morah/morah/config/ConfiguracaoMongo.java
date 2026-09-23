package org.morah.morah.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Liga a auditoria do Spring Data: os campos {@code criadoEm} e {@code atualizadoEm}
 * da {@code EntidadeBase} passam a ser preenchidos sozinhos.
 */
@Configuration
@EnableMongoAuditing
public class ConfiguracaoMongo {
}
