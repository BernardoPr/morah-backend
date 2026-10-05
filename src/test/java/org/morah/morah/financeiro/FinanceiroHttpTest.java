package org.morah.morah.financeiro;

import static org.morah.morah.financeiro.DadosDeTeste.cobranca;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.financeiro.servico.VerificadorDeAssinaturaHmac;
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
 * Teste de ponta a ponta (HTTP -> seguranca -> controller -> service) do modulo financeiro:
 * rota publica do webhook com HMAC, perfis do {@code @PreAuthorize}, validacao de entrada e o
 * JSON devolvido. O MongoDB e trocado por um repositorio de mentira ({@code @MockitoBean}).
 */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false",
        "morah.financeiro.webhook-segredo=" + DadosDeTeste.SEGREDO
})
@AutoConfigureMockMvc
class FinanceiroHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CobrancaRepository cobrancaRepository;

    private final VerificadorDeAssinaturaHmac verificador = new VerificadorDeAssinaturaHmac(DadosDeTeste.propriedades());

    private static RequestPostProcessor logadoComo(Perfil perfil, Long unidadeId) {
        var usuario = new UsuarioAutenticado(1L, "Teste", "00000000000", perfil, 1L, "Residencial Morah",
                unidadeId, "jti");
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null,
                List.of(new SimpleGrantedAuthority(perfil.comoRole()))));
    }

    // ---------- webhook (rota publica, autenticada por HMAC) ----------

    @Test
    @DisplayName("webhook sem assinatura: 401 no formato RFC 9457 (sem precisar de JWT)")
    void webhookSemAssinatura() throws Exception {
        mockMvc.perform(post("/financeiro/webhooks/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transacaoId\":\"tx\",\"cobrancaId\":1,\"status\":\"confirmado\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Nao autenticado"));
    }

    @Test
    @DisplayName("webhook assinado para cobranca inexistente: 202")
    void webhookAssinado() throws Exception {
        String corpo = "{\"transacaoId\":\"tx-1\",\"cobrancaId\":999,\"status\":\"confirmado\"}";
        BDDMockito.given(cobrancaRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(post("/financeiro/webhooks/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", "sha256=" + verificador.assinar(corpo))
                        .content(corpo))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("webhook assinado sem campo obrigatorio: 400 com a lista de erros")
    void webhookSemCampoObrigatorio() throws Exception {
        String corpo = "{\"cobrancaId\":1,\"status\":\"confirmado\"}";

        mockMvc.perform(post("/financeiro/webhooks/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", verificador.assinar(corpo))
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados invalidos"))
                .andExpect(jsonPath("$.erros[0].campo").value("transacaoId"));
    }

    // ---------- perfis e validacao ----------

    @Test
    @DisplayName("portaria nao lista cobrancas; morador nao ve inadimplencia nem cria taxa: 403")
    void perfisSemPermissao() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas").with(logadoComo(Perfil.PORTARIA, null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/financeiro/inadimplencia").with(logadoComo(Perfil.MORADOR, 101L)))
                .andExpect(status().isForbidden());
        // Corpo valido de proposito: o @Valid roda antes do @PreAuthorize (corpo invalido daria 400).
        mockMvc.perform(post("/financeiro/taxas").with(logadoComo(Perfil.MORADOR, 101L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Fundo de reserva","competencia":"2026-10","valor":300.00,
                                 "tipoRateio":"igualitario","vencimentoDia":10}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("competencia fora do formato AAAA-MM: 400")
    void competenciaInvalida() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas").param("competencia", "2026-13")
                        .with(logadoComo(Perfil.MORADOR, 101L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("taxa com vencimento no dia 31: 400 apontando o campo")
    void taxaInvalida() throws Exception {
        mockMvc.perform(post("/financeiro/taxas").with(logadoComo(Perfil.SINDICO, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descricao":"Fundo de reserva","competencia":"2026-10","valor":300.00,
                                 "tipoRateio":"igualitario","vencimentoDia":31}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("vencimentoDia"));
    }

    // ---------- detalhe: JSON do contrato e "quem ve o que" ----------

    @Test
    @DisplayName("detalhe: status 'atrasado' e encargos no JSON; cobranca de outra unidade da 403")
    void detalheDaCobranca() throws Exception {
        BDDMockito.given(cobrancaRepository.findByIdAndCondominioId(7L, 1L))
                .willReturn(Optional.of(cobranca(7L, 101L, "500.00", Datas.hoje().minusDays(15))));

        mockMvc.perform(get("/financeiro/cobrancas/7").with(logadoComo(Perfil.PROPRIETARIO, 101L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("atrasado"))
                .andExpect(jsonPath("$.multa").value(10.00))
                .andExpect(jsonPath("$.juros").value(2.50))
                .andExpect(jsonPath("$.valorTotal").value(512.50))
                .andExpect(jsonPath("$.vencimento").value(Datas.hoje().minusDays(15).toString()));

        mockMvc.perform(get("/financeiro/cobrancas/7").with(logadoComo(Perfil.MORADOR, 202L)))
                .andExpect(status().isForbidden());
    }
}
