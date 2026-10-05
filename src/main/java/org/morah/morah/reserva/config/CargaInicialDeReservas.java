package org.morah.morah.reserva.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.reserva.modelo.AreaComum;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.modelo.StatusReserva;
import org.morah.morah.reserva.repositorio.AreaComumRepository;
import org.morah.morah.reserva.repositorio.ReservaRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dados de demonstracao do modulo de reservas (roda depois da carga de usuarios e unidades).
 *
 * <p>Areas do condominio 1:
 * <ul>
 *   <li>Salao de festas - 80 pessoas, R$ 150,00, 10:00-22:00 em blocos de 4h;</li>
 *   <li>Churrasqueira - 20 pessoas, R$ 50,00, 10:00-22:00 em blocos de 4h;</li>
 *   <li>Quadra poliesportiva - 20 pessoas, gratuita, 08:00-22:00 em blocos de 1h;</li>
 *   <li>Academia - 10 pessoas, uso livre (nao exige reserva).</li>
 * </ul>
 *
 * <p>E uma reserva confirmada da Ana (Apto 101) no salao de festas, amanha das 18:00 as 22:00 -
 * assim a disponibilidade de amanha ja mostra um horario ocupado.
 *
 * <p>Idempotente: so cria as areas se o condominio ainda nao tiver nenhuma, e so cria a reserva
 * se o condominio ainda nao tiver nenhuma reserva.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "morah.carga-inicial", havingValue = "true")
public class CargaInicialDeReservas implements CommandLineRunner {

    private static final Long CONDOMINIO = 1L;
    private static final Long UNIDADE_DA_ANA = 101L;
    private static final String CPF_DA_ANA = "11111111111";
    private static final String SALAO_DE_FESTAS = "Salao de festas";

    private final AreaComumRepository areaComumRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        criarAreas();
        criarReservaDeExemplo();
    }

    private void criarAreas() {
        if (areaComumRepository.countByCondominioId(CONDOMINIO) > 0) {
            return; // ja existem areas, nao faz nada
        }

        areaComumRepository.saveAll(List.of(
                area(SALAO_DE_FESTAS, "Salao com cozinha de apoio, mesas e cadeiras.",
                        80, "150.00", true, "10:00", "22:00", 240),
                area("Churrasqueira", "Churrasqueira coberta com pia e geladeira.",
                        20, "50.00", true, "10:00", "22:00", 240),
                area("Quadra poliesportiva", "Quadra para futsal, volei e basquete.",
                        20, "0.00", true, "08:00", "22:00", 60),
                area("Academia", "Uso livre, sem necessidade de reserva.",
                        10, "0.00", false, "06:00", "23:00", null)));
        log.info("Carga inicial: areas comuns criadas (salao, churrasqueira, quadra e academia)");
    }

    private void criarReservaDeExemplo() {
        if (reservaRepository.countByCondominioId(CONDOMINIO) > 0) {
            return;
        }

        Optional<Usuario> ana = usuarioRepository.findByCpf(CPF_DA_ANA);
        Optional<AreaComum> salao = areaComumRepository.findByCondominioIdOrderByNome(CONDOMINIO).stream()
                .filter(area -> SALAO_DE_FESTAS.equals(area.getNome()))
                .findFirst();
        if (ana.isEmpty() || salao.isEmpty()) {
            return;
        }

        LocalDate amanha = Datas.hoje().plusDays(1);
        Instant inicio = amanha.atTime(LocalTime.of(18, 0)).atZone(Datas.FUSO).toInstant();
        Instant fim = amanha.atTime(LocalTime.of(22, 0)).atZone(Datas.FUSO).toInstant();

        Reserva reserva = new Reserva();
        reserva.setCondominioId(CONDOMINIO);
        reserva.setAreaComumId(salao.get().getId());
        reserva.setAreaComumNome(salao.get().getNome());
        reserva.setUnidadeId(UNIDADE_DA_ANA);
        reserva.setSolicitanteId(ana.get().getId());
        reserva.setSolicitanteNome(ana.get().getNome());
        reserva.setSolicitanteTelefone(ana.get().getTelefone());
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        reserva.setStatus(StatusReserva.CONFIRMADA);
        reserva.setValor(salao.get().getTaxa());

        reservaRepository.save(reserva);
        log.info("Carga inicial: reserva da Ana no salao de festas em {} das 18:00 as 22:00", amanha);
    }

    private AreaComum area(String nome, String descricao, int capacidade, String taxa, boolean requerReserva,
                           String abertura, String fechamento, Integer duracaoSlotMinutos) {
        AreaComum area = new AreaComum();
        area.setCondominioId(CONDOMINIO);
        area.setNome(nome);
        area.setDescricao(descricao);
        area.setCapacidade(capacidade);
        area.setTaxa(new BigDecimal(taxa));
        area.setRequerReserva(requerReserva);
        area.setStatus(AreaComum.ATIVA);
        area.setHoraAbertura(abertura);
        area.setHoraFechamento(fechamento);
        area.setDuracaoSlotMinutos(duracaoSlotMinutos);
        return area;
    }
}
