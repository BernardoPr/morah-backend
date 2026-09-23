package org.morah.morah.notificacao.template;

import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Notificacao enviada quando um usuario e cadastrado.
 * Preenche apenas os passos variaveis do {@link NotificacaoTemplate}.
 *
 * <p>Os dados ({@code D}) aqui sao o nome do condominio.
 */
@Component
public class NotificacaoDeBoasVindas extends NotificacaoTemplate<String> {

    public NotificacaoDeBoasVindas(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.AVISO;
    }

    @Override
    protected String montarTitulo(String nomeDoCondominio) {
        return "Bem-vindo(a) ao " + nomeDoCondominio;
    }

    @Override
    protected String montarMensagem(Usuario destinatario, String nomeDoCondominio) {
        return "Ola, %s! Seu acesso ao aplicativo do %s foi criado."
                .formatted(destinatario.getNome(), nomeDoCondominio);
    }

    /** Hook sobrescrito: quem acabou de ser cadastrado ainda nao instalou o app. */
    @Override
    protected CanalNotificacao escolherCanal(String nomeDoCondominio) {
        return CanalNotificacao.EMAIL;
    }
}
