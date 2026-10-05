package org.morah.morah.financeiro.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.morah.morah.financeiro.modelo.TipoRateio;
import org.morah.morah.unidade.modelo.Unidade;
import org.springframework.stereotype.Component;

/**
 * Rateio em partes iguais: cada unidade ativa paga {@code valor / quantidade de unidades}.
 *
 * <p><b>O problema dos centavos:</b> R$ 100,00 / 3 = 33,333... Arredondar cada parte para
 * R$ 33,33 faz o condominio arrecadar R$ 99,99 - some um centavo. Por isso a conta e feita em
 * CENTAVOS inteiros: 10000 / 3 = 3333 e sobram 1. Os centavos que sobram vao, um para cada, para
 * as primeiras unidades da lista (que chega ordenada pela identificacao). Resultado: 33,34 +
 * 33,33 + 33,33 = 100,00, exatamente o valor da taxa.
 */
@Component
public class RateioIgualitarioStrategy implements RateioStrategy {

    @Override
    public TipoRateio tipo() {
        return TipoRateio.IGUALITARIO;
    }

    @Override
    public List<ParcelaDoRateio> ratear(BigDecimal valorTotal, List<Unidade> unidadesAtivas) {
        if (unidadesAtivas.isEmpty()) {
            return List.of();
        }

        long totalEmCentavos = valorTotal.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        int quantidade = unidadesAtivas.size();
        long centavosPorUnidade = totalEmCentavos / quantidade;
        long centavosQueSobram = totalEmCentavos % quantidade;

        List<ParcelaDoRateio> parcelas = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            long centavos = centavosPorUnidade + (i < centavosQueSobram ? 1 : 0);
            parcelas.add(ParcelaDoRateio.ok(unidadesAtivas.get(i).getId(), BigDecimal.valueOf(centavos, 2)));
        }
        return parcelas;
    }
}
