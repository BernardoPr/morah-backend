package org.morah.morah.aviso.strategy;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.springframework.stereotype.Component;

/** Escolhe o exportador pelo nome do formato recebido na requisicao. */
@Component
public class SeletorDeExportador {

    private final Map<String, ExportadorDeAvisosStrategy> estrategias;

    public SeletorDeExportador(List<ExportadorDeAvisosStrategy> exportadores) {
        this.estrategias = exportadores.stream()
                .collect(Collectors.toMap(ExportadorDeAvisosStrategy::formato, Function.identity()));
    }

    public ExportadorDeAvisosStrategy obter(String formato) {
        ExportadorDeAvisosStrategy estrategia =
                estrategias.get(formato == null ? "" : formato.toLowerCase(Locale.ROOT));

        if (estrategia == null) {
            throw new RegraDeNegocioException(
                    "Formato nao suportado. Use um destes: " + estrategias.keySet());
        }
        return estrategia;
    }
}
