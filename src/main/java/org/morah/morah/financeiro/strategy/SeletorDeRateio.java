package org.morah.morah.financeiro.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.financeiro.modelo.TipoRateio;
import org.springframework.stereotype.Component;

/**
 * Guarda todas as estrategias de rateio e entrega a certa para o tipo da taxa.
 *
 * <p>Mesmo truque do {@code SeletorDeCanal} e do {@code SeletorDeExportador}: o Spring injeta a
 * LISTA com todas as classes que implementam {@link RateioStrategy}; aqui ela vira um mapa
 * (tipo -> estrategia). Nenhum {@code if} ou {@code switch} por tipo de rateio.
 */
@Component
public class SeletorDeRateio {

    private final Map<TipoRateio, RateioStrategy> estrategias = new EnumMap<>(TipoRateio.class);

    public SeletorDeRateio(List<RateioStrategy> estrategiasDisponiveis) {
        estrategiasDisponiveis.forEach(estrategia -> estrategias.put(estrategia.tipo(), estrategia));
    }

    public RateioStrategy obter(TipoRateio tipo) {
        RateioStrategy estrategia = tipo == null ? null : estrategias.get(tipo);
        if (estrategia == null) {
            throw new RegraDeNegocioException(
                    "Tipo de rateio nao suportado: " + tipo + ". Use um destes: " + estrategias.keySet());
        }
        return estrategia;
    }
}
