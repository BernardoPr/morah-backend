package org.morah.morah.reserva.servico;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.reserva.dto.AreaComumResponse;
import org.morah.morah.reserva.dto.DisponibilidadeResponse;
import org.morah.morah.reserva.dto.DisponibilidadeResponse.Horario;
import org.morah.morah.reserva.modelo.AreaComum;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.repositorio.AreaComumRepository;
import org.morah.morah.reserva.repositorio.ReservaRepository;
import org.morah.morah.seguranca.jwt.ContextoDeSeguranca;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Areas comuns do condominio e a grade de horarios de cada uma.
 *
 * <p>Nao estende o {@code ServicoCrudTemplate}: o contrato so tem consultas de area (o cadastro
 * vem da carga inicial), entao nao ha fluxo de criacao para reaproveitar.
 */
@Service
@RequiredArgsConstructor
public class AreaComumService {

    private final AreaComumRepository areaComumRepository;
    private final ReservaRepository reservaRepository;

    /**
     * GET /areas-comuns: todas as areas do condominio do token, em ordem alfabetica.
     *
     * <p>Devolve tambem as que estao em manutencao ou que nao exigem reserva: o schema traz
     * {@code status} e {@code requerReserva} justamente para o app mostrar isso ao morador.
     */
    public List<AreaComumResponse> listarDoCondominioAtivo() {
        Long condominioId = ContextoDeSeguranca.usuarioLogado().condominioId();
        return areaComumRepository.findByCondominioIdOrderByNome(condominioId).stream()
                .map(AreaComumResponse::de)
                .toList();
    }

    /**
     * GET /areas-comuns/{id}/disponibilidade?data=: a grade de horarios da area no dia.
     *
     * <p>Os horarios vao da abertura ao fechamento em passos de {@code duracaoSlotMinutos}, na
     * hora local do condominio. Um horario esta disponivel quando:
     * <ol>
     *   <li>a area esta ativa e exige reserva;</li>
     *   <li>o horario ainda nao comecou;</li>
     *   <li>nenhuma reserva CONFIRMADA ocupa parte dele.</li>
     * </ol>
     */
    public DisponibilidadeResponse disponibilidade(Long areaComumId, LocalDate data) {
        Long condominioId = ContextoDeSeguranca.usuarioLogado().condominioId();
        AreaComum area = buscarDoCondominio(areaComumId, condominioId);

        // Uma unica consulta traz as reservas confirmadas que tocam o dia; o resto e feito em memoria.
        List<Reserva> confirmadasDoDia = reservaRepository.listarConfirmadasQueSobrepoem(
                area.getId(), Datas.inicioDoDia(data), Datas.fimDoDia(data));

        return new DisponibilidadeResponse(data, montarHorarios(area, data, confirmadasDoDia, Instant.now()));
    }

    /** Busca a area garantindo que ela e do condominio informado (outra area vira 404). */
    public AreaComum buscarDoCondominio(Long areaComumId, Long condominioId) {
        return areaComumRepository.findByIdAndCondominioId(areaComumId, condominioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area comum", areaComumId));
    }

    /**
     * Gera os blocos do dia. Se o funcionamento nao for multiplo da duracao (ex.: 10:00-21:00 em
     * blocos de 4h), o ultimo bloco e encurtado ate o fechamento (18:00-21:00).
     * Area sem horario ou sem duracao configurada (ex.: academia, de uso livre) nao tem grade.
     */
    private List<Horario> montarHorarios(AreaComum area, LocalDate data, List<Reserva> confirmadas, Instant agora) {
        Integer duracao = area.getDuracaoSlotMinutos();
        if (!area.temHorarioDeFuncionamento() || duracao == null || duracao <= 0) {
            return List.of();
        }

        LocalTime fechamento = area.fechamento();
        List<Horario> horarios = new ArrayList<>();

        LocalTime inicioLocal = area.abertura();
        while (inicioLocal.isBefore(fechamento)) {
            LocalTime fimLocal = inicioLocal.plusMinutes(duracao);
            // "!isAfter" pega a virada da meia-noite (22:00 + 4h = 02:00, que "volta" no relogio).
            if (fimLocal.isAfter(fechamento) || !fimLocal.isAfter(inicioLocal)) {
                fimLocal = fechamento;
            }

            Instant inicio = data.atTime(inicioLocal).atZone(Datas.FUSO).toInstant();
            Instant fim = data.atTime(fimLocal).atZone(Datas.FUSO).toInstant();

            boolean ocupado = confirmadas.stream()
                    .anyMatch(reserva -> reserva.getInicio().isBefore(fim) && reserva.getFim().isAfter(inicio));
            boolean disponivel = area.aceitaReservas() && inicio.isAfter(agora) && !ocupado;

            horarios.add(new Horario(inicio, fim, disponivel));
            inicioLocal = fimLocal;
        }
        return horarios;
    }
}
