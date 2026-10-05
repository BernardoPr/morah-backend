package org.morah.morah.reserva;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.reserva.modelo.AreaComum;
import org.morah.morah.reserva.modelo.Reserva;
import org.morah.morah.reserva.modelo.StatusReserva;
import org.morah.morah.reserva.repositorio.AreaComumRepository;
import org.morah.morah.reserva.repositorio.ReservaRepository;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Camada HTTP do modulo: perfis do contrato ({@code @PreAuthorize}), formato do JSON e erros 400.
 * As regras de negocio ficam nos testes de service (bem mais rapidos).
 */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class ReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaRepository reservaRepository;

    @MockitoBean
    private AreaComumRepository areaComumRepository;

    private static RequestPostProcessor como(Perfil perfil, Long unidadeId) {
        var usuario = new UsuarioAutenticado(1L, "Teste", "11111111111", perfil, 1L, "Residencial Morah",
                unidadeId, "jti");
        return authentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority(perfil.comoRole()))));
    }

    @Test
    @DisplayName("GET /areas-comuns devolve um array no formato do schema AreaComum")
    void listaAreas() throws Exception {
        AreaComum salao = new AreaComum();
        salao.setId(1L);
        salao.setNome("Salao de festas");
        salao.setCapacidade(80);
        salao.setTaxa(new BigDecimal("150"));
        salao.setHoraAbertura("10:00");
        salao.setHoraFechamento("22:00");
        BDDMockito.given(areaComumRepository.findByCondominioIdOrderByNome(1L)).willReturn(List.of(salao));

        mockMvc.perform(get("/areas-comuns").with(como(Perfil.PORTARIA, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Salao de festas"))
                .andExpect(jsonPath("$[0].taxa").value(150.0))
                .andExpect(jsonPath("$[0].requerReserva").value(true))
                .andExpect(jsonPath("$[0].status").value("ativa"))
                .andExpect(jsonPath("$[0].horaAbertura").doesNotExist());
    }

    @Test
    @DisplayName("GET /reservas/{id} usa os valores do contrato (status em minusculo, solicitante resumido)")
    void detalheNoFormatoDoContrato() throws Exception {
        Reserva reserva = new Reserva();
        reserva.setId(7L);
        reserva.setCondominioId(1L);
        reserva.setAreaComumId(1L);
        reserva.setAreaComumNome("Salao de festas");
        reserva.setUnidadeId(101L);
        reserva.setSolicitanteId(1L);
        reserva.setSolicitanteNome("Ana Souza");
        reserva.setInicio(Instant.parse("2026-10-05T13:00:00Z"));
        reserva.setFim(Instant.parse("2026-10-05T17:00:00Z"));
        reserva.setStatus(StatusReserva.NAO_COMPARECEU);
        reserva.setValor(new BigDecimal("150"));
        BDDMockito.given(reservaRepository.findByIdAndCondominioId(7L, 1L)).willReturn(Optional.of(reserva));

        mockMvc.perform(get("/reservas/7").with(como(Perfil.SINDICO, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("nao_compareceu"))
                .andExpect(jsonPath("$.solicitante.nome").value("Ana Souza"))
                .andExpect(jsonPath("$.inicio").value("2026-10-05T13:00:00Z"))
                .andExpect(jsonPath("$.valor").value(150.0))
                .andExpect(jsonPath("$.vistorias").doesNotExist());
    }

    @Test
    @DisplayName("perfis fora do contrato recebem 403")
    void perfisErrados() throws Exception {
        mockMvc.perform(post("/reservas").with(como(Perfil.PORTARIA, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"areaComumId":1,"inicio":"2030-01-01T13:00:00Z","fim":"2030-01-01T17:00:00Z"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/reservas/7/vistoria").with(como(Perfil.MORADOR, 101L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"momento":"saida"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/reservas/7").with(como(Perfil.SINDICO, null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/areas-comuns/1/disponibilidade").param("data", "2030-01-01")
                        .with(como(Perfil.PORTARIA, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("dados invalidos recebem 400 no formato RFC 9457")
    void dadosInvalidos() throws Exception {
        mockMvc.perform(get("/areas-comuns/1/disponibilidade").param("data", "amanha")
                        .with(como(Perfil.MORADOR, 101L)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/reservas").with(como(Perfil.MORADOR, 101L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"areaComumId":1,"fim":"2030-01-01T17:00:00Z"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("inicio"));

        mockMvc.perform(post("/reservas/7/vistoria").with(como(Perfil.PORTARIA, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"momento":"talvez"}"""))
                .andExpect(status().isBadRequest());
    }
}
