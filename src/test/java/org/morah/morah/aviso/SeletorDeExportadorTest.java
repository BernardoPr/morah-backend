package org.morah.morah.aviso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.morah.morah.aviso.strategy.ExportadorCsvStrategy;
import org.morah.morah.aviso.strategy.SeletorDeExportador;
import org.morah.morah.comum.dto.PessoaResumo;
import org.morah.morah.comum.erro.RegraDeNegocioException;

class SeletorDeExportadorTest {

    private final SeletorDeExportador seletor = new SeletorDeExportador(List.of(new ExportadorCsvStrategy()));

    private final AvisoResponse aviso = new AvisoResponse(
            1L, "Manutencao do elevador; sabado", "Conteudo", PrioridadeAviso.URGENTE,
            new PessoaResumo(2L, "Carlos Lima", null), Instant.parse("2026-09-18T12:00:00Z"), null, false);

    @Test
    void exportaEmCsvComCabecalho() {
        String csv = seletor.obter("csv").exportar(List.of(aviso));

        assertThat(csv).startsWith("id;titulo;prioridade;autor;publicadoEm");
        // O ";" do titulo vira "," para nao quebrar as colunas.
        assertThat(csv).contains("1;Manutencao do elevador, sabado;urgente;Carlos Lima;");
    }

    @Test
    void formatoDesconhecidoViraErroDeNegocio() {
        assertThatThrownBy(() -> seletor.obter("pdf"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Formato nao suportado");
    }
}
