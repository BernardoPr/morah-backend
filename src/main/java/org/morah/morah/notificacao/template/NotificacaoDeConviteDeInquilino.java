package org.morah.morah.notificacao.template;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * "Convite de acesso" enviado ao inquilino cadastrado pelo proprietario
 * (POST /unidades/minha/inquilinos).
 *
 * <p>Sai por e-mail (hook {@link #escolherCanal}): quem acabou de ser cadastrado ainda nao
 * instalou o app, entao um push nao chegaria. Quando o inquilino ja tinha cadastro, o convite
 * so avisa que a unidade nova apareceu no app; quando o cadastro foi criado agora, ele leva a
 * senha temporaria para o primeiro acesso.
 *
 * <p><b>Atencao (limitacao da base):</b> o texto da notificacao e gravado na colecao
 * "notificacoes" e o canal de e-mail ainda e simulado por log, entao a senha temporaria aparece
 * nesses dois lugares. Com um provedor de e-mail real, o ideal e trocar a senha por um link de
 * definicao de senha com validade curta.
 */
@Component
public class NotificacaoDeConviteDeInquilino extends NotificacaoTemplate<NotificacaoDeConviteDeInquilino.Convite> {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Dados do convite.
     *
     * @param senhaTemporaria preenchida so quando o usuario foi criado agora; nula se ele ja tinha acesso
     */
    public record Convite(Long vinculoId,
                          String nomeDoCondominio,
                          String identificacaoDaUnidade,
                          LocalDate contratoInicio,
                          LocalDate contratoFim,
                          String senhaTemporaria) {

        /** Nunca escreve a senha em log por acidente (o toString padrao do record a mostraria). */
        @Override
        public String toString() {
            return "Convite[vinculoId=%d, unidade=%s, senhaTemporaria=%s]"
                    .formatted(vinculoId, identificacaoDaUnidade, senhaTemporaria == null ? "nao" : "***");
        }
    }

    public NotificacaoDeConviteDeInquilino(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    /** O contrato nao tem um tipo "unidade"; o mais proximo e um aviso. */
    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.AVISO;
    }

    @Override
    protected String montarTitulo(Convite convite) {
        return "Convite de acesso - " + convite.nomeDoCondominio();
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Convite convite) {
        String periodo = convite.contratoFim() == null
                ? "a partir de " + convite.contratoInicio().format(DATA_BR)
                : "de %s a %s".formatted(convite.contratoInicio().format(DATA_BR), convite.contratoFim().format(DATA_BR));

        String comoEntrar = convite.senhaTemporaria() == null
                ? "Entre no app com seu CPF e a senha que voce ja usa."
                : "Entre no app com seu CPF e a senha temporaria " + convite.senhaTemporaria() + ".";

        return "Ola, %s! Voce foi cadastrado(a) como inquilino(a) da unidade %s do %s, contrato %s. %s"
                .formatted(destinatario.getNome(), convite.identificacaoDaUnidade(),
                        convite.nomeDoCondominio(), periodo, comoEntrar);
    }

    @Override
    protected Long referenciaId(Convite convite) {
        return convite.vinculoId();
    }

    /** Hook sobrescrito: convite vai por e-mail. */
    @Override
    protected CanalNotificacao escolherCanal(Convite convite) {
        return CanalNotificacao.EMAIL;
    }
}
