package org.morah.morah.config;

import org.morah.morah.comum.web.ConversorDeEnumSemCaixa;
import org.morah.morah.comum.web.InterceptadorDeMetricas;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/** Registra o interceptador de metricas, o conversor de enums e libera o front (CORS). */
@Configuration
@RequiredArgsConstructor
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private final InterceptadorDeMetricas interceptadorDeMetricas;
    private final PropriedadesMorah propriedades;

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(interceptadorDeMetricas);
    }

    /** Aceita ?status=pendente (como no contrato) e nao so ?status=PENDENTE. */
    @Override
    public void addFormatters(FormatterRegistry registro) {
        registro.addConverterFactory(new ConversorDeEnumSemCaixa());
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/**")
                .allowedOrigins(propriedades.cors().origens().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
