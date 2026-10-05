package org.morah.morah.unidade.dto;

import org.morah.morah.unidade.modelo.Cpf;
import org.morah.morah.unidade.modelo.TipoVinculo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /unidades/minha/vinculos/solicitacoes (schema SolicitacaoVinculoCreateRequest).
 *
 * <p>O CPF e opcional (um dependente crianca pode nao ter), mas, se vier, precisa ter 11
 * digitos - com ou sem pontuacao. CPF fora do formato vira 400 com o campo "cpf" no array
 * "erros" da resposta.
 */
public record SolicitacaoVinculoCreateRequest(
        @NotNull(message = "informe o tipo de vinculo") TipoVinculo tipoVinculo,
        @NotBlank(message = "informe o nome") @Size(max = 150, message = "o nome deve ter no maximo 150 caracteres") String nome,
        @Pattern(regexp = Cpf.FORMATO, message = Cpf.MENSAGEM_FORMATO) String cpf,
        @Size(max = 500, message = "a observacao deve ter no maximo 500 caracteres") String observacao) {
}
