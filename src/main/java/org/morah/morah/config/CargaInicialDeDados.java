package org.morah.morah.config;

import java.math.BigDecimal;
import java.util.List;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cria as unidades e os usuarios de teste na primeira execucao, para que qualquer pessoa
 * consiga fazer login logo depois de clonar o projeto.
 *
 * <p>Ligado/desligado pela propriedade {@code morah.carga-inicial} (desligado no perfil prod).
 * Roda antes das cargas dos outros modulos ({@code @Order(1)}), porque elas dependem das
 * unidades e dos usuarios criados aqui.
 *
 * <p>Unidades do condominio 1 (o id e o proprio numero do apartamento, para facilitar os testes):
 * 101 e 102 no Bloco A, 201 e 202 no Bloco B.
 *
 * <p>Usuarios criados (senha: <b>morah1234</b>):
 * <ul>
 *   <li><b>11111111111</b> - Ana, moradora (inquilina) do Apto 101;</li>
 *   <li><b>22222222222</b> - Carlos, sindico e tambem morador do Apto 202
 *       (use ele para testar o POST /auth/contexto);</li>
 *   <li><b>33333333333</b> - Joana, portaria;</li>
 *   <li><b>44444444444</b> - Bruno, proprietario do Apto 101 (alugado para a Ana).</li>
 * </ul>
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "morah.carga-inicial", havingValue = "true")
public class CargaInicialDeDados implements CommandLineRunner {

    private static final Long CONDOMINIO = 1L;
    private static final String NOME_DO_CONDOMINIO = "Residencial Morah";
    private static final String SENHA_PADRAO = "morah1234";

    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final PasswordEncoder codificadorDeSenha;

    @Override
    public void run(String... args) {
        criarUnidades();
        criarUsuarios();
    }

    private void criarUnidades() {
        if (unidadeRepository.countByCondominioId(CONDOMINIO) > 0) {
            return; // ja existem unidades, nao faz nada
        }

        unidadeRepository.saveAll(List.of(
                unidade(101L, 1L, "Bloco A", "Apto 101", "70", "0.23"),
                unidade(102L, 1L, "Bloco A", "Apto 102", "70", "0.23"),
                unidade(201L, 2L, "Bloco B", "Apto 201", "85", "0.27"),
                unidade(202L, 2L, "Bloco B", "Apto 202", "85", "0.27")));
        log.info("Carga inicial: unidades 101, 102 (Bloco A), 201 e 202 (Bloco B) criadas");
    }

    private void criarUsuarios() {
        criarSeNaoExistir("Ana Souza", "11111111111", "ana@morah.com.br", "51999990001",
                new VinculoPerfil(Perfil.MORADOR, CONDOMINIO, NOME_DO_CONDOMINIO, 101L, "Apto 101"));

        criarSeNaoExistir("Carlos Lima", "22222222222", "carlos@morah.com.br", "51999990002",
                new VinculoPerfil(Perfil.SINDICO, CONDOMINIO, NOME_DO_CONDOMINIO, null, null),
                new VinculoPerfil(Perfil.MORADOR, CONDOMINIO, NOME_DO_CONDOMINIO, 202L, "Apto 202"));

        criarSeNaoExistir("Joana Reis", "33333333333", "joana@morah.com.br", "51999990003",
                new VinculoPerfil(Perfil.PORTARIA, CONDOMINIO, NOME_DO_CONDOMINIO, null, null));

        criarSeNaoExistir("Bruno Alves", "44444444444", "bruno@morah.com.br", "51999990004",
                new VinculoPerfil(Perfil.PROPRIETARIO, CONDOMINIO, NOME_DO_CONDOMINIO, 101L, "Apto 101"));
    }

    /** Confere pelo CPF: assim um banco antigo ganha os usuarios novos sem duplicar os existentes. */
    private void criarSeNaoExistir(String nome, String cpf, String email, String telefone,
                                   VinculoPerfil... vinculos) {
        if (usuarioRepository.existsByCpf(cpf)) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setCpf(cpf);
        usuario.setEmail(email);
        usuario.setTelefone(telefone);
        usuario.setSenhaHash(codificadorDeSenha.encode(SENHA_PADRAO));
        usuario.setVinculos(List.of(vinculos));

        usuarioRepository.save(usuario);
        log.info("Carga inicial: usuario {} (CPF {}) criado, senha {}", nome, cpf, SENHA_PADRAO);
    }

    private Unidade unidade(Long id, Long blocoId, String blocoNome, String identificacao,
                            String areaM2, String fracaoIdeal) {
        Unidade unidade = new Unidade();
        unidade.setId(id);
        unidade.setCondominioId(CONDOMINIO);
        unidade.setBlocoId(blocoId);
        unidade.setBlocoNome(blocoNome);
        unidade.setIdentificacao(identificacao);
        unidade.setTipo("apartamento");
        unidade.setAreaM2(new BigDecimal(areaM2));
        unidade.setFracaoIdeal(new BigDecimal(fracaoIdeal));
        unidade.setStatus("ativa");
        return unidade;
    }
}
