package org.morah.morah.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.financeiro.config.PropriedadesFinanceiro;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.usuario.modelo.Usuario;

/** Objetos prontos para os testes do modulo financeiro (sem banco, sem Spring). */
public final class DadosDeTeste {

    public static final String SEGREDO = "segredo-de-teste";

    private DadosDeTeste() {
    }

    /** Mesmos valores do application.yaml: multa 2%, juros 1% a.m., PIX valido por 30 min. */
    public static PropriedadesFinanceiro propriedades() {
        return new PropriedadesFinanceiro(SEGREDO, "financeiro@morah.com.br", 30,
                new BigDecimal("2.0"), new BigDecimal("1.0"));
    }

    public static Unidade unidade(Long id, String fracaoIdeal) {
        Unidade unidade = new Unidade();
        unidade.setId(id);
        unidade.setCondominioId(1L);
        unidade.setIdentificacao("Apto " + id);
        unidade.setFracaoIdeal(fracaoIdeal == null ? null : new BigDecimal(fracaoIdeal));
        unidade.setStatus("ativa");
        return unidade;
    }

    public static Cobranca cobranca(Long id, Long unidadeId, String valor, LocalDate vencimento) {
        Cobranca cobranca = new Cobranca();
        cobranca.setId(id);
        cobranca.setCondominioId(1L);
        cobranca.setUnidadeId(unidadeId);
        cobranca.setTaxaId(10L);
        cobranca.setTaxaDescricao("Taxa condominial");
        cobranca.setCompetencia(vencimento.toString().substring(0, 7));
        cobranca.setVencimento(vencimento);
        cobranca.setValorOriginal(new BigDecimal(valor));
        cobranca.setStatus(StatusCobranca.PENDENTE);
        return cobranca;
    }

    public static UsuarioAutenticado morador(Long unidadeId) {
        return new UsuarioAutenticado(1L, "Ana Souza", "11111111111", Perfil.MORADOR, 1L,
                "Residencial Morah", unidadeId, "jti-morador");
    }

    public static UsuarioAutenticado sindico() {
        return new UsuarioAutenticado(2L, "Carlos Lima", "22222222222", Perfil.SINDICO, 1L,
                "Residencial Morah", null, "jti-sindico");
    }

    public static Usuario usuario(Long id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail(nome.toLowerCase().replace(' ', '.') + "@morah.com.br");
        return usuario;
    }
}
