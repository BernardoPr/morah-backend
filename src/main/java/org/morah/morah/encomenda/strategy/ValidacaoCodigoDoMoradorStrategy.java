package org.morah.morah.encomenda.strategy;

import java.time.Instant;
import java.util.Optional;

import org.morah.morah.encomenda.modelo.Encomenda;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Retirada com o codigo que o morador recebeu na notificacao de chegada da encomenda.
 *
 * <p>E a forma mais comum, por isso e a primeira da lista ({@code @Order(1)}). O codigo nao
 * expira: vale enquanto a encomenda estiver aguardando retirada (o service confere o status
 * antes de chamar as estrategias). Como nao sabemos quem da unidade veio buscar, nome e
 * documento ficam por conta do que o porteiro informar.
 */
@Component
@Order(1)
public class ValidacaoCodigoDoMoradorStrategy implements ValidacaoDeRetiradaStrategy {

    @Override
    public String forma() {
        return "codigo do morador";
    }

    @Override
    public Optional<RetiradaValidada> validar(Encomenda encomenda, String codigo, Instant agora) {
        if (encomenda.getCodigoRetirada() == null || !encomenda.getCodigoRetirada().equals(codigo)) {
            return Optional.empty();
        }
        return Optional.of(new RetiradaValidada(forma(), null, null, null));
    }
}
