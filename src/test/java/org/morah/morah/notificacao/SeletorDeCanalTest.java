package org.morah.morah.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.strategy.CanalEmailStrategy;
import org.morah.morah.notificacao.strategy.CanalPushStrategy;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;

/** O seletor entrega a strategy do canal pedido, sem nenhum if/else por canal. */
class SeletorDeCanalTest {

    private final SeletorDeCanal seletor =
            new SeletorDeCanal(List.of(new CanalEmailStrategy(), new CanalPushStrategy()));

    @Test
    void entregaAEstrategiaDoCanal() {
        assertThat(seletor.obter(CanalNotificacao.EMAIL)).isInstanceOf(CanalEmailStrategy.class);
        assertThat(seletor.obter(CanalNotificacao.PUSH)).isInstanceOf(CanalPushStrategy.class);
    }

    @Test
    void canalSemEstrategiaCadastradaFalha() {
        assertThatThrownBy(() -> seletor.obter(CanalNotificacao.SMS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMS");
    }
}
