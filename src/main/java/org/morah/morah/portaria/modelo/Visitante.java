package org.morah.morah.portaria.modelo;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "visitantes" (schema Visitante do contrato).
 *
 * <p>Cadastro de quem ja passou pela portaria do condominio. Quando o porteiro registra alguem
 * com um documento ja conhecido, o cadastro e reaproveitado (e atualizado) em vez de duplicado.
 */
@Getter
@Setter
@Document(collection = "visitantes")
public class Visitante extends EntidadeBase {

    @Indexed
    private Long condominioId;

    private String nome;

    /** RG/CPF guardado so com letras e numeros ("12.345.678-9" vira "123456789"). */
    @Indexed
    private String documento;

    private String telefone;

    /** Campo de saida do contrato; o contrato nao tem entrada para ele, entao fica nulo por ora. */
    private String observacao;
}
