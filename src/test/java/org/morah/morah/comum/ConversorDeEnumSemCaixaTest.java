package org.morah.morah.comum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.morah.morah.comum.web.ConversorDeEnumSemCaixa;
import org.morah.morah.ocorrencia.modelo.StatusOcorrencia;

/** Os parametros de URL chegam como no contrato ("em_andamento") e viram a constante Java. */
class ConversorDeEnumSemCaixaTest {

    private final ConversorDeEnumSemCaixa conversor = new ConversorDeEnumSemCaixa();

    @Test
    void aceitaOValorDoContratoEmMinusculo() {
        assertThat(conversor.getConverter(StatusOcorrencia.class).convert("em_andamento"))
                .isEqualTo(StatusOcorrencia.EM_ANDAMENTO);
        assertThat(conversor.getConverter(StatusOcorrencia.class).convert("ABERTA"))
                .isEqualTo(StatusOcorrencia.ABERTA);
    }

    @Test
    void valorDesconhecidoEhRecusado() {
        assertThatThrownBy(() -> conversor.getConverter(StatusOcorrencia.class).convert("talvez"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
