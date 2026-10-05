package org.morah.morah.aviso.controle;

import org.morah.morah.aviso.dto.AvisoCreateRequest;
import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.aviso.dto.AvisoUpdateRequest;
import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.morah.morah.aviso.servico.AvisoService;
import org.morah.morah.aviso.strategy.ExportadorDeAvisosStrategy;
import org.morah.morah.aviso.strategy.SeletorDeExportador;
import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Endpoints do mural de avisos (secao "Avisos" do contrato).
 *
 * <p>Este controller e o MODELO para os demais modulos (financeiro, portaria, reservas...):
 * recebe DTO validado, delega para o service e devolve DTO. Nenhuma regra de negocio aqui.
 */
@Tag(name = "Avisos", description = "Mural de comunicados do condominio")
@RestController
@RequestMapping("/avisos")
@RequiredArgsConstructor
public class AvisoController {

    private static final Sort ORDEM_DE_PUBLICACAO = Sort.by(Sort.Direction.DESC, "publicadoEm");

    private final AvisoService avisoService;
    private final SeletorDeExportador seletorDeExportador;

    @Operation(summary = "Listar avisos vigentes visiveis ao contexto ativo")
    @GetMapping
    public PaginaResponse<AvisoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(required = false) PrioridadeAviso prioridade,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var paginacao = PageRequest.of(page, Math.min(size, 100), ORDEM_DE_PUBLICACAO);
        return avisoService.listarVisiveis(usuario, prioridade, paginacao);
    }

    @Operation(summary = "Publicar um novo aviso (somente sindico)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SINDICO')")
    public AvisoResponse criar(@Valid @RequestBody AvisoCreateRequest requisicao) {
        return avisoService.criar(requisicao);
    }

    @Operation(summary = "Detalhar um aviso (marca como lido; 410 se estiver fora de vigencia)")
    @GetMapping("/{avisoId}")
    public AvisoResponse detalhar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                  @PathVariable Long avisoId) {
        return avisoService.detalhar(avisoId, usuario);
    }

    @Operation(summary = "Editar um aviso (somente sindico)")
    @PatchMapping("/{avisoId}")
    @PreAuthorize("hasRole('SINDICO')")
    public AvisoResponse atualizar(@PathVariable Long avisoId,
                                   @RequestBody AvisoUpdateRequest requisicao) {
        return avisoService.atualizar(avisoId, requisicao);
    }

    @Operation(summary = "Remover um aviso (somente sindico)")
    @DeleteMapping("/{avisoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SINDICO')")
    public void remover(@PathVariable Long avisoId) {
        avisoService.remover(avisoId);
    }

    /** Endpoint extra (fora do contrato) que demonstra a Strategy de exportacao. */
    @Operation(summary = "Exportar os avisos em csv ou json (somente sindico)")
    @GetMapping("/exportar")
    @PreAuthorize("hasRole('SINDICO')")
    public ResponseEntity<String> exportar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                           @RequestParam(defaultValue = "csv") String formato) {
        ExportadorDeAvisosStrategy exportador = seletorDeExportador.obter(formato);

        var avisos = avisoService
                .listarVisiveis(usuario, null, PageRequest.of(0, 1000, ORDEM_DE_PUBLICACAO))
                .content();

        return ResponseEntity.ok()
                .header("Content-Type", exportador.tipoDeConteudo())
                .header("Content-Disposition", "attachment; filename=avisos." + exportador.formato())
                .body(exportador.exportar(avisos));
    }
}
