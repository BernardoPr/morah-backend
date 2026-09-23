package org.morah.morah.login.controle;

import org.morah.morah.login.dto.ContextoAtivoResponse;
import org.morah.morah.login.dto.LoginRequest;
import org.morah.morah.login.dto.MeResponse;
import org.morah.morah.login.dto.RefreshRequest;
import org.morah.morah.login.dto.SelecionarContextoRequest;
import org.morah.morah.login.servico.LoginService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints de sessao (secao "Login" do contrato).
 *
 * <p><b>Diferenca em relacao ao contrato:</b> o contrato previa o Keycloak como servidor de
 * autenticacao e por isso nao tinha um endpoint de usuario/senha. Como a hospedagem sera no
 * plano gratuito do Azure (sem espaco para subir um Keycloak), a autenticacao foi feita
 * dentro da propria API e ganhamos o {@code POST /auth/login}. Os demais endpoints
 * (/auth/me, /auth/contexto, /auth/refresh, /auth/logout) seguem o contrato.
 */
@Tag(name = "Login", description = "Sessao, contexto ativo e perfil autenticado")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;

    @Operation(summary = "Entrar com CPF e senha (rota publica)")
    @PostMapping("/login")
    public ContextoAtivoResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return loginService.entrar(requisicao);
    }

    @Operation(summary = "Consultar a pessoa autenticada e seus perfis disponiveis")
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return loginService.consultarSessao(usuario);
    }

    @Operation(summary = "Selecionar o par condominio/perfil ativo da sessao")
    @PostMapping("/contexto")
    public ContextoAtivoResponse selecionarContexto(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                    @Valid @RequestBody SelecionarContextoRequest requisicao) {
        return loginService.selecionarContexto(usuario, requisicao);
    }

    @Operation(summary = "Renovar o token a partir do refresh token (rota publica)")
    @PostMapping("/refresh")
    public ContextoAtivoResponse renovar(@Valid @RequestBody RefreshRequest requisicao) {
        return loginService.renovar(requisicao);
    }

    @Operation(summary = "Encerrar a sessao")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        loginService.sair(usuario);
    }
}
