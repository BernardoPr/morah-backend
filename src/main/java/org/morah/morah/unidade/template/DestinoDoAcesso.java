package org.morah.morah.unidade.template;

import org.morah.morah.unidade.modelo.Unidade;

/**
 * Onde o acesso esta sendo concedido: a unidade e o nome do condominio.
 *
 * <p>O nome do condominio vem do token de quem esta operando (sindico ou proprietario), porque
 * a {@link Unidade} nao o guarda - e o {@code VinculoPerfil} do usuario precisa dele para o
 * login mostrar "Residencial Morah" na lista de perfis disponiveis.
 */
public record DestinoDoAcesso(Unidade unidade, String nomeDoCondominio) {
}
