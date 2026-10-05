package org.morah.morah.financeiro.strategy;

import java.math.BigDecimal;
import java.util.List;

import org.morah.morah.financeiro.modelo.TipoRateio;
import org.morah.morah.unidade.modelo.Unidade;

/**
 * PADRAO DE PROJETO: STRATEGY (exemplo 4 de 6) - rateio de taxas.
 *
 * <p><b>Problema:</b> o sindico cadastra uma taxa (ex.: R$ 2.000,00 de condominio) e ela precisa
 * ser dividida entre as unidades. Existem jeitos diferentes de dividir: em partes iguais ou
 * proporcional a fracao ideal de cada apartamento - e amanha pode surgir outro (por area em m2,
 * por consumo de agua...). Um {@code if/else} ou {@code switch} no service cresceria a cada regra
 * nova e misturaria a matematica de todas elas no mesmo metodo.
 *
 * <p><b>Solucao:</b> cada forma de dividir vira uma classe que implementa esta interface
 * ({@link RateioIgualitarioStrategy}, {@link RateioPorFracaoIdealStrategy}). O
 * {@link SeletorDeRateio} recebe do Spring a lista com todas elas e entrega a que atende o
 * {@link TipoRateio} da taxa. Nova regra = nova classe {@code @Component} + novo valor no enum;
 * nenhum codigo existente muda (principio aberto/fechado).
 *
 * <p><b>Quem usa:</b> {@code TaxaService.gerarCobrancas}, que nao sabe (nem precisa saber) qual
 * calculo esta rodando - so pede as parcelas e cria uma cobranca para cada uma.
 */
public interface RateioStrategy {

    /** Tipo de rateio que esta estrategia atende. */
    TipoRateio tipo();

    /**
     * Divide {@code valorTotal} entre as unidades.
     *
     * @param valorTotal     valor da taxa (2 casas decimais)
     * @param unidadesAtivas unidades que participam do rateio (o service ja tirou as inativas)
     * @return uma parcela por unidade, na mesma ordem recebida. Unidade que nao pode ser cobrada
     *         (ex.: sem fracao ideal cadastrada) volta como {@link ParcelaDoRateio#falha}.
     */
    List<ParcelaDoRateio> ratear(BigDecimal valorTotal, List<Unidade> unidadesAtivas);
}
