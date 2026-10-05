package org.morah.morah.encomenda.strategy;

/**
 * Resultado de uma {@link ValidacaoDeRetiradaStrategy} que aceitou o codigo.
 *
 * @param forma          como a retirada foi autorizada (ex.: "codigo do morador"), para log
 * @param autorizacaoId  autorizacao de terceiro usada; {@code null} quando foi o codigo do morador
 * @param nome           nome de quem pode retirar, quando a forma de retirada sabe (ex.: o terceiro
 *                       cadastrado na autorizacao); {@code null} se nao souber
 * @param documento      documento de quem pode retirar, mesma regra do nome
 */
public record RetiradaValidada(String forma, Long autorizacaoId, String nome, String documento) {
}
