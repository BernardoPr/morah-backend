package org.morah.morah.unidade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.SolicitacaoVinculoRepository;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
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
 * Teste de ponta a ponta do modulo (HTTP -> seguranca -> service -> Template Method), com os
 * repositorios "de mentira". Confere perfis ({@code @PreAuthorize}), status HTTP do contrato e
 * o formato do JSON (valores dos enums em minusculo, datas ISO).
 */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class UnidadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnidadeRepository unidadeRepository;

    @MockitoBean
    private VinculoUnidadeRepository vinculoUnidadeRepository;

    @MockitoBean
    private SolicitacaoVinculoRepository solicitacaoVinculoRepository;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private NotificacaoRepository notificacaoRepository;

    @BeforeEach
    void prepararBanco() {
        given(unidadeRepository.findByIdAndCondominioId(101L, 1L))
                .willReturn(Optional.of(DadosDeTeste.unidade(101L, "Apto 101")));
        given(unidadeRepository.findByIdAndCondominioId(301L, 1L)).willReturn(Optional.empty());
        given(notificacaoRepository.save(any(Notificacao.class))).willAnswer(chamada -> chamada.getArgument(0));
    }

    /** Equivale a mandar um token com esse perfil (o proprietario tambem recebe ROLE_MORADOR). */
    private static RequestPostProcessor como(UsuarioAutenticado usuario, String... roles) {
        var permissoes = Arrays.stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null, permissoes));
    }

    private static RequestPostProcessor comoAna() {
        return como(DadosDeTeste.ana(), "MORADOR");
    }

    private static RequestPostProcessor comoBruno() {
        return como(DadosDeTeste.bruno(), "PROPRIETARIO", "MORADOR");
    }

    private static RequestPostProcessor comoSindico() {
        return como(DadosDeTeste.carlosSindico(), "SINDICO");
    }

    @Test
    @DisplayName("GET /unidades/minha: 200 para morador, 403 para sindico")
    void minhaUnidade() throws Exception {
        mockMvc.perform(get("/unidades/minha").with(comoAna()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.identificacao").value("Apto 101"))
                .andExpect(jsonPath("$.fracaoIdeal").value(0.23));

        mockMvc.perform(get("/unidades/minha").with(comoSindico()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /unidades/minha/vinculos/solicitacoes: 202 com status pendente; CPF invalido vira 400")
    void solicitarVinculo() throws Exception {
        given(solicitacaoVinculoRepository.save(any(SolicitacaoVinculo.class))).willAnswer(chamada -> {
            SolicitacaoVinculo solicitacao = chamada.getArgument(0);
            solicitacao.setId(10L);
            return solicitacao;
        });

        mockMvc.perform(post("/unidades/minha/vinculos/solicitacoes").with(comoAna())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoVinculo":"dependente","nome":"Pedro Souza","cpf":"123.456.789-01"}"""))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("pendente"))
                .andExpect(jsonPath("$.tipoVinculo").value("dependente"))
                .andExpect(jsonPath("$.unidadeId").value(101));

        mockMvc.perform(post("/unidades/minha/vinculos/solicitacoes").with(comoAna())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoVinculo":"dependente","nome":"Pedro Souza","cpf":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("cpf"));
    }

    @Test
    @DisplayName("POST /unidades/minha/inquilinos: so proprietario; 201 com o vinculo; periodo invertido vira 400")
    void cadastrarInquilino() throws Exception {
        String corpo = """
                {"nome":"Paula Dias","cpf":"555.555.555-55","contratoInicio":"2026-11-01","contratoFim":"2027-10-31"}""";

        mockMvc.perform(post("/unidades/minha/inquilinos").with(comoAna())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/unidades/minha/inquilinos").with(comoBruno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Paula Dias","cpf":"55555555555","contratoInicio":"2026-11-01","contratoFim":"2026-10-01"}"""))
                .andExpect(status().isBadRequest());

        Usuario paula = DadosDeTeste.usuario(50L, "Paula Dias", "55555555555");
        given(usuarioRepository.findByCpf("55555555555")).willReturn(Optional.of(paula));
        given(usuarioRepository.save(any(Usuario.class))).willAnswer(chamada -> chamada.getArgument(0));
        given(vinculoUnidadeRepository.save(any(VinculoUnidade.class))).willAnswer(chamada -> {
            VinculoUnidade vinculo = chamada.getArgument(0);
            vinculo.setId(77L);
            return vinculo;
        });

        mockMvc.perform(post("/unidades/minha/inquilinos").with(comoBruno())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.tipoVinculo").value("inquilino"))
                .andExpect(jsonPath("$.pessoa.id").value(50))
                .andExpect(jsonPath("$.pessoa.nome").value("Paula Dias"))
                .andExpect(jsonPath("$.inicio").value("2026-11-01"))
                .andExpect(jsonPath("$.fim").value("2027-10-31"));
    }

    @Test
    @DisplayName("DELETE /unidades/minha/inquilinos/{id}: 204 para o proprietario")
    void encerrarInquilino() throws Exception {
        VinculoUnidade contrato = DadosDeTeste.vinculo(3L, 101L, "Ana Souza", "11111111111",
                TipoVinculo.INQUILINO, false, LocalDate.of(2025, 3, 1), LocalDate.of(2027, 2, 28));
        contrato.setPessoaId(1L);
        given(vinculoUnidadeRepository.findByIdAndCondominioId(3L, 1L)).willReturn(Optional.of(contrato));
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(
                DadosDeTeste.usuario(1L, "Ana Souza", "11111111111", DadosDeTeste.moradorDa(101L))));

        mockMvc.perform(delete("/unidades/minha/inquilinos/3").with(comoBruno()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Swagger lista as rotas do modulo e nao mostra o metodo de validacao do periodo como campo")
    void documentacao() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/unidades/minha'].get").exists())
                .andExpect(jsonPath("$.paths['/unidades/minha/inquilinos/{vinculoId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/unidades/{unidadeId}/vinculos/solicitacoes/{solicitacaoId}'].patch").exists())
                .andExpect(jsonPath("$.components.schemas.InquilinoCreateRequest.properties.contratoInicio").exists())
                .andExpect(jsonPath("$.components.schemas.InquilinoCreateRequest.properties.periodoDoContratoValido")
                        .doesNotExist());
    }

    @Test
    @DisplayName("rotas do sindico: 404 para unidade de outro condominio, 400 para decisao invalida, 403 para morador")
    void rotasDoSindico() throws Exception {
        mockMvc.perform(get("/unidades/301").with(comoSindico()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/unidades/101/vinculos/solicitacoes").with(comoSindico()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(patch("/unidades/101/vinculos/solicitacoes/10").with(comoSindico())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decisao":"talvez"}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/unidades/101/vinculos/solicitacoes/10").with(comoAna())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decisao":"aprovada"}"""))
                .andExpect(status().isForbidden());
    }
}
