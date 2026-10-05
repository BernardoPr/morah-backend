package org.morah.morah.reserva.controle;

import java.time.LocalDate;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.reserva.dto.ReservaCreateRequest;
import org.morah.morah.reserva.dto.ReservaResponse;
import org.morah.morah.reserva.dto.VistoriaRequest;
import org.morah.morah.reserva.servico.ReservaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Endpoints de reservas e vistorias (secao "Reservas" do contrato).
 *
 * <p>Sem regra de negocio: o que cada perfil enxerga, quem pode cancelar e o que a vistoria
 * dispara ficam no {@link ReservaService}.
 */
@Tag(name = "Reservas", description = "Reserva e vistoria de areas comuns")
@RestController
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @Operation(summary = "Listar reservas (proprias, do dia na guarita, ou do condominio para o sindico)")
    @GetMapping
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO','PORTARIA','SINDICO')")
    public PaginaResponse<ReservaResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reservaService.listarDoContextoAtivo(data, page, size);
    }

    @Operation(summary = "Criar uma reserva de area comum")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public ReservaResponse criar(@Valid @RequestBody ReservaCreateRequest requisicao) {
        return reservaService.criar(requisicao);
    }

    @Operation(summary = "Detalhar uma reserva")
    @GetMapping("/{reservaId}")
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO','PORTARIA','SINDICO')")
    public ReservaResponse detalhar(@PathVariable Long reservaId) {
        return reservaService.detalhar(reservaId);
    }

    @Operation(summary = "Cancelar uma reserva")
    @DeleteMapping("/{reservaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public void cancelar(@PathVariable Long reservaId) {
        reservaService.cancelar(reservaId);
    }

    @Operation(summary = "Registrar vistoria de entrada ou saida do espaco reservado (somente portaria)")
    @PostMapping("/{reservaId}/vistoria")
    @PreAuthorize("hasRole('PORTARIA')")
    public ReservaResponse registrarVistoria(@PathVariable Long reservaId,
                                             @Valid @RequestBody VistoriaRequest requisicao) {
        return reservaService.registrarVistoria(reservaId, requisicao);
    }
}
