package org.morah.morah.comum;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Os erros da API precisam sair sempre no formato RFC 9457 exigido pelo contrato. */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class TratadorGlobalDeErrosTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("campos obrigatorios ausentes viram 400 com a lista de erros por campo")
    void dadosInvalidos() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados invalidos"))
                .andExpect(jsonPath("$.erros[0].campo").exists());
    }

    @Test
    @DisplayName("metodo HTTP errado vira 405, e nao 500")
    void metodoNaoSuportado() throws Exception {
        mockMvc.perform(post("/monitor/metricas"))
                .andExpect(status().isMethodNotAllowed());
    }
}
