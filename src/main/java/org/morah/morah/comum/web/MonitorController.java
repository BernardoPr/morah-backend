package org.morah.morah.comum.web;

import java.util.Map;

import org.morah.morah.comum.singleton.ContadorDeRequisicoesSingleton;
import org.morah.morah.seguranca.singleton.RegistroDeTokensRevogadosSingleton;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoint de apoio (nao faz parte do contrato com o front) que mostra os dois
 * singletons funcionando. Util em sala de aula e na demonstracao do trabalho.
 */
@Tag(name = "Monitor", description = "Endpoints auxiliares de demonstracao (fora do contrato)")
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Operation(summary = "Estado atual dos singletons da aplicacao")
    @GetMapping("/metricas")
    public Map<String, Object> metricas() {
        ContadorDeRequisicoesSingleton contador = ContadorDeRequisicoesSingleton.getInstancia();
        RegistroDeTokensRevogadosSingleton tokens = RegistroDeTokensRevogadosSingleton.getInstancia();

        return Map.of(
                "totalDeRequisicoes", contador.getTotal(),
                "requisicoesPorRota", contador.getTotaisPorRota(),
                "segundosNoAr", contador.getTempoDeAtividade().toSeconds(),
                "tokensRevogados", tokens.getQuantidade());
    }
}
