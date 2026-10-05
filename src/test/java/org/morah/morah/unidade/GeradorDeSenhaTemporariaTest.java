package org.morah.morah.unidade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.morah.morah.unidade.servico.GeradorDeSenhaTemporaria;

class GeradorDeSenhaTemporariaTest {

    private final GeradorDeSenhaTemporaria gerador = new GeradorDeSenhaTemporaria();

    @Test
    void geraSenhasDe12CaracteresSemLetrasAmbiguas() {
        String senha = gerador.gerar();

        assertThat(senha).hasSize(12).matches("[A-HJ-NP-Za-km-np-z2-9]+");
        assertThat(gerador.gerar()).isNotEqualTo(senha);
    }
}
