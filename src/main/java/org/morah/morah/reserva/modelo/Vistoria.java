package org.morah.morah.reserva.modelo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vistoria de entrada ou de saida feita pela portaria no espaco reservado.
 *
 * <p>Fica embutida dentro do documento da reserva (no maximo duas por reserva, uma de cada
 * momento), entao nao vale uma colecao separada - mesmo raciocinio do {@code VinculoPerfil}
 * dentro do usuario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vistoria {

    private MomentoVistoria momento;
    private String observacao;
    private boolean dano;
    private List<String> fotosUrl = new ArrayList<>();

    private Instant registradaEm;
    private Long registradaPorId;
    private String registradaPorNome;
}
