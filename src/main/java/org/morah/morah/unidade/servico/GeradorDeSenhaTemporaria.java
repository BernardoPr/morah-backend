package org.morah.morah.unidade.servico;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

/**
 * Gera a senha temporaria do inquilino cadastrado pelo proprietario.
 *
 * <p>Usa {@link SecureRandom} (e nao {@code Random}/{@code Math.random()}): o gerador comum e
 * previsivel - quem descobre a semente consegue adivinhar as proximas senhas. O alfabeto deixa
 * de fora caracteres faceis de confundir ao digitar (0/O, 1/l/I).
 *
 * <p>E um {@code @Component} (e nao um metodo estatico) para os testes poderem trocar a senha
 * aleatoria por uma fixa.
 */
@Component
public class GeradorDeSenhaTemporaria {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TAMANHO = 12;

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerar() {
        StringBuilder senha = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            senha.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }
}
