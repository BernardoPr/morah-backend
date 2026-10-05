package org.morah.morah.financeiro.controle;

import org.morah.morah.financeiro.dto.InadimplenciaResumoResponse;
import org.morah.morah.financeiro.servico.InadimplenciaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Resumo de inadimplencia (secao "Financeiro" do contrato). Somente sindico. */
@Tag(name = "Financeiro", description = "Boletos/cobrancas, PIX, documentos financeiros e gestao de inadimplencia")
@RestController
@RequestMapping("/financeiro/inadimplencia")
@RequiredArgsConstructor
public class InadimplenciaController {

    private final InadimplenciaService inadimplenciaService;

    @Operation(summary = "Obter o resumo de inadimplencia do condominio")
    @GetMapping
    @PreAuthorize("hasRole('SINDICO')")
    public InadimplenciaResumoResponse resumo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return inadimplenciaService.resumo(usuario.condominioId());
    }
}
