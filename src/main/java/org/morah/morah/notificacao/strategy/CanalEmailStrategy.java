package org.morah.morah.notificacao.strategy;

import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Envio por e-mail.
 *
 * <p>Base pronta: aqui entraria a chamada ao provedor (SendGrid, Azure Communication
 * Services, JavaMailSender...). Por enquanto apenas registramos no log.
 */
@Slf4j
@Component
public class CanalEmailStrategy implements CanalDeEnvioStrategy {

    @Override
    public CanalNotificacao canal() {
        return CanalNotificacao.EMAIL;
    }

    @Override
    public void enviar(Notificacao notificacao, Usuario destinatario) {
        log.info("[EMAIL] para {}: {} - {}",
                destinatario.getEmail(), notificacao.getTitulo(), notificacao.getMensagem());
    }
}
