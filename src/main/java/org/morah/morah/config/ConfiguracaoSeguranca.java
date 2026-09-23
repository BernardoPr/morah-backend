package org.morah.morah.config;

import org.morah.morah.seguranca.jwt.FiltroAutenticacaoJwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Configuracao de seguranca da API.
 *
 * <p>Regras principais:
 * <ul>
 *   <li>API sem sessao (STATELESS): a identificacao vem do JWT em cada chamada;</li>
 *   <li>CSRF desligado (nao ha formulario HTML, apenas clientes que enviam token);</li>
 *   <li>rotas publicas: login, refresh, health, Swagger e monitor;</li>
 *   <li>todo o resto exige token valido;</li>
 *   <li>{@code @EnableMethodSecurity} habilita o {@code @PreAuthorize} nos controllers.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class ConfiguracaoSeguranca {

    private static final String[] ROTAS_PUBLICAS = {
            "/auth/login",
            "/auth/refresh",
            "/actuator/health",
            "/actuator/info",
            "/monitor/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private final FiltroAutenticacaoJwt filtroAutenticacaoJwt;

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((requisicao, resposta, excecao) -> {
                            resposta.setStatus(HttpStatus.UNAUTHORIZED.value());
                            resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            resposta.getWriter().write("""
                                    {"title":"Nao autenticado","status":401,\
                                    "detail":"Envie um token valido no cabecalho Authorization."}""");
                        }))
                .addFilterBefore(filtroAutenticacaoJwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** BCrypt: algoritmo usado para gravar e conferir as senhas. */
    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }
}
