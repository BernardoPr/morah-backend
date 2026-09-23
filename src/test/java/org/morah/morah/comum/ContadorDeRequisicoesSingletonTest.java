package org.morah.morah.comum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.comum.singleton.ContadorDeRequisicoesSingleton;

/** Mostra na pratica o que o padrao Singleton garante: existe UMA instancia so. */
class ContadorDeRequisicoesSingletonTest {

    @BeforeEach
    void limpar() {
        ContadorDeRequisicoesSingleton.getInstancia().zerar();
    }

    @Test
    @DisplayName("getInstancia sempre devolve o mesmo objeto")
    void instanciaUnica() {
        var primeira = ContadorDeRequisicoesSingleton.getInstancia();
        var segunda = ContadorDeRequisicoesSingleton.getInstancia();

        assertThat(primeira).isSameAs(segunda);
    }

    @Test
    @DisplayName("o placar e compartilhado por quem pedir a instancia")
    void placarCompartilhado() {
        ContadorDeRequisicoesSingleton.getInstancia().registrar("GET", "/avisos");
        ContadorDeRequisicoesSingleton.getInstancia().registrar("GET", "/avisos");
        ContadorDeRequisicoesSingleton.getInstancia().registrar("POST", "/auth/login");

        var contador = ContadorDeRequisicoesSingleton.getInstancia();

        assertThat(contador.getTotal()).isEqualTo(3);
        assertThat(contador.getTotaisPorRota()).containsEntry("GET /avisos", 2L);
    }
}
