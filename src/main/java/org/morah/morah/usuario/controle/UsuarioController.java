package org.morah.morah.usuario.controle;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.usuario.dto.UsuarioCreateRequest;
import org.morah.morah.usuario.dto.UsuarioResponse;
import org.morah.morah.usuario.servico.UsuarioService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Cadastro de usuarios (endpoints administrativos, usados pelo sindico).
 *
 * <p>Nao faz parte do contrato com o app - ele existe para dar suporte ao login e
 * servir de exemplo completo de CRUD (modelo -> repositorio -> service -> controller).
 */
@Tag(name = "Usuarios", description = "Cadastro de usuarios (apoio ao login)")
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Cadastrar um usuario (somente sindico)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SINDICO')")
    public UsuarioResponse criar(@Valid @RequestBody UsuarioCreateRequest requisicao) {
        return usuarioService.criar(requisicao);
    }

    @Operation(summary = "Listar usuarios (somente sindico)")
    @GetMapping
    @PreAuthorize("hasRole('SINDICO')")
    public PaginaResponse<UsuarioResponse> listar(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return usuarioService.listar(PageRequest.of(page, size, Sort.by("nome")));
    }

    @Operation(summary = "Detalhar um usuario (somente sindico)")
    @GetMapping("/{usuarioId}")
    @PreAuthorize("hasRole('SINDICO')")
    public UsuarioResponse detalhar(@PathVariable Long usuarioId) {
        return usuarioService.buscarPorId(usuarioId);
    }

    @Operation(summary = "Desativar o acesso de um usuario (somente sindico)")
    @PostMapping("/{usuarioId}/desativar")
    @PreAuthorize("hasRole('SINDICO')")
    public UsuarioResponse desativar(@PathVariable Long usuarioId) {
        return usuarioService.desativar(usuarioId);
    }
}
