package org.morah.morah.aviso.modelo;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.morah.morah.comum.modelo.EntidadeBase;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Colecao "avisos" (mural do condominio).
 *
 * <p>Segundo modelo de banco do projeto - serve de molde para as demais entidades
 * (cobrancas, reservas, encomendas...).
 */
@Getter
@Setter
@Document(collection = "avisos")
public class Aviso extends EntidadeBase {

    @Indexed
    private Long condominioId;

    private String titulo;
    private String conteudo;
    private PrioridadeAviso prioridade = PrioridadeAviso.NORMAL;
    private PublicoAlvo publicoAlvo;

    private Long autorId;
    private String autorNome;

    private Instant publicadoEm;
    private Instant expiraEm;

    /**
     * Ids dos usuarios que ja abriram o aviso (campo lidoPeloUsuario do contrato).
     *
     * <p>Fica embutido no proprio aviso: um condominio tem algumas centenas de pessoas, entao
     * a lista cabe com folga no documento e a listagem nao precisa consultar outra colecao.
     */
    private Set<Long> lidoPor = new HashSet<>();

    public boolean foiLidoPor(Long usuarioId) {
        return lidoPor != null && lidoPor.contains(usuarioId);
    }

    /** Fora de vigencia: tinha data de expiracao e ela ja passou. */
    public boolean expirouEm(Instant momento) {
        return expiraEm != null && !expiraEm.isAfter(momento);
    }
}
