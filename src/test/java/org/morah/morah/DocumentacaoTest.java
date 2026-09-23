package org.morah.morah;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Garante que a documentacao (Swagger) continua sendo gerada com os endpoints da API. */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class DocumentacaoTest {

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
}
