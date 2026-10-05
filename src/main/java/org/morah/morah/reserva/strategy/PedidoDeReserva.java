package org.morah.morah.reserva.strategy;

import java.time.Instant;

import org.morah.morah.reserva.modelo.AreaComum;

/**
 * Tudo que as regras precisam para julgar um pedido de reserva.
 *
 * <p>A area ja vem carregada (e ja conferida como sendo do condominio do token), e o "agora"
 * vem de fora: assim todas as regras usam o mesmo instante e os testes conseguem fixar o tempo.
 */
public record PedidoDeReserva(AreaComum area, Instant inicio, Instant fim, Instant agora) {
}
