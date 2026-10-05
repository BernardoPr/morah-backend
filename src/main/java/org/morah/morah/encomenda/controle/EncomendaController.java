package org.morah.morah.encomenda.controle;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.encomenda.dto.AutorizacaoRetiradaCreateRequest;
import org.morah.morah.encomenda.dto.AutorizacaoRetiradaResponse;
import org.morah.morah.encomenda.dto.EncomendaCreateRequest;
import org.morah.morah.encomenda.dto.EncomendaResponse;
import org.morah.morah.encomenda.dto.OcorrenciaCreateRequest;
import org.morah.morah.encomenda.dto.RetiradaConfirmRequest;
import org.morah.morah.encomenda.modelo.StatusEncomenda;
import org.morah.morah.encomenda.servico.EncomendaService;
import org.morah.morah.ocorrencia.dto.OcorrenciaResponse;
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
 * Endpoints de encomendas (secao "Encomendas" do contrato).
 *
 * <p>Fluxo completo: a portaria registra o pacote (POST /encomendas) -> o morador recebe o
 * codigo na notificacao -> opcionalmente autoriza um terceiro -> a portaria confere o codigo e
 * da baixa (POST /encomendas/{id}/retirada). Se o pacote sumir, o morador abre uma ocorrencia.
 */
@Tag(name = "Encomendas", description = "Recebimento, autorizacao de retirada por terceiros e baixa de encomendas")
@RestController
@RequestMapping("/encomendas")
@RequiredArgsConstructor
public class EncomendaController {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 100;

    private final EncomendaService encomendaService;

    @Operation(summary = "Listar encomendas (da unidade ativa, ou de todo o condominio para portaria/sindico)")
    @GetMapping
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO','PORTARIA','SINDICO')")
    public PaginaResponse<EncomendaResponse> listar(
            @RequestParam(required = false) StatusEncomenda status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var paginacao = PageRequest.of(
                Math.max(page, 0),
                Math.clamp(size, 1, TAMANHO_MAXIMO_DA_PAGINA),
                Sort.by(Sort.Direction.DESC, "recebidaEm").and(Sort.by(Sort.Direction.DESC, "id")));
        return encomendaService.listarDoContextoAtivo(status, paginacao);
    }

    @Operation(summary = "Registrar o recebimento de uma encomenda na portaria")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PORTARIA')")
    public EncomendaResponse registrar(@Valid @RequestBody EncomendaCreateRequest requisicao) {
        return encomendaService.criar(requisicao);
    }

    @Operation(summary = "Detalhar uma encomenda")
    @GetMapping("/{encomendaId}")
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO','PORTARIA','SINDICO')")
    public EncomendaResponse detalhar(@PathVariable Long encomendaId) {
        return encomendaService.detalhar(encomendaId);
    }

    @Operation(summary = "Autorizar um terceiro a retirar a encomenda")
    @PostMapping("/{encomendaId}/autorizacoes-retirada")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public AutorizacaoRetiradaResponse autorizarRetiradaDeTerceiro(
            @PathVariable Long encomendaId,
            @Valid @RequestBody AutorizacaoRetiradaCreateRequest requisicao) {
        return encomendaService.autorizarRetiradaDeTerceiro(encomendaId, requisicao);
    }

    @Operation(summary = "Confirmar a retirada da encomenda na portaria")
    @PostMapping("/{encomendaId}/retirada")
    @PreAuthorize("hasRole('PORTARIA')")
    public EncomendaResponse confirmarRetirada(@PathVariable Long encomendaId,
                                               @Valid @RequestBody RetiradaConfirmRequest requisicao) {
        return encomendaService.confirmarRetirada(encomendaId, requisicao);
    }

    @Operation(summary = "Abrir uma ocorrencia de extravio vinculada a encomenda")
    @PostMapping("/{encomendaId}/ocorrencias")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public OcorrenciaResponse abrirOcorrencia(@PathVariable Long encomendaId,
                                              @Valid @RequestBody OcorrenciaCreateRequest requisicao) {
        return encomendaService.abrirOcorrencia(encomendaId, requisicao);
    }
}
