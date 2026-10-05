package org.morah.morah.portaria.controle;

import java.time.LocalDate;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.portaria.dto.AcessoCreateRequest;
import org.morah.morah.portaria.dto.AcessoResponse;
import org.morah.morah.portaria.dto.AutorizacaoVisitaCreateRequest;
import org.morah.morah.portaria.dto.AutorizacaoVisitaResponse;
import org.morah.morah.portaria.dto.DecisaoAutorizacaoRequest;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.morah.morah.portaria.servico.AcessoService;
import org.morah.morah.portaria.servico.AutorizacaoVisitaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * Endpoints da portaria (secao "Portaria" do contrato): visitantes, autorizacoes e acessos.
 * Sem regra de negocio: recebe, delega para o service e devolve.
 */
@Tag(name = "Portaria", description = "Autorizacao e registro de entrada de visitantes")
@RestController
@RequestMapping("/portaria")
@RequiredArgsConstructor
public class PortariaController {

    private static final int TAMANHO_MAXIMO_DA_PAGINA = 100;

    private final AutorizacaoVisitaService autorizacaoVisitaService;
    private final AcessoService acessoService;

    @Operation(summary = "Registrar um visitante e solicitar autorizacao de entrada ao morador")
    @PostMapping("/visitantes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PORTARIA')")
    public AutorizacaoVisitaResponse registrarVisitante(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                        @Valid @RequestBody AutorizacaoVisitaCreateRequest requisicao) {
        return autorizacaoVisitaService.registrarVisitante(usuario, requisicao);
    }

    @Operation(summary = "Listar autorizacoes de visita (fila da guarita ou pendencias do morador)")
    @GetMapping("/autorizacoes")
    @PreAuthorize("hasAnyRole('PORTARIA','MORADOR','PROPRIETARIO')")
    public PaginaResponse<AutorizacaoVisitaResponse> listarAutorizacoes(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) StatusAutorizacaoVisita status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return autorizacaoVisitaService.listar(usuario, status, paginacao(page, size, "solicitadoEm"));
    }

    @Operation(summary = "Detalhar uma autorizacao de visita")
    @GetMapping("/autorizacoes/{autorizacaoId}")
    @PreAuthorize("hasAnyRole('PORTARIA','MORADOR','PROPRIETARIO')")
    public AutorizacaoVisitaResponse detalharAutorizacao(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                         @PathVariable Long autorizacaoId) {
        return autorizacaoVisitaService.detalhar(usuario, autorizacaoId);
    }

    @Operation(summary = "Autorizar ou recusar a entrada de um visitante")
    @PatchMapping("/autorizacoes/{autorizacaoId}/decisao")
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public AutorizacaoVisitaResponse decidir(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                             @PathVariable Long autorizacaoId,
                                             @Valid @RequestBody DecisaoAutorizacaoRequest requisicao) {
        return autorizacaoVisitaService.decidir(usuario, autorizacaoId, requisicao);
    }

    @Operation(summary = "Reenviar a notificacao push de autorizacao pendente")
    @PostMapping("/autorizacoes/{autorizacaoId}/reenviar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('PORTARIA')")
    public void reenviar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                         @PathVariable Long autorizacaoId) {
        autorizacaoVisitaService.reenviar(usuario, autorizacaoId);
    }

    @Operation(summary = "Consultar o historico de acessos do condominio")
    @GetMapping("/acessos")
    @PreAuthorize("hasAnyRole('PORTARIA','SINDICO')")
    public PaginaResponse<AcessoResponse> listarAcessos(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return acessoService.listar(usuario, data, paginacao(page, size, "entrada"));
    }

    @Operation(summary = "Registrar a entrada ou saida efetiva de um visitante ja autorizado")
    @PostMapping("/acessos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PORTARIA')")
    public AcessoResponse registrarAcesso(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                          @Valid @RequestBody AcessoCreateRequest requisicao) {
        return acessoService.registrar(usuario, requisicao);
    }

    /** Pagina pedida, limitada a 100 itens (contrato), do mais recente para o mais antigo. */
    private static PageRequest paginacao(int page, int size, String campoDeData) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, TAMANHO_MAXIMO_DA_PAGINA),
                Sort.by(Sort.Direction.DESC, campoDeData));
    }
}
