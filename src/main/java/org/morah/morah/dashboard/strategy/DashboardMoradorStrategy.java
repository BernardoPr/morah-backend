package org.morah.morah.dashboard.strategy;

import java.util.List;

import org.morah.morah.aviso.servico.AvisoService;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.dashboard.dto.DashboardMoradorResponse;
import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.encomenda.servico.EncomendaService;
import org.morah.morah.financeiro.servico.CobrancaService;
import org.morah.morah.portaria.servico.AutorizacaoVisitaService;
import org.morah.morah.reserva.servico.ReservaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.servico.UnidadeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Tela inicial de morador e proprietario: tudo o que importa para a unidade do token.
 *
 * <p>Cada lista vem do servico do seu modulo (financeiro, encomendas, portaria, reservas);
 * a estrategia so junta as partes, sem repetir nenhuma regra de negocio.
 */
@Component
@RequiredArgsConstructor
public class DashboardMoradorStrategy implements DashboardStrategy {

    private static final int ITENS_POR_LISTA = 5;

    private final AvisoService avisoService;
    private final UnidadeService unidadeService;
    private final CobrancaService cobrancaService;
    private final EncomendaService encomendaService;
    private final AutorizacaoVisitaService autorizacaoVisitaService;
    private final ReservaService reservaService;

    @Override
    public Perfil perfilAtendido() {
        return Perfil.MORADOR;
    }

    /** O contrato define que proprietario ve tudo que o morador ve. */
    @Override
    public boolean atende(Perfil perfil) {
        return perfil == Perfil.MORADOR || perfil == Perfil.PROPRIETARIO;
    }

    @Override
    public DashboardResponse montar(UsuarioAutenticado usuario) {
        var avisos = avisoService.listarVisiveis(usuario, null,
                PageRequest.of(0, ITENS_POR_LISTA, Sort.by(Sort.Direction.DESC, "publicadoEm"))).content();

        Long unidadeId = usuario.unidadeId();
        if (unidadeId == null) {
            // Vinculo de morador sem unidade e um cadastro incompleto: mostra so os avisos.
            return new DashboardMoradorResponse(usuario.perfil().getValor(), null,
                    List.of(), avisos, List.of(), List.of(), List.of());
        }

        return new DashboardMoradorResponse(
                usuario.perfil().getValor(),
                unidadeService.buscarResposta(unidadeId),
                cobrancaService.proximasDaUnidade(unidadeId, ITENS_POR_LISTA),
                avisos,
                encomendaService.pendentesDaUnidade(unidadeId, ITENS_POR_LISTA),
                autorizacaoVisitaService.pendentesDaUnidade(unidadeId, ITENS_POR_LISTA),
                reservaService.proximasDaUnidade(unidadeId, ITENS_POR_LISTA));
    }
}
