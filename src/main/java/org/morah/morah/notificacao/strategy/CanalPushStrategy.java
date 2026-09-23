package org.morah.morah.notificacao.strategy;

import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Envio por push no aplicativo (Firebase Cloud Messaging, por exemplo).
 * Base pronta: substitua o log pela chamada real ao provedor.
 */
@Slf4j
@Component
public class CanalPushStrategy implements CanalDeEnvioStrategy {

    @Override
    public CanalNotificacao canal() {
        return CanalNotificacao.PUSH;
    }

    @Override
    public void enviar(Notificacao notificacao, Usuario destinatario) {
        log.info("[PUSH] para usuario {}: {}", destinatario.getId(), notificacao.getTitulo());
    }
}
