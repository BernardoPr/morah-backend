package org.morah.morah.notificacao.modelo;

/** Por onde a notificacao sai. Cada canal tem uma Strategy de envio. */
public enum CanalNotificacao {
    EMAIL,
    PUSH,
    SMS
}
