package org.morah.morah.encomenda.strategy;

import java.time.Instant;
import java.util.Optional;

import org.morah.morah.encomenda.modelo.Encomenda;

/**
 * PADRAO DE PROJETO: STRATEGY (exemplo 6 de 6) - formas de retirada de encomenda.
 *
 * <p><b>Problema:</b> na portaria, a encomenda pode ser liberada com codigos de origens
 * diferentes: o codigo do proprio morador ou o codigo de uma autorizacao dada a um terceiro.
 * Cada origem tem regras proprias (o do morador nao expira; o do terceiro tem validade e so
 * pode ser usado uma vez). Um {@code if/else} no service cresceria a cada forma nova de
 * retirada (QR code, biometria, armario inteligente...).
 *
 * <p><b>Solucao:</b> cada forma de retirada vira uma classe que implementa esta interface.
 * O {@code EncomendaService} recebe do Spring a LISTA com todas as implementacoes e pergunta
 * a cada uma, em ordem ({@code @Order}), se ela reconhece o codigo; a primeira que aceitar
 * decide a retirada. Se nenhuma aceitar, o codigo e invalido ou expirou (HTTP 409).
 *
 * <p><b>Quem usa:</b> {@code EncomendaService#confirmarRetirada} (POST /encomendas/{id}/retirada).
 *
 * <p><b>Para criar uma forma nova (ex.: QR code):</b> crie uma classe {@code @Component}
 * implementando esta interface. Nenhum codigo existente muda (principio aberto/fechado).
 *
 * <p>Diferente das outras Strategies do projeto, aqui nao existe um seletor por chave
 * (formato, canal, perfil): quem escolhe a estrategia e o proprio codigo digitado. Por isso o
 * service percorre a lista em vez de consultar um mapa - e a variacao "cadeia de tentativas".
 */
public interface ValidacaoDeRetiradaStrategy {

    /** Nome da forma de retirada (ex.: "codigo do morador"), usado no log e no resultado. */
    String forma();

    /**
     * Confere o codigo SEM alterar nada no banco.
     *
     * @return o resultado preenchido se o codigo e desta forma de retirada e ainda vale;
     *         vazio se o codigo nao e desta forma (o service pergunta para a proxima)
     */
    Optional<RetiradaValidada> validar(Encomenda encomenda, String codigo, Instant agora);

    /**
     * Hook chamado depois que a retirada foi gravada, para efeitos colaterais da propria forma
     * de retirada (ex.: "queimar" um codigo de uso unico). Por padrao nao faz nada.
     */
    default void registrarUso(Encomenda encomenda, RetiradaValidada retirada, Instant agora) {
        // por padrao, nao faz nada
    }
}
