package org.morah.morah.unidade;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.StatusSolicitacaoVinculo;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.modelo.VinculoPerfil;

/** Objetos prontos para os testes do modulo (mesma historia da carga inicial). */
public final class DadosDeTeste {

    public static final Long CONDOMINIO = 1L;
    public static final String NOME_DO_CONDOMINIO = "Residencial Morah";

    private DadosDeTeste() {
    }

    public static Unidade unidade(Long id, String identificacao) {
        Unidade unidade = new Unidade();
        unidade.setId(id);
        unidade.setCondominioId(CONDOMINIO);
        unidade.setBlocoId(id < 200 ? 1L : 2L);
        unidade.setBlocoNome(id < 200 ? "Bloco A" : "Bloco B");
        unidade.setIdentificacao(identificacao);
        unidade.setTipo("apartamento");
        unidade.setAreaM2(new BigDecimal("70"));
        unidade.setFracaoIdeal(new BigDecimal("0.23"));
        unidade.setStatus("ativa");
        return unidade;
    }

    public static UsuarioAutenticado logado(Long id, String nome, Perfil perfil, Long unidadeId) {
        return new UsuarioAutenticado(id, nome, "00000000000", perfil, CONDOMINIO, NOME_DO_CONDOMINIO, unidadeId, "jti");
    }

    /** Ana, inquilina do 101 (token de MORADOR). */
    public static UsuarioAutenticado ana() {
        return logado(1L, "Ana Souza", Perfil.MORADOR, 101L);
    }

    /** Bruno, proprietario do 101. */
    public static UsuarioAutenticado bruno() {
        return logado(4L, "Bruno Alves", Perfil.PROPRIETARIO, 101L);
    }

    /** Carlos com o perfil de sindico (sem unidade no token). */
    public static UsuarioAutenticado carlosSindico() {
        return logado(2L, "Carlos Lima", Perfil.SINDICO, null);
    }

    public static Usuario usuario(Long id, String nome, String cpf, VinculoPerfil... vinculos) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setCpf(cpf);
        usuario.setEmail(nome.toLowerCase().replace(' ', '.') + "@morah.com.br");
        usuario.setTelefone("51999990000");
        usuario.setAtivo(true);
        // List.of(...) e imutavel de proposito: o codigo nao pode tentar alterar a lista original.
        usuario.setVinculos(List.of(vinculos));
        return usuario;
    }

    public static VinculoPerfil moradorDa(Long unidadeId) {
        return new VinculoPerfil(Perfil.MORADOR, CONDOMINIO, NOME_DO_CONDOMINIO, unidadeId, "Apto " + unidadeId);
    }

    public static VinculoUnidade vinculo(Long id, Long unidadeId, String nome, String cpf, TipoVinculo tipo,
                                         boolean principal, LocalDate inicio, LocalDate fim) {
        VinculoUnidade vinculo = new VinculoUnidade();
        vinculo.setId(id);
        vinculo.setCondominioId(CONDOMINIO);
        vinculo.setUnidadeId(unidadeId);
        vinculo.setPessoaNome(nome);
        vinculo.setPessoaCpf(cpf);
        vinculo.setTipoVinculo(tipo);
        vinculo.setPrincipal(principal);
        vinculo.setInicio(inicio);
        vinculo.setFim(fim);
        vinculo.setAtivo(true);
        return vinculo;
    }

    public static SolicitacaoVinculo solicitacaoPendente(Long id, Long unidadeId, String nome, String cpf) {
        SolicitacaoVinculo solicitacao = new SolicitacaoVinculo();
        solicitacao.setId(id);
        solicitacao.setCondominioId(CONDOMINIO);
        solicitacao.setUnidadeId(unidadeId);
        solicitacao.setUnidadeIdentificacao("Apto " + unidadeId);
        solicitacao.setTipoVinculo(TipoVinculo.DEPENDENTE);
        solicitacao.setNomeSolicitado(nome);
        solicitacao.setCpf(cpf);
        solicitacao.setStatus(StatusSolicitacaoVinculo.PENDENTE);
        solicitacao.setSolicitadoEm(Instant.parse("2026-10-01T12:00:00Z"));
        solicitacao.setSolicitanteId(2L);
        solicitacao.setSolicitanteNome("Carlos Lima");
        return solicitacao;
    }
}
