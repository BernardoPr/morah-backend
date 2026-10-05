package org.morah.morah.reserva.strategy;

/**
 * PADRAO DE PROJETO: STRATEGY (exemplo 5 de 6) - regras de validacao de uma reserva nova.
 *
 * <p><b>Problema:</b> criar uma reserva exige varias conferencias (area disponivel, horario no
 * futuro, dentro do funcionamento, sem conflito com outra reserva...). Escritas todas dentro do
 * service, elas virariam uma sequencia de {@code if}s que cresce a cada regra nova que o
 * condominio inventar (antecedencia minima, limite de reservas por mes, bloqueio de inadimplente...).
 *
 * <p><b>Solucao:</b> cada regra e uma classe que implementa esta interface e e anotada com
 * {@code @Component}. O Spring injeta no {@code ReservaService} a <b>lista</b> com todas as
 * implementacoes, ja ordenada pelo {@code @Order} de cada uma, e o service aplica uma por uma.
 * Regra nova = classe nova; nenhum codigo existente muda.
 *
 * <p><b>Diferenca para os outros Strategies do projeto:</b> no dashboard, no canal de notificacao
 * e na exportacao de avisos um seletor escolhe <i>uma</i> estrategia. Aqui <i>todas</i> sao
 * aplicadas em sequencia, e qualquer uma pode barrar o pedido lancando
 * {@link org.morah.morah.comum.erro.RegraDeNegocioException} (HTTP 409).
 *
 * <p><b>Quem usa:</b> {@code ReservaService.validarCriacao}, que e um hook do Template Method
 * {@code ServicoCrudTemplate} - os dois padroes trabalhando juntos: o Template Method fixa
 * <i>quando</i> validar, o Strategy define <i>o que</i> validar.
 *
 * <p>Implementacoes (na ordem em que rodam): {@link RegraDeAreaDisponivel},
 * {@link RegraDePeriodoValido}, {@link RegraDeInicioNoFuturo},
 * {@link RegraDeHorarioDeFuncionamento} e {@link RegraDeConflitoDeHorario} (a unica que consulta
 * o banco, por isso fica por ultimo).
 */
public interface RegraDeReservaStrategy {

    /** Nao devolve nada: se o pedido quebrar a regra, lanca {@code RegraDeNegocioException}. */
    void validar(PedidoDeReserva pedido);
}
