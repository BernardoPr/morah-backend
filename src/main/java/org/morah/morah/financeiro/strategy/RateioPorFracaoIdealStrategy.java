package org.morah.morah.financeiro.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.morah.morah.financeiro.modelo.TipoRateio;
import org.morah.morah.unidade.modelo.Unidade;
import org.springframework.stereotype.Component;

/**
 * Rateio pela fracao ideal: cada unidade paga {@code valor x fracaoIdeal} (a fracao ideal e a
 * parte do terreno/edificio que pertence a unidade - um apartamento maior paga mais).
 *
 * <p>Unidade sem fracao ideal cadastrada nao tem como ser cobrada e volta como falha (o sindico
 * ve o motivo no relatorio). As demais NAO sao recalculadas para cobrir a parte dela: a fracao
 * ideal e a parte de cada unidade no condominio inteiro, entao cada uma paga so a sua.
 *
 * <p><b>Centavos:</b> o mesmo cuidado do rateio igualitario. Com fracoes "quebradas" (ex.: 1/3 =
 * 0,3333333333) arredondar cada parcela isoladamente pode perder ou criar centavos. Usamos o
 * <i>metodo do maior resto</i>: corta todas as parcelas para baixo e devolve os centavos que
 * faltam, um a um, as unidades que mais perderam no corte. Assim a soma das parcelas e sempre o
 * valor exato de {@code valor x soma das fracoes} - e, quando as fracoes somam 1, o valor da taxa.
 */
@Component
public class RateioPorFracaoIdealStrategy implements RateioStrategy {

    static final String SEM_FRACAO_IDEAL = "unidade sem fracao ideal cadastrada";

    @Override
    public TipoRateio tipo() {
        return TipoRateio.FRACAO_IDEAL;
    }

    @Override
    public List<ParcelaDoRateio> ratear(BigDecimal valorTotal, List<Unidade> unidadesAtivas) {
        // 1. Valor exato (sem arredondar) de cada unidade que tem fracao ideal.
        Map<Long, BigDecimal> valoresExatos = new LinkedHashMap<>();
        for (Unidade unidade : unidadesAtivas) {
            if (temFracaoIdeal(unidade)) {
                valoresExatos.put(unidade.getId(), valorTotal.multiply(unidade.getFracaoIdeal()));
            }
        }

        // 2. Arredonda para centavos sem perder nem criar dinheiro.
        Map<Long, BigDecimal> valoresEmReais = arredondarSemPerderCentavos(valoresExatos);

        // 3. Uma parcela por unidade, na ordem recebida.
        List<ParcelaDoRateio> parcelas = new ArrayList<>(unidadesAtivas.size());
        for (Unidade unidade : unidadesAtivas) {
            BigDecimal valor = valoresEmReais.get(unidade.getId());
            parcelas.add(valor == null
                    ? ParcelaDoRateio.falha(unidade.getId(), SEM_FRACAO_IDEAL)
                    : ParcelaDoRateio.ok(unidade.getId(), valor));
        }
        return parcelas;
    }

    private boolean temFracaoIdeal(Unidade unidade) {
        return unidade.getFracaoIdeal() != null && unidade.getFracaoIdeal().signum() > 0;
    }

    /** Metodo do maior resto (explicado no Javadoc da classe). */
    private Map<Long, BigDecimal> arredondarSemPerderCentavos(Map<Long, BigDecimal> valoresExatos) {
        BigDecimal somaExata = valoresExatos.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalEmCentavos = somaExata.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();

        Map<Long, Long> centavos = new LinkedHashMap<>();
        Map<Long, BigDecimal> restos = new LinkedHashMap<>();
        long somaCortada = 0;

        for (Map.Entry<Long, BigDecimal> entrada : valoresExatos.entrySet()) {
            BigDecimal exatoEmCentavos = entrada.getValue().movePointRight(2);
            long cortado = exatoEmCentavos.setScale(0, RoundingMode.FLOOR).longValueExact();
            centavos.put(entrada.getKey(), cortado);
            restos.put(entrada.getKey(), exatoEmCentavos.subtract(BigDecimal.valueOf(cortado)));
            somaCortada += cortado;
        }

        // Quem perdeu mais no corte recebe o centavo de volta (empate: vale a ordem da lista).
        long centavosQueFaltam = totalEmCentavos - somaCortada;
        valoresExatos.keySet().stream()
                .sorted(Comparator.comparing((Long unidadeId) -> restos.get(unidadeId)).reversed())
                .limit(Math.max(centavosQueFaltam, 0))
                .forEach(unidadeId -> centavos.merge(unidadeId, 1L, Long::sum));

        Map<Long, BigDecimal> emReais = new LinkedHashMap<>();
        centavos.forEach((unidadeId, valor) -> emReais.put(unidadeId, BigDecimal.valueOf(valor, 2)));
        return emReais;
    }
}
