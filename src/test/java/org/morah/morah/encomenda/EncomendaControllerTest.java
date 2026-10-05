package org.morah.morah.encomenda;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.encomenda.modelo.StatusEncomenda;
import org.morah.morah.encomenda.repositorio.AutorizacaoRetiradaRepository;
import org.morah.morah.encomenda.repositorio.EncomendaRepository;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Teste HTTP do modulo: perfis do {@code @PreAuthorize}, validacao dos DTOs (400) e formato do
 * JSON (status em minusculo, codigo de retirada oculto para a portaria). Sem banco: os
 * repositorios sao substituidos por mocks.
 */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class EncomendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EncomendaRepository encomendaRepository;

    @MockitoBean
    private AutorizacaoRetiradaRepository autorizacaoRetiradaRepository;

    @MockitoBean
    private UnidadeRepository unidadeRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("portaria registra (201) e a resposta nao traz o codigo de retirada")
    void portariaRegistra() throws Exception {
        given(unidadeRepository.findByIdAndCondominioId(101L, 1L)).willReturn(Optional.of(new Unidade()));
        given(encomendaRepository.save(any(Encomenda.class))).willAnswer(chamada -> {
            Encomenda encomenda = chamada.getArgument(0);
            encomenda.setId(10L);
            return encomenda;
        });

        mockMvc.perform(post("/encomendas").with(como(Perfil.PORTARIA, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":101,"transportadora":"Correios","remetente":"Loja X"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("aguardando_retirada"))
                .andExpect(jsonPath("$.codigoRetirada").value(nullValue()));
    }

    @Test
    @DisplayName("morador nao registra encomenda (403)")
    void moradorNaoRegistra() throws Exception {
        mockMvc.perform(post("/encomendas").with(como(Perfil.MORADOR, 101L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":101,"transportadora":"Correios"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("morador lista a propria unidade filtrando por status em minusculo e ve o codigo")
    void moradorLista() throws Exception {
        Encomenda encomenda = new Encomenda();
        encomenda.setId(10L);
        encomenda.setCondominioId(1L);
        encomenda.setUnidadeId(101L);
        encomenda.setTransportadora("Correios");
        encomenda.setCodigoRetirada("123456");
        encomenda.setRecebidaEm(Instant.parse("2026-10-04T12:00:00Z"));
        given(encomendaRepository.findByCondominioIdAndUnidadeIdAndStatus(
                eq(1L), eq(101L), eq(StatusEncomenda.AGUARDANDO_RETIRADA), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(encomenda)));

        mockMvc.perform(get("/encomendas?status=aguardando_retirada").with(como(Perfil.MORADOR, 101L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoRetirada").value("123456"))
                .andExpect(jsonPath("$.content[0].status").value("aguardando_retirada"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("portaria nao autoriza terceiro (403) e validade fora de 1..168h vira 400")
    void autorizacaoDeTerceiro() throws Exception {
        // O @Valid roda antes do @PreAuthorize: para testar o 403, o corpo precisa ser valido.
        mockMvc.perform(post("/encomendas/10/autorizacoes-retirada").with(como(Perfil.PORTARIA, null))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nomeTerceiro":"Marcos","documento":"RG 123"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/encomendas/10/autorizacoes-retirada").with(como(Perfil.MORADOR, 101L))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nomeTerceiro":"Marcos","documento":"RG 123","validadeHoras":200}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("retirada sem codigo vira 400; morador nao confirma retirada (403)")
    void retirada() throws Exception {
        mockMvc.perform(post("/encomendas/10/retirada").with(como(Perfil.PORTARIA, null))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/encomendas/10/retirada").with(como(Perfil.MORADOR, 101L))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"codigo":"123456"}"""))
                .andExpect(status().isForbidden());
    }

    /** Autentica direto no contexto de seguranca, sem gerar JWT. */
    private static RequestPostProcessor como(Perfil perfil, Long unidadeId) {
        var usuario = new UsuarioAutenticado(3L, "Usuario de Teste", "33333333333",
                perfil, 1L, "Residencial Morah", unidadeId, "jti");
        return authentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority(perfil.comoRole()))));
    }
}
