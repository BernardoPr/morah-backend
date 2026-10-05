package org.morah.morah.unidade.dto;

import java.time.LocalDate;

import org.morah.morah.unidade.modelo.Cpf;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST /unidades/minha/inquilinos (schema InquilinoCreateRequest).
 *
 * <p>O e-mail e opcional no contrato, mas passa a ser obrigatorio quando o inquilino ainda nao
 * tem cadastro no app: e por ele que sai o convite de acesso. Essa regra depende do banco, entao
 * fica no service ({@code ConcessaoPorCadastroDeInquilino}), e nao aqui.
 */
public record InquilinoCreateRequest(
        @NotBlank(message = "informe o nome") @Size(max = 150, message = "o nome deve ter no maximo 150 caracteres") String nome,

        @NotBlank(message = "informe o CPF")
        @Pattern(regexp = Cpf.FORMATO, message = Cpf.MENSAGEM_FORMATO)
        String cpf,

        @Email(message = "e-mail invalido") String email,
        String telefone,
        @NotNull(message = "informe o inicio do contrato") LocalDate contratoInicio,
        LocalDate contratoFim) {

    /**
     * Regra "contratoFim >= contratoInicio" verificada pelo {@code @Valid}, junto com as demais:
     * assim o erro sai como 400 (dado invalido) com o campo no array "erros", e nao como 409.
     * O {@link JsonIgnore} impede que esse metodo apareca no JSON e no Swagger.
     */
    @JsonIgnore
    @AssertTrue(message = "o fim do contrato deve ser igual ou posterior ao inicio")
    public boolean isPeriodoDoContratoValido() {
        return contratoInicio == null || contratoFim == null || !contratoFim.isBefore(contratoInicio);
    }
}
