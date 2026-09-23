package org.morah.morah.dashboard.controle;

import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.dashboard.strategy.SeletorDeDashboard;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * GET /home/dashboard do contrato.
 *
 * <p>Repare que o controller nao tem nenhum {@code if} por perfil: ele pede a estrategia
 * certa e manda montar.
 */
@Tag(name = "Tela Inicial", description = "Painel inicial do perfil ativo")
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class DashboardController {

    private final SeletorDeDashboard seletorDeDashboard;

    @Operation(summary = "Dados agregados da tela inicial do perfil ativo")
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return seletorDeDashboard.obter(usuario.perfil()).montar(usuario);
    }
}
