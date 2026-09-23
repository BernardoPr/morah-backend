package org.morah.morah.notificacao.strategy;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.springframework.stereotype.Component;

/**
 * Guarda todas as estrategias de canal e entrega a certa.
 *
 * <p>O Spring injeta automaticamente a LISTA com todas as classes que implementam
 * {@link CanalDeEnvioStrategy}; aqui so transformamos essa lista em um mapa
 * (canal -> estrategia). Nenhum {@code if} ou {@code switch} e necessario.
 */
@Component
public class SeletorDeCanal {

    private final Map<CanalNotificacao, CanalDeEnvioStrategy> estrategias;

    public SeletorDeCanal(List<CanalDeEnvioStrategy> canaisDisponiveis) {
        this.estrategias = canaisDisponiveis.stream()
                .collect(java.util.stream.Collectors.toMap(CanalDeEnvioStrategy::canal, Function.identity()));
    }

    public CanalDeEnvioStrategy obter(CanalNotificacao canal) {
        CanalDeEnvioStrategy estrategia = estrategias.get(canal);
        if (estrategia == null) {
            throw new IllegalStateException("Nenhuma estrategia cadastrada para o canal " + canal);
        }
        return estrategia;
    }
}
