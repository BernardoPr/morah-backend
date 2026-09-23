package org.morah.morah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada da API Morah.
 *
 * <p>Comece por aqui: {@code ARQUITETURA.md} (na raiz) explica a estrutura de pastas e
 * onde cada padrao de projeto foi aplicado.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MorahApplication {

    public static void main(String[] args) {
        SpringApplication.run(MorahApplication.class, args);
    }
}
