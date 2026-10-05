package org.morah.morah.portaria.modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia dos dados do visitante guardada dentro da autorizacao e do acesso.
 *
 * <p>Por que copiar? A autorizacao e o acesso sao registros historicos: se o visitante trocar de
 * telefone amanha, o historico de ontem deve continuar mostrando os dados daquele dia. E de quebra
 * as listagens nao precisam consultar a colecao "visitantes" para cada linha.
 *
 * <p>Fica embutido no documento (sem colecao propria), como o {@code VinculoPerfil} do usuario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DadosDoVisitante {

    private String nome;
    private String documento;
    private String telefone;
    private String observacao;

    public static DadosDoVisitante de(Visitante visitante) {
        return new DadosDoVisitante(
                visitante.getNome(),
                visitante.getDocumento(),
                visitante.getTelefone(),
                visitante.getObservacao());
    }
}
