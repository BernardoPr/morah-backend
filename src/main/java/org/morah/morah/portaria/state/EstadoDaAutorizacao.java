package org.morah.morah.portaria.state;

import java.time.Instant;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.DecisaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;

/**
 * PADRAO DE PROJETO: STATE (exemplo 1 de 1) - estados da autorizacao de visita.
 *
 * <p><b>Problema:</b> a autorizacao passa por um ciclo de vida (pendente -> autorizada / recusada /
 * expirada) e cada operacao so vale em alguns status: so a pendente pode ser decidida, reenviada ou
 * expirar; so a autorizada libera a entrada do visitante. Escrito com {@code if/switch}, o service
 * teria em cada metodo um bloco "se status == X ... senao se status == Y ...", repetido e facil de
 * esquecer quando um status novo aparecer.
 *
 * <p><b>Solucao:</b> cada status vira uma classe que implementa esta interface e responde as
 * operacoes do seu jeito: aceita (e faz a transicao, trocando o status da autorizacao) ou recusa
 * com um 409 explicando o motivo. O comportamento padrao abaixo e "recusar"; cada estado
 * sobrescreve apenas o que ele aceita. O service nao pergunta "qual e o status?": ele pede o
 * estado atual ao {@link SeletorDeEstadoDaAutorizacao} e chama a operacao.
 *
 * <p><b>State x Strategy:</b> a estrutura e parecida (interface + implementacoes + seletor), mas a
 * intencao e outra. Na Strategy quem escolhe o algoritmo e o cliente (o perfil do token, o
 * {@code ?formato=}); no State quem decide o comportamento e o <i>estado interno do objeto</i>, e
 * o proprio estado provoca a troca para o proximo (a pendente, ao ser decidida, vira autorizada).
 *
 * <p><b>Contexto:</b> o "contexto" do GoF e a {@link AutorizacaoVisita}. Como ela e gravada no
 * MongoDB, guardamos so o enum {@code status}; o seletor devolve o objeto-estado correspondente.
 *
 * <p><b>Quem usa:</b> {@code AutorizacaoVisitaService} (decidir, reenviar, expirar e liberar a
 * entrada pedida pelo {@code AcessoService}).
 */
public interface EstadoDaAutorizacao {

    /** Status que este estado representa. */
    StatusAutorizacaoVisita status();

    /**
     * Expiracao preguicosa: se a autorizacao venceu, passa para EXPIRADA.
     *
     * @return {@code true} se o status mudou (o chamador deve gravar)
     */
    default boolean expirarSeVencida(AutorizacaoVisita autorizacao, Instant agora) {
        return false;
    }

    /** Resposta do morador (PATCH .../decisao). */
    default void decidir(AutorizacaoVisita autorizacao, DecisaoVisita decisao, Instant agora) {
        throw new RegraDeNegocioException(
                "Esta autorizacao ja esta " + status().getValor() + " e nao pode mais ser decidida.");
    }

    /** Novo envio da notificacao ao morador (POST .../reenviar). */
    default void reenviar(AutorizacaoVisita autorizacao, Instant agora) {
        throw new RegraDeNegocioException(
                "So autorizacoes pendentes podem ser reenviadas; esta esta " + status().getValor() + ".");
    }

    /** Porteiro registrando a entrada do visitante com esta autorizacao (POST /portaria/acessos). */
    default void registrarEntrada(AutorizacaoVisita autorizacao, Instant agora) {
        throw new RegraDeNegocioException(
                "A autorizacao esta " + status().getValor() + ": a entrada nao pode ser liberada.");
    }
}
