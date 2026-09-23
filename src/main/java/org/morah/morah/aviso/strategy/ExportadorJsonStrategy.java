package org.morah.morah.aviso.strategy;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Exporta os avisos em JSON identado (para integrar com outro sistema).
 *
 * <p>O {@link ObjectMapper} vem do pacote {@code tools.jackson} porque o Spring Boot 4
 * usa o Jackson 3 - se voce ja programou com Boot 3, o import mudou de lugar.
 */
@Component
@RequiredArgsConstructor
public class ExportadorJsonStrategy implements ExportadorDeAvisosStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public String formato() {
        return "json";
    }

    @Override
    public String tipoDeConteudo() {
        return "application/json";
    }

    @Override
    public String exportar(List<AvisoResponse> avisos) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(avisos);
        } catch (JacksonException excecao) {
            throw new RegraDeNegocioException("Nao foi possivel gerar o arquivo JSON.");
        }
    }
}
