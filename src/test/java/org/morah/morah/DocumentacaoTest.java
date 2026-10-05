package org.morah.morah;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

import com.jayway.jsonpath.JsonPath;

/** Garante que a documentacao (Swagger) continua sendo gerada com os endpoints da API. */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class DocumentacaoTest {

    private static final Set<String> METODOS_HTTP = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("o /v3/api-docs lista os endpoints de login")
    void documentacaoDisponivel() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Morah API"))
                .andExpect(jsonPath("$.paths./auth/login").exists())
                .andExpect(jsonPath("$.paths./home/dashboard").exists());
    }

    /**
     * Confere a API contra o contrato combinado com o front: toda operacao (metodo + rota) do
     * {@code Documentos/morah-api.yaml} precisa existir na documentacao gerada pela aplicacao.
     * Os nomes dos parametros de rota sao ignorados ({cobrancaId} e {id} contam como iguais).
     */
    @Test
    @DisplayName("todas as operacoes do contrato existem na API")
    @SuppressWarnings("unchecked")
    void todasAsOperacoesDoContratoExistem() throws Exception {
        Map<String, Object> contrato;
        try (InputStream arquivo = Files.newInputStream(Path.of("Documentos", "morah-api.yaml"))) {
            contrato = new Yaml().load(arquivo);
        }
        Set<String> esperadas = operacoes((Map<String, Map<String, Object>>) contrato.get("paths"));

        String apiDocs = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Set<String> geradas = operacoes(JsonPath.read(apiDocs, "$.paths"));

        assertThat(esperadas).hasSizeGreaterThanOrEqualTo(50); // garante que o arquivo foi lido
        assertThat(geradas).containsAll(esperadas);
    }

    /** Transforma o bloco "paths" do OpenAPI em textos como "GET /financeiro/cobrancas/{}". */
    private Set<String> operacoes(Map<String, Map<String, Object>> caminhos) {
        Set<String> operacoes = new TreeSet<>();
        caminhos.forEach((rota, metodos) -> metodos.keySet().stream()
                .filter(METODOS_HTTP::contains)
                .forEach(metodo -> operacoes.add(metodo.toUpperCase() + " " + rota.replaceAll("\\{[^}]+}", "{}"))));
        return operacoes;
    }
}
