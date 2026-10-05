package org.morah.morah.unidade.modelo;

import java.time.LocalDate;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "vinculos_unidade" (schema VinculoUnidade do contrato): quem mora ou e dono da
 * unidade - proprietario, inquilino ou dependente.
 *
 * <p><b>Por que isso nao e o mesmo que o {@code VinculoPerfil} do usuario?</b>
 * <ul>
 *   <li>{@code VinculoPerfil} (dentro de {@code Usuario}) e o <i>acesso ao app</i>: e ele que o
 *       login consulta para montar o token (perfil + condominio + unidade);</li>
 *   <li>{@code VinculoUnidade} (esta classe) e o <i>cadastro da unidade</i>: inclui gente que
 *       nao usa o app (ex.: um filho pequeno, dependente sem CPF), guarda o periodo do contrato
 *       de locacao e o historico de quem ja morou ali.</li>
 * </ul>
 * Por isso {@code pessoaId} pode ficar nulo, e os dados basicos da pessoa (nome, CPF, telefone)
 * ficam copiados aqui.
 */
@Getter
@Setter
@Document(collection = "vinculos_unidade")
public class VinculoUnidade extends EntidadeBase {

    @Indexed
    private Long condominioId;

    @Indexed
    private Long unidadeId;

    /** Id do {@code Usuario} da pessoa; nulo quando ela nao tem acesso ao app. */
    private Long pessoaId;
    private String pessoaNome;
    private String pessoaCpf;
    private String pessoaTelefone;

    private TipoVinculo tipoVinculo;

    /** Responsavel principal pela unidade (ex.: o proprietario titular). */
    private boolean principal;

    private LocalDate inicio;

    /** Fim do vinculo (ex.: fim do contrato de locacao); nulo = sem prazo. */
    private LocalDate fim;

    /** Fica {@code false} quando o vinculo e encerrado - o registro nao e apagado (historico). */
    private boolean ativo = true;

    /** Ativo e ainda dentro do prazo: {@code fim} nulo ou maior/igual ao dia informado. */
    public boolean estaVigenteEm(LocalDate dia) {
        return ativo && (fim == null || !fim.isBefore(dia));
    }

    /** Mesma pessoa (pelo CPF) com o mesmo tipo de vinculo. */
    public boolean ehDaPessoaComTipo(String cpf, TipoVinculo tipo) {
        return cpf != null && cpf.equals(pessoaCpf) && tipoVinculo == tipo;
    }
}
