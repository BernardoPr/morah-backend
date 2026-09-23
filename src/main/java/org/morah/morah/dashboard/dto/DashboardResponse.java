package org.morah.morah.dashboard.dto;

/**
 * Marcador comum das respostas da tela inicial.
 *
 * <p>O contrato usa {@code oneOf} com o discriminador "perfil": o front olha esse campo
 * para saber qual formato chegou.
 */
public interface DashboardResponse {

    String perfil();
}
