package org.morah.morah.reserva.repositorio;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.modelo.StatusReserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Consultas de reservas.
 *
 * <p>Os filtros "do dia" recebem um {@link Range} de instantes: com
 * {@code Range.rightOpen(inicioDoDia, fimDoDia)} o Spring Data gera
 * {@code { "inicio": { $gte: inicioDoDia, $lt: fimDoDia } }}. Nao da para escrever
 * {@code InicioGreaterThanEqualAndInicioLessThan} no nome do metodo: o Mongo nao aceita
 * dois criterios separados para o mesmo campo.
 */
@Repository
public interface ReservaRepository extends MongoRepository<Reserva, Long> {

    /** Garante que a reserva pertence ao condominio do token (reserva de outro condominio vira 404). */
    Optional<Reserva> findByIdAndCondominioId(Long id, Long condominioId);

    /**
     * Reservas da area, com o status pedido, que se SOBREPOEM ao periodo {@code [inicio, fim)}.
     *
     * <p>Dois periodos se sobrepoem quando um comeca antes do outro terminar e termina depois
     * do outro comecar. Reservas "encostadas" (uma termina 14:00 e a outra comeca 14:00) nao
     * conflitam.
     */
    List<Reserva> findByAreaComumIdAndStatusAndInicioLessThanAndFimGreaterThan(
            Long areaComumId, StatusReserva status, Instant fim, Instant inicio);

    /** Atalho com os parametros na ordem natural (inicio, fim) e apenas as CONFIRMADAS. */
    default List<Reserva> listarConfirmadasQueSobrepoem(Long areaComumId, Instant inicio, Instant fim) {
        return findByAreaComumIdAndStatusAndInicioLessThanAndFimGreaterThan(
                areaComumId, StatusReserva.CONFIRMADA, fim, inicio);
    }

    // ---------- listagem (GET /reservas) ----------

    Page<Reserva> findByUnidadeId(Long unidadeId, Pageable paginacao);

    Page<Reserva> findByUnidadeIdAndInicioBetween(Long unidadeId, Range<Instant> periodo, Pageable paginacao);

    Page<Reserva> findByCondominioId(Long condominioId, Pageable paginacao);

    Page<Reserva> findByCondominioIdAndInicioBetween(Long condominioId, Range<Instant> periodo, Pageable paginacao);

    // ---------- dashboard ----------

    /** Reservas da unidade com o status que terminam depois de {@code agora} (limite e ordem vem no Pageable). */
    List<Reserva> findByUnidadeIdAndStatusAndFimGreaterThan(
            Long unidadeId, StatusReserva status, Instant agora, Pageable limite);

    /** Reservas do condominio no periodo, exceto as do status informado (ex.: sem as canceladas). */
    List<Reserva> findByCondominioIdAndStatusNotAndInicioBetween(
            Long condominioId, StatusReserva statusIgnorado, Range<Instant> periodo, Sort ordenacao);

    long countByCondominioId(Long condominioId);
}
