package org.morah.morah.reserva.controle;

import java.time.LocalDate;
import java.util.List;

import org.morah.morah.reserva.dto.AreaComumResponse;
import org.morah.morah.reserva.dto.DisponibilidadeResponse;
import org.morah.morah.reserva.servico.AreaComumService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Endpoints de areas comuns (primeira parte da secao "Reservas" do contrato). */
@Tag(name = "Reservas", description = "Reserva e vistoria de areas comuns")
@RestController
@RequestMapping("/areas-comuns")
@RequiredArgsConstructor
public class AreaComumController {

    private final AreaComumService areaComumService;

    @Operation(summary = "Listar as areas comuns do condominio ativo")
    @GetMapping
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO','SINDICO','PORTARIA')")
    public List<AreaComumResponse> listar() {
        return areaComumService.listarDoCondominioAtivo();
    }

    @Operation(summary = "Consultar horarios disponiveis de uma area comum em uma data")
    @GetMapping("/{areaComumId}/disponibilidade")
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public DisponibilidadeResponse disponibilidade(
            @PathVariable Long areaComumId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return areaComumService.disponibilidade(areaComumId, data);
    }
}
