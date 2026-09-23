package org.morah.morah.usuario.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Corpo do POST /usuarios. */
public record UsuarioCreateRequest(
        @NotBlank(message = "informe o nome") String nome,

        @NotBlank(message = "informe o CPF")
        @Pattern(regexp = "[0-9]{11}", message = "o CPF deve ter 11 digitos, somente numeros")
        String cpf,

        @NotBlank(message = "informe o e-mail")
        @Email(message = "e-mail invalido")
        String email,

        String telefone,

        @NotBlank(message = "informe a senha")
        @Size(min = 8, message = "a senha deve ter ao menos 8 caracteres")
        String senha,

        @NotEmpty(message = "informe ao menos um vinculo")
        List<@Valid VinculoPerfilRequest> vinculos) {
}
