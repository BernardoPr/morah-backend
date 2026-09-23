package org.morah.morah.notificacao.strategy;

import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Envio por SMS (usado, por exemplo, quando o morador nao tem o app instalado).
 * Base pronta: substitua o log pela chamada real ao provedor.
 */
@Slf4j
@Component
public class CanalSmsStrategy implements CanalDeEnvioStrategy {

    @Override
    public CanalNotificacao canal() {
        return CanalNotificacao.SMS;
    }

    @Override
    public void enviar(Notificacao notificacao, Usuario destinatario) {
        log.info("[SMS] para {}: {}", destinatario.getTelefone(), notificacao.getTitulo());
    }
}
