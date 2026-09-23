package org.morah.morah;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe o contexto do Spring para conferir se toda a configuracao (beans, seguranca,
 * repositorios) esta coerente. A carga inicial fica desligada para o teste nao depender
 * de um MongoDB rodando.
 */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
class MorahApplicationTests {

    @Test
    void contextoSobe() {
    }
}
