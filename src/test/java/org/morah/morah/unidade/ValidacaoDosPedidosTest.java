package org.morah.morah.unidade;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.unidade.dto.InquilinoCreateRequest;
import org.morah.morah.unidade.dto.SolicitacaoVinculoCreateRequest;
import org.morah.morah.unidade.modelo.Cpf;
import org.morah.morah.unidade.modelo.TipoVinculo;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** Regras de formato que viram 400 pelo {@code @Valid} (antes de chegar ao service). */
class ValidacaoDosPedidosTest {

    private static final ValidatorFactory FABRICA = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDADOR = FABRICA.getValidator();

    @AfterAll
    static void fechar() {
        FABRICA.close();
    }

    private static Set<String> camposInvalidos(Object requisicao) {
        Set<ConstraintViolation<Object>> violacoes = VALIDADOR.validate(requisicao);
        return violacoes.stream().map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    @DisplayName("CPF da solicitacao e opcional, mas se vier precisa ter 11 digitos (com ou sem pontuacao)")
    void cpfDaSolicitacao() {
        assertThat(camposInvalidos(new SolicitacaoVinculoCreateRequest(TipoVinculo.DEPENDENTE, "Lucas", null, null))).isEmpty();
        assertThat(camposInvalidos(new SolicitacaoVinculoCreateRequest(TipoVinculo.DEPENDENTE, "Lucas", "", null))).isEmpty();
        assertThat(camposInvalidos(new SolicitacaoVinculoCreateRequest(TipoVinculo.DEPENDENTE, "Lucas", "123.456.789-01", null))).isEmpty();
        assertThat(camposInvalidos(new SolicitacaoVinculoCreateRequest(TipoVinculo.DEPENDENTE, "Lucas", "1234", null)))
                .containsExactly("cpf");
        assertThat(camposInvalidos(new SolicitacaoVinculoCreateRequest(null, " ", null, null)))
                .containsExactlyInAnyOrder("tipoVinculo", "nome");
    }

    @Test
    @DisplayName("contrato do inquilino: fim antes do inicio e invalido; sem fim e valido")
    void periodoDoContrato() {
        LocalDate inicio = LocalDate.of(2026, 11, 1);

        assertThat(camposInvalidos(new InquilinoCreateRequest("Paula", "55555555555", null, null, inicio, null))).isEmpty();
        assertThat(camposInvalidos(new InquilinoCreateRequest("Paula", "55555555555", null, null, inicio, inicio))).isEmpty();
        assertThat(camposInvalidos(new InquilinoCreateRequest("Paula", "55555555555", null, null, inicio, inicio.minusDays(1))))
                .containsExactly("periodoDoContratoValido");
        assertThat(camposInvalidos(new InquilinoCreateRequest("Paula", "", "nao-e-email", null, null, null)))
                .containsExactlyInAnyOrder("cpf", "email", "contratoInicio");
    }

    @Test
    @DisplayName("CPF e gravado so com digitos; em branco vira nulo")
    void normalizaCpf() {
        assertThat(Cpf.normalizar("123.456.789-01")).isEqualTo("12345678901");
        assertThat(Cpf.normalizar("  ")).isNull();
        assertThat(Cpf.normalizar(null)).isNull();
    }
}
