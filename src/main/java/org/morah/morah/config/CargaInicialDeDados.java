package org.morah.morah.config;

import java.util.List;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cria dois usuarios de teste na primeira execucao, para que qualquer pessoa consiga
 * fazer login logo depois de clonar o projeto.
 *
 * <p>Ligado/desligado pela propriedade {@code morah.carga-inicial} (desligado no perfil prod).
 *
 * <p>Usuarios criados (senha: <b>morah1234</b>):
 * <ul>
 *   <li><b>11111111111</b> - Ana, moradora do Apto 101;</li>
 *   <li><b>22222222222</b> - Carlos, sindico e tambem morador do Apto 202
 *       (use ele para testar o POST /auth/contexto).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "morah.carga-inicial", havingValue = "true")
public class CargaInicialDeDados implements CommandLineRunner {

    private static final Long CONDOMINIO = 1L;
    private static final String NOME_DO_CONDOMINIO = "Residencial Morah";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder codificadorDeSenha;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return; // ja existem dados, nao faz nada
        }

        Usuario ana = new Usuario();
        ana.setNome("Ana Souza");
        ana.setCpf("11111111111");
        ana.setEmail("ana@morah.com.br");
        ana.setTelefone("51999990001");
        ana.setSenhaHash(codificadorDeSenha.encode("morah1234"));
        ana.setVinculos(List.of(
                new VinculoPerfil(Perfil.MORADOR, CONDOMINIO, NOME_DO_CONDOMINIO, 101L, "Apto 101")));

        Usuario carlos = new Usuario();
        carlos.setNome("Carlos Lima");
        carlos.setCpf("22222222222");
        carlos.setEmail("carlos@morah.com.br");
        carlos.setTelefone("51999990002");
        carlos.setSenhaHash(codificadorDeSenha.encode("morah1234"));
        carlos.setVinculos(List.of(
                new VinculoPerfil(Perfil.SINDICO, CONDOMINIO, NOME_DO_CONDOMINIO, null, null),
                new VinculoPerfil(Perfil.MORADOR, CONDOMINIO, NOME_DO_CONDOMINIO, 202L, "Apto 202")));

        usuarioRepository.saveAll(List.of(ana, carlos));
        log.info("Carga inicial criada: CPFs 11111111111 (morador) e 22222222222 (sindico), senha morah1234");
    }
}
