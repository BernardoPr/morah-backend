package org.morah.morah.financeiro.controle;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.financeiro.dto.BaixaManualRequest;
import org.morah.morah.financeiro.dto.CobrancaResponse;
import org.morah.morah.financeiro.dto.PixResponse;
import org.morah.morah.financeiro.modelo.Competencia;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.servico.CobrancaService;
import org.morah.morah.financeiro.servico.PixService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/** Boletos/cobrancas e PIX (secao "Financeiro" do contrato). */
@Tag(name = "Financeiro", description = "Boletos/cobrancas, PIX, documentos financeiros e gestao de inadimplencia")
@RestController
@RequestMapping("/financeiro/cobrancas")
@RequiredArgsConstructor
public class CobrancaController {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 100;

    private final CobrancaService cobrancaService;
    private final PixService pixService;

    /**
     * O {@code @Pattern} na competencia e validado pelo proprio Spring MVC antes de chamar o
     * metodo: valor fora do formato AAAA-MM vira 400.
     */
    @Operation(summary = "Listar cobrancas da unidade ativa ou do condominio (sindico)")
    @GetMapping
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO','SINDICO')")
    public PaginaResponse<CobrancaResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) StatusCobranca status,
            @RequestParam(required = false)
            @Pattern(regexp = Competencia.FORMATO, message = Competencia.MENSAGEM_DE_FORMATO) String competencia,
            @RequestParam(required = false) Long unidadeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var paginacao = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, TAMANHO_MAXIMO_DA_PAGINA),
                Sort.by(Sort.Order.desc("vencimento"), Sort.Order.asc("unidadeId")));
        return cobrancaService.listar(usuario, status, competencia, unidadeId, paginacao);
    }

    @Operation(summary = "Detalhar uma cobranca")
    @GetMapping("/{cobrancaId}")
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO','SINDICO')")
    public CobrancaResponse detalhar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                     @PathVariable Long cobrancaId) {
        return cobrancaService.detalhar(usuario, cobrancaId);
    }

    @Operation(summary = "Gerar/obter o codigo PIX copia-e-cola da cobranca (simulado)")
    @GetMapping("/{cobrancaId}/pix")
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public PixResponse obterPix(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                @PathVariable Long cobrancaId) {
        return pixService.obterPix(usuario, cobrancaId);
    }

    @Operation(summary = "Registrar baixa manual de um pagamento recebido fora do gateway (somente sindico)")
    @PostMapping("/{cobrancaId}/baixa-manual")
    @PreAuthorize("hasRole('SINDICO')")
    public CobrancaResponse baixarManualmente(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                              @PathVariable Long cobrancaId,
                                              @Valid @RequestBody BaixaManualRequest requisicao) {
        return cobrancaService.baixarManualmente(usuario, cobrancaId, requisicao);
    }
}
