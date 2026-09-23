package org.morah.morah.dashboard.strategy;

import java.util.List;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.dashboard.dto.DashboardPortariaResponse;
import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.stereotype.Component;

/** Tela inicial da portaria: painel operacional do dia. */
@Component
public class DashboardPortariaStrategy implements DashboardStrategy {

    @Override
    public Perfil perfilAtendido() {
        return Perfil.PORTARIA;
    }

    @Override
    public DashboardResponse montar(UsuarioAutenticado usuario) {
        // Sera preenchido pelos modulos de Portaria e Encomendas.
        return new DashboardPortariaResponse(usuario.perfil().getValor(), List.of(), List.of(), 0);
    }
}
