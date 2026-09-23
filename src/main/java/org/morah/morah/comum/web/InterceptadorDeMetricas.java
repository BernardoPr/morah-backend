package org.morah.morah.comum.web;

import org.morah.morah.comum.singleton.ContadorDeRequisicoesSingleton;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Roda antes de cada requisicao e avisa o {@link ContadorDeRequisicoesSingleton}.
 * Registrado em {@code ConfiguracaoWeb}.
 */
@Component
public class InterceptadorDeMetricas implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest requisicao, HttpServletResponse resposta, Object handler) {
        ContadorDeRequisicoesSingleton.getInstancia()
                .registrar(requisicao.getMethod(), requisicao.getRequestURI());
        return true; // true = segue para o controller
    }
}
