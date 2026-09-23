package org.morah.morah.notificacao.controle;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.notificacao.dto.NotificacaoResponse;
import org.morah.morah.notificacao.servico.NotificacaoService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Endpoints de notificacoes (secao "Notificacoes" do contrato). */
@Tag(name = "Notificacoes", description = "Notificacoes internas do usuario logado")
@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @Operation(summary = "Listar notificacoes do perfil ativo")
    @GetMapping
    public PaginaResponse<NotificacaoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return notificacaoService.listarDoUsuario(usuario, PageRequest.of(page, size));
    }

    @Operation(summary = "Marcar uma notificacao como lida")
    @PatchMapping("/{notificacaoId}/lida")
    public NotificacaoResponse marcarComoLida(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @PathVariable Long notificacaoId) {
        return notificacaoService.marcarComoLida(usuario, notificacaoId);
    }
}
