package org.morah.morah.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.morah.morah.seguranca.singleton.RegistroDeTokensRevogadosSingleton;

class RegistroDeTokensRevogadosSingletonTest {

    @Test
    void tokenRevogadoEmUmLugarEhVistoNoOutro() {
        RegistroDeTokensRevogadosSingleton.getInstancia().revogar("token-do-logout");

        // Outra parte do sistema pede a instancia e enxerga a mesma lista.
        boolean revogado = RegistroDeTokensRevogadosSingleton.getInstancia().estaRevogado("token-do-logout");

        assertThat(revogado).isTrue();
        assertThat(RegistroDeTokensRevogadosSingleton.getInstancia().estaRevogado("outro-token")).isFalse();
    }
}
