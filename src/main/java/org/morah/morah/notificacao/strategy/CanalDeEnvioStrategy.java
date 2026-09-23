package org.morah.morah.notificacao.strategy;

import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.usuario.modelo.Usuario;

/**
 * PADRAO DE PROJETO: STRATEGY (exemplo 2 de 3) - contrato comum.
 *
 * <p><b>Problema:</b> uma notificacao pode sair por e-mail, push ou SMS. Resolver isso com
 * {@code if/else} espalha o codigo de envio e obriga a mexer no service a cada canal novo.
 *
 * <p><b>Solucao:</b> cada canal vira uma classe que implementa esta interface. Quem envia
 * pede a estrategia certa ao {@link SeletorDeCanal} e chama {@link #enviar}, sem saber
 * qual implementacao esta rodando.
 *
 * <p><b>Para criar um canal novo (ex.: WhatsApp):</b> adicione o valor no enum
 * {@code CanalNotificacao}, crie a classe {@code @Component} implementando esta interface
 * e pronto - nenhum codigo existente muda (principio aberto/fechado).
 */
public interface CanalDeEnvioStrategy {

    /** Canal que esta estrategia atende. */
    CanalNotificacao canal();

    void enviar(Notificacao notificacao, Usuario destinatario);
}
