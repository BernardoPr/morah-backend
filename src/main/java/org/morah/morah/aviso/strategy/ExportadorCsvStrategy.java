package org.morah.morah.aviso.strategy;

import java.util.List;

import org.morah.morah.aviso.dto.AvisoResponse;
import org.springframework.stereotype.Component;

/** Exporta os avisos em CSV (abre direto no Excel). */
@Component
public class ExportadorCsvStrategy implements ExportadorDeAvisosStrategy {

    @Override
    public String formato() {
        return "csv";
    }

    @Override
    public String tipoDeConteudo() {
        return "text/csv";
    }

    @Override
    public String exportar(List<AvisoResponse> avisos) {
        StringBuilder csv = new StringBuilder("id;titulo;prioridade;autor;publicadoEm\n");
        for (AvisoResponse aviso : avisos) {
            csv.append(aviso.id()).append(';')
                    .append(limpar(aviso.titulo())).append(';')
                    .append(aviso.prioridade().getValor()).append(';')
                    .append(limpar(aviso.autor().nome())).append(';')
                    .append(aviso.publicadoEm())
                    .append('\n');
        }
        return csv.toString();
    }

    /** Evita quebrar as colunas quando o texto tem ";" ou quebra de linha. */
    private String limpar(String texto) {
        return texto == null ? "" : texto.replace(";", ",").replace("\n", " ");
    }
}
