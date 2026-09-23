package org.morah.morah.dashboard.strategy;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.dashboard.dto.DashboardResponse;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;

/**
 * PADRAO DE PROJETO: STRATEGY (exemplo 1 de 3) - contrato comum.
 *
 * <p><b>Problema:</b> o endpoint {@code GET /home/dashboard} e unico, mas devolve conteudos
 * completamente diferentes conforme o perfil ativo (morador, sindico ou portaria).
 * Um metodo com {@code if (perfil == ...) ... else if ...} ficaria enorme e teria que ser
 * alterado sempre que surgisse um perfil novo.
 *
 * <p><b>Solucao:</b> cada perfil ganha uma classe com o seu proprio algoritmo de montagem.
 * O {@link SeletorDeDashboard} descobre qual usar e o controller nao conhece nenhuma delas.
 */
public interface DashboardStrategy {

    /** Perfil principal atendido por esta estrategia. */
    Perfil perfilAtendido();

    /**
     * Permite que uma estrategia atenda mais de um perfil.
     * Por padrao atende apenas o {@link #perfilAtendido()}.
     */
    default boolean atende(Perfil perfil) {
        return perfilAtendido() == perfil;
    }

    DashboardResponse montar(UsuarioAutenticado usuario);
}
