package org.morah.morah.financeiro.controle;

import org.morah.morah.financeiro.dto.GerarCobrancasResponse;
import org.morah.morah.financeiro.dto.TaxaCreateRequest;
import org.morah.morah.financeiro.dto.TaxaResponse;
import org.morah.morah.financeiro.servico.TaxaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Taxas e geracao de cobrancas em lote (secao "Financeiro" do contrato). Somente sindico. */
@Tag(name = "Financeiro", description = "Boletos/cobrancas, PIX, documentos financeiros e gestao de inadimplencia")
@RestController
@RequestMapping("/financeiro/taxas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SINDICO')")
public class TaxaController {

    private final TaxaService taxaService;

    @Operation(summary = "Cadastrar uma taxa/rateio para o condominio")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxaResponse criar(@Valid @RequestBody TaxaCreateRequest requisicao) {
        return taxaService.criar(requisicao);
    }

    @Operation(summary = "Gerar cobrancas em lote para todas as unidades a partir de uma taxa")
    @PostMapping("/{taxaId}/gerar-cobrancas")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GerarCobrancasResponse gerarCobrancas(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                 @PathVariable Long taxaId) {
        return taxaService.gerarCobrancas(usuario, taxaId);
    }
}
