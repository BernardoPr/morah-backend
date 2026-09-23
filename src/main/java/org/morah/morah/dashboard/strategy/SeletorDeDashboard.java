package org.morah.morah.dashboard.strategy;

import java.util.List;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Escolhe a estrategia de dashboard do perfil ativo.
 *
 * <p>O Spring injeta a lista com todas as implementacoes de {@link DashboardStrategy}.
 * Criar um perfil novo = criar uma classe nova; esta aqui nao muda.
 */
@Component
@RequiredArgsConstructor
public class SeletorDeDashboard {

    private final List<DashboardStrategy> estrategias;

    public DashboardStrategy obter(Perfil perfil) {
        return estrategias.stream()
                .filter(estrategia -> estrategia.atende(perfil))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Nenhuma tela inicial configurada para o perfil " + perfil.getValor()));
    }
}
