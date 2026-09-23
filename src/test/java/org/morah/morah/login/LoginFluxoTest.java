package org.morah.morah.login;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Teste de ponta a ponta do login (HTTP -> seguranca -> Template Method -> JWT).
 *
 * <p>O MongoDB e substituido por um repositorio "de mentira" ({@code @MockitoBean}),
 * entao o teste roda sem banco nenhum.
 */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class LoginFluxoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder codificadorDeSenha;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void prepararUsuario() {
        Usuario ana = new Usuario();
        ana.setId(1L);
        ana.setNome("Ana Souza");
        ana.setCpf("11111111111");
        ana.setEmail("ana@morah.com.br");
        ana.setSenhaHash(codificadorDeSenha.encode("morah1234"));
        ana.setVinculos(List.of(
                new VinculoPerfil(Perfil.MORADOR, 1L, "Residencial Morah", 101L, "Apto 101")));

        BDDMockito.given(usuarioRepository.findByCpf("11111111111")).willReturn(Optional.of(ana));
        BDDMockito.given(usuarioRepository.findById(1L)).willReturn(Optional.of(ana));
    }

    @Test
    @DisplayName("login devolve tokens e o contexto ativo; o token abre o /auth/me")
    void loginEDepoisMe() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"111.111.111-11","senha":"morah1234"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.contexto.perfil").value("morador"))
                .andExpect(jsonPath("$.contexto.unidadeId").value(101))
                .andReturn();

        String accessToken = com.jayway.jsonpath.JsonPath.read(
                resultado.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoa.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.perfisDisponiveis[0].condominioNome").value("Residencial Morah"));
    }

    @Test
    @DisplayName("senha errada devolve 401 no formato RFC 9457")
    void senhaErrada() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"11111111111","senha":"errada"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    @DisplayName("sem token, os endpoints protegidos devolvem 401")
    void semTokenNaoEntra() throws Exception {
        mockMvc.perform(get("/avisos")).andExpect(status().isUnauthorized());
    }
}
