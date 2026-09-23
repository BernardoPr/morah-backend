package org.morah.morah.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.dashboard.dto.DashboardPortariaResponse;
import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.dashboard.strategy.DashboardPortariaStrategy;
import org.morah.morah.dashboard.strategy.DashboardStrategy;
import org.morah.morah.dashboard.strategy.SeletorDeDashboard;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;

/**
 * Mostra o Strategy escolhendo o algoritmo certo sem nenhum if/else no controller.
 * Usamos uma estrategia "de mentira" para nao precisar do banco no teste.
 */
class SeletorDeDashboardTest {

    /** Estrategia falsa de morador (e tambem de proprietario). */
    private static class DashboardMoradorFake implements DashboardStrategy {

        @Override
        public Perfil perfilAtendido() {
            return Perfil.MORADOR;
        }

        @Override
        public boolean atende(Perfil perfil) {
            return perfil == Perfil.MORADOR || perfil == Perfil.PROPRIETARIO;
        }

        @Override
        public DashboardResponse montar(UsuarioAutenticado usuario) {
            return () -> "morador";
        }
    }

    private final SeletorDeDashboard seletor =
            new SeletorDeDashboard(List.of(new DashboardMoradorFake(), new DashboardPortariaStrategy()));

    @Test
    void escolheAEstrategiaDoPerfil() {
        var usuario = new UsuarioAutenticado(1L, "Joao", "11111111111",
                Perfil.PORTARIA, 1L, "Residencial Morah", null, "jti");

        DashboardResponse resposta = seletor.obter(Perfil.PORTARIA).montar(usuario);

        assertThat(resposta).isInstanceOf(DashboardPortariaResponse.class);
        assertThat(resposta.perfil()).isEqualTo("portaria");
    }

    @Test
    void proprietarioUsaAEstrategiaDoMorador() {
        assertThat(seletor.obter(Perfil.PROPRIETARIO)).isInstanceOf(DashboardMoradorFake.class);
    }
}
