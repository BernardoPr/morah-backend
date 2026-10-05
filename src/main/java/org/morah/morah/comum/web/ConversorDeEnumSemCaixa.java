package org.morah.morah.comum.web;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * Converte os parametros de URL nos enums do Java ignorando maiusculas/minusculas:
 * {@code ?status=aguardando_retirada} vira {@code AGUARDANDO_RETIRADA}.
 *
 * <p>O conversor padrao do Spring so aceita o nome exato da constante ({@code ?status=PENDENTE}),
 * mas o contrato usa os valores em minusculo. Registrado em {@code ConfiguracaoWeb}.
 *
 * <p>Por isso a convencao do projeto: o nome da constante e o valor do contrato em maiusculo
 * ("nao_compareceu" -> {@code NAO_COMPARECEU}).
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ConversorDeEnumSemCaixa implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> tipo) {
        return texto -> {
            String procurado = texto.trim();
            if (procurado.isEmpty()) {
                return null;
            }
            for (T constante : tipo.getEnumConstants()) {
                if (constante.name().equalsIgnoreCase(procurado)) {
                    return constante;
                }
            }
            throw new IllegalArgumentException("Valor invalido: " + texto);
        };
    }
}
