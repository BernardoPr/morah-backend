package org.morah.morah.dashboard.strategy;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.aviso.repositorio.AvisoRepository;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.dashboard.dto.DashboardMoradorResponse;
import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Tela inicial de morador e proprietario.
 *
 * <p>As listas de boletos, encomendas, autorizacoes e reservas ficam vazias porque esses
 * modulos ainda nao foram implementados - o formato do JSON, porem, ja e o do contrato.
 */
@Component
@RequiredArgsConstructor
public class DashboardMoradorStrategy implements DashboardStrategy {

    private static final int QUANTIDADE_DE_AVISOS = 5;

    private final AvisoRepository avisoRepository;

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
        var paginacao = PageRequest.of(0, QUANTIDADE_DE_AVISOS, Sort.by(Sort.Direction.DESC, "publicadoEm"));

        List<AvisoResponse> avisos = avisoRepository
                .findByCondominioId(usuario.condominioId(), paginacao)
                .map(AvisoResponse::de)
                .getContent();

        return new DashboardMoradorResponse(
                usuario.perfil().getValor(),
                usuario.unidadeId(),
                avisos,
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
