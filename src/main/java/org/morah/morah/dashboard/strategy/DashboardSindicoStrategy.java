package org.morah.morah.dashboard.strategy;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.aviso.repositorio.AvisoRepository;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.dashboard.dto.DashboardSindicoResponse;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Tela inicial do sindico: visao administrativa do condominio. */
@Component
@RequiredArgsConstructor
public class DashboardSindicoStrategy implements DashboardStrategy {

    private final AvisoRepository avisoRepository;

    @Override
    public Perfil perfilAtendido() {
        return Perfil.SINDICO;
    }

    @Override
    public DashboardResponse montar(UsuarioAutenticado usuario) {
        var paginacao = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "publicadoEm"));

        List<AvisoResponse> avisos = avisoRepository
                .findByCondominioId(usuario.condominioId(), paginacao)
                .map(AvisoResponse::de)
                .getContent();

        // Os contadores virao dos modulos de vinculos e ocorrencias quando forem implementados.
        return new DashboardSindicoResponse(usuario.perfil().getValor(), avisos, 0, 0);
    }
}
