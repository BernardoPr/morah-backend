package org.morah.morah.financeiro.controle;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.financeiro.dto.DocumentoFinanceiroCreateRequest;
import org.morah.morah.financeiro.dto.DocumentoFinanceiroResponse;
import org.morah.morah.financeiro.servico.DocumentoFinanceiroService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

/** Documentos financeiros publicados pelo sindico (secao "Financeiro" do contrato). */
@Tag(name = "Financeiro", description = "Boletos/cobrancas, PIX, documentos financeiros e gestao de inadimplencia")
@RestController
@RequestMapping("/financeiro/documentos")
@RequiredArgsConstructor
public class DocumentoFinanceiroController {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 100;

    private final DocumentoFinanceiroService documentoFinanceiroService;

    @Operation(summary = "Listar documentos financeiros publicados (notas fiscais, prestacao de contas)")
    @GetMapping
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO','SINDICO')")
    public PaginaResponse<DocumentoFinanceiroResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var paginacao = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, TAMANHO_MAXIMO_DA_PAGINA),
                Sort.by(Sort.Direction.DESC, "publicadoEm"));
        return documentoFinanceiroService.listarDoCondominio(usuario.condominioId(), paginacao);
    }

    @Operation(summary = "Publicar um novo documento financeiro (somente sindico)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SINDICO')")
    public DocumentoFinanceiroResponse publicar(@Valid @RequestBody DocumentoFinanceiroCreateRequest requisicao) {
        return documentoFinanceiroService.criar(requisicao);
    }
}
