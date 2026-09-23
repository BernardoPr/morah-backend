package org.morah.morah.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.config.PropriedadesMorah;
import org.morah.morah.seguranca.jwt.ServicoJwt;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;

/** O token gerado precisa carregar o contexto ativo (perfil + condominio + unidade). */
class ServicoJwtTest {

    private final ServicoJwt servicoJwt = new ServicoJwt(new PropriedadesMorah(
            new PropriedadesMorah.Jwt("segredo-de-teste-com-mais-de-32-caracteres", 60, 7),
            new PropriedadesMorah.Cors(List.of()),
            false));

    @Test
    void tokenCarregaOContextoAtivo() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNome("Ana Souza");
        usuario.setCpf("11111111111");

        var vinculo = new VinculoPerfil(Perfil.MORADOR, 1L, "Residencial Morah", 101L, "Apto 101");

        String token = servicoJwt.gerarAccessToken(usuario, vinculo);
        var autenticado = servicoJwt.paraUsuarioAutenticado(servicoJwt.lerClaims(token));

        assertThat(autenticado.id()).isEqualTo(7L);
        assertThat(autenticado.perfil()).isEqualTo(Perfil.MORADOR);
        assertThat(autenticado.condominioId()).isEqualTo(1L);
        assertThat(autenticado.unidadeId()).isEqualTo(101L);
    }
}
