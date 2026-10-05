package org.morah.morah.dashboard.strategy;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.dashboard.dto.DashboardPortariaResponse;
import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.encomenda.servico.EncomendaService;
import org.morah.morah.portaria.servico.AutorizacaoVisitaService;
import org.morah.morah.reserva.servico.ReservaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Tela inicial da portaria: painel operacional do dia. */
@Component
@RequiredArgsConstructor
public class DashboardPortariaStrategy implements DashboardStrategy {

    private static final int QUANTIDADE_NA_FILA = 10;

    private final AutorizacaoVisitaService autorizacaoVisitaService;
    private final ReservaService reservaService;
    private final EncomendaService encomendaService;

    @Override
    public Perfil perfilAtendido() {
        return Perfil.PORTARIA;
    }

    @Override
    public DashboardResponse montar(UsuarioAutenticado usuario) {
        Long condominioId = usuario.condominioId();

        return new DashboardPortariaResponse(
                usuario.perfil().getValor(),
                autorizacaoVisitaService.pendentesDoCondominio(condominioId, QUANTIDADE_NA_FILA),
                reservaService.doDia(condominioId, Datas.hoje()),
                encomendaService.contarRecebidasHoje(condominioId));
    }
}
