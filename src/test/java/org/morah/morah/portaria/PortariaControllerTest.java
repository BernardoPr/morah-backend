package org.morah.morah.portaria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.comum.dto.PaginaMetadata;
import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.portaria.dto.AcessoResponse;
import org.morah.morah.portaria.dto.AutorizacaoVisitaResponse;
import org.morah.morah.portaria.dto.VisitanteResponse;
import org.morah.morah.portaria.modelo.StatusAcesso;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.morah.morah.portaria.modelo.TipoAcesso;
import org.morah.morah.portaria.servico.AcessoService;
import org.morah.morah.portaria.servico.AutorizacaoVisitaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Camada HTTP da portaria: perfis do contrato ({@code @PreAuthorize}), status HTTP, validacao e
 * os valores do JSON ("pendente", "em_andamento"...). Os services sao mockados - as regras de
 * negocio ja sao testadas nos testes de service.
 */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class PortariaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutorizacaoVisitaService autorizacaoVisitaService;

    @MockitoBean
    private AcessoService acessoService;

    private static RequestPostProcessor logadoComo(UsuarioAutenticado usuario, String... roles) {
        var permissoes = java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null, permissoes));
    }

    private static RequestPostProcessor porteira() {
        return logadoComo(PortariaFixtures.porteira(), "ROLE_PORTARIA");
    }

    private static RequestPostProcessor moradora() {
        return logadoComo(PortariaFixtures.moradora101(), "ROLE_MORADOR");
    }

    private static AutorizacaoVisitaResponse autorizacao(StatusAutorizacaoVisita status) {
        return new AutorizacaoVisitaResponse(1L, new VisitanteResponse(10L, "Joao Visitante", "123456789", null, null),
                PortariaFixtures.APTO_101, "Visita", Instant.parse("2026-10-04T15:00:00Z"), null, status);
    }

    @Test
    @DisplayName("POST /portaria/visitantes: portaria recebe 201 com o status em minusculo")
    void registrarVisitante() throws Exception {
        when(autorizacaoVisitaService.registrarVisitante(any(), any()))
                .thenReturn(autorizacao(StatusAutorizacaoVisita.PENDENTE));

        mockMvc.perform(post("/portaria/visitantes").with(porteira())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":101,"visitante":{"nome":"Joao Visitante","documento":"123456789"},"motivo":"Visita"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pendente"))
                .andExpect(jsonPath("$.visitante.nome").value("Joao Visitante"))
                .andExpect(jsonPath("$.unidadeId").value(101));
    }

    @Test
    @DisplayName("POST /portaria/visitantes: sem nome do visitante e sem motivo -> 400 com os campos")
    void registrarVisitanteInvalido() throws Exception {
        mockMvc.perform(post("/portaria/visitantes").with(porteira())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":101,"visitante":{"documento":"123"}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[?(@.campo == 'visitante.nome')]").exists())
                .andExpect(jsonPath("$.erros[?(@.campo == 'motivo')]").exists());
    }

    @Test
    @DisplayName("POST /portaria/visitantes: morador nao registra visitante -> 403")
    void moradorNaoRegistraVisitante() throws Exception {
        mockMvc.perform(post("/portaria/visitantes").with(moradora())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"unidadeId":101,"visitante":{"nome":"Joao"},"motivo":"Visita"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /portaria/autorizacoes?status=expirada: morador lista e o filtro chega como enum")
    void listarAutorizacoes() throws Exception {
        when(autorizacaoVisitaService.listar(any(), eq(StatusAutorizacaoVisita.EXPIRADA), any(Pageable.class)))
                .thenReturn(new PaginaResponse<>(List.of(autorizacao(StatusAutorizacaoVisita.EXPIRADA)),
                        new PaginaMetadata(0, 20, 1, 1)));

        mockMvc.perform(get("/portaria/autorizacoes").param("status", "expirada").with(moradora()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("expirada"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @DisplayName("PATCH .../decisao: proprietario decide; valor fora do enum -> 400; portaria -> 403")
    void decidir() throws Exception {
        when(autorizacaoVisitaService.decidir(any(), eq(1L), any()))
                .thenReturn(autorizacao(StatusAutorizacaoVisita.AUTORIZADA));

        mockMvc.perform(patch("/portaria/autorizacoes/1/decisao")
                        .with(logadoComo(PortariaFixtures.proprietario101(), "ROLE_PROPRIETARIO", "ROLE_MORADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decisao":"autorizado","justificativa":"Pode subir"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("autorizada"));

        mockMvc.perform(patch("/portaria/autorizacoes/1/decisao").with(moradora())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decisao":"talvez"}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/portaria/autorizacoes/1/decisao").with(porteira())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decisao":"autorizado"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH .../decisao: regra de negocio vira 409 no formato RFC 9457")
    void decisaoConflitante() throws Exception {
        when(autorizacaoVisitaService.decidir(any(), eq(1L), any()))
                .thenThrow(new RegraDeNegocioException("Esta autorizacao ja esta recusada e nao pode mais ser decidida."));

        mockMvc.perform(patch("/portaria/autorizacoes/1/decisao").with(moradora())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decisao":"recusado"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Operacao nao permitida"));
    }

    @Test
    @DisplayName("POST .../reenviar: portaria recebe 202")
    void reenviar() throws Exception {
        mockMvc.perform(post("/portaria/autorizacoes/1/reenviar").with(porteira()))
                .andExpect(status().isAccepted());

        verify(autorizacaoVisitaService).reenviar(any(), eq(1L));
    }

    @Test
    @DisplayName("GET /portaria/acessos?data=: sindico consulta; morador -> 403")
    void listarAcessos() throws Exception {
        when(acessoService.listar(any(), eq(LocalDate.of(2026, 10, 4)), any(Pageable.class)))
                .thenReturn(new PaginaResponse<>(List.of(), new PaginaMetadata(0, 20, 0, 0)));

        mockMvc.perform(get("/portaria/acessos").param("data", "2026-10-04")
                        .with(logadoComo(PortariaFixtures.sindico(), "ROLE_SINDICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/portaria/acessos").with(moradora()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /portaria/acessos: entrada registrada -> 201 com tipo e status do acesso")
    void registrarAcesso() throws Exception {
        when(acessoService.registrar(any(), any())).thenReturn(new AcessoResponse(20L, 1L,
                new VisitanteResponse(10L, "Joao Visitante", null, null, null),
                Instant.parse("2026-10-04T15:10:00Z"), null, TipoAcesso.ENTRADA, StatusAcesso.EM_ANDAMENTO));

        mockMvc.perform(post("/portaria/acessos").with(porteira())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visitanteId":10,"tipo":"entrada"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("entrada"))
                .andExpect(jsonPath("$.status").value("em_andamento"))
                .andExpect(jsonPath("$.autorizacaoId").value(1));
    }
}
