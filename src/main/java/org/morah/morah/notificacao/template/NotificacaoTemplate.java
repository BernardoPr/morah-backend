package org.morah.morah.notificacao.template;

import java.time.Instant;

import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.usuario.modelo.Usuario;

/**
 * PADRAO DE PROJETO: TEMPLATE METHOD (exemplo 3 de 3).
 *
 * <p><b>Problema:</b> toda notificacao segue o mesmo roteiro (montar titulo, montar mensagem,
 * gravar no banco, despachar). Só muda o TEXTO e o canal preferido de cada tipo.
 *
 * <p><b>Solucao:</b> o metodo {@link #enviar} (final) guarda o roteiro; as filhas escrevem
 * apenas o titulo e a mensagem.
 *
 * <p><b>Template Method x Strategy:</b> os dois padroes aparecem juntos aqui e e uma boa
 * forma de entender a diferenca:
 * <ul>
 *   <li>Template Method (heranca) define <i>a ordem dos passos</i> do envio;</li>
 *   <li>Strategy (composicao) decide <i>como</i> o ultimo passo acontece - e-mail, push ou SMS.</li>
 * </ul>
 *
 * @param <D> dados necessarios para montar o texto (ex.: o Aviso publicado)
 */
public abstract class NotificacaoTemplate<D> {

    protected final NotificacaoRepository repositorio;
    protected final SeletorDeCanal seletorDeCanal;

    protected NotificacaoTemplate(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        this.repositorio = repositorio;
        this.seletorDeCanal = seletorDeCanal;
    }

    /** TEMPLATE METHOD: o roteiro do envio, igual para todos os tipos de notificacao. */
    public final Notificacao enviar(Usuario destinatario, D dados) {
        Notificacao notificacao = new Notificacao();
        notificacao.setDestinatarioId(destinatario.getId());
        notificacao.setTipo(tipo());                              // passo obrigatorio
        notificacao.setTitulo(montarTitulo(dados));               // passo obrigatorio
        notificacao.setMensagem(montarMensagem(destinatario, dados)); // passo obrigatorio
        notificacao.setReferenciaId(referenciaId(dados));         // hook
        notificacao.setCanal(escolherCanal(dados));               // hook
        notificacao.setEnviadaEm(Instant.now());

        Notificacao salva = repositorio.save(notificacao);

        // Ultimo passo delegado a uma STRATEGY:
        seletorDeCanal.obter(salva.getCanal()).enviar(salva, destinatario);
        return salva;
    }

    // ---------- passos obrigatorios ----------

    protected abstract TipoNotificacao tipo();

    protected abstract String montarTitulo(D dados);

    protected abstract String montarMensagem(Usuario destinatario, D dados);

    // ---------- hooks (sobrescreva so se precisar) ----------

    protected Long referenciaId(D dados) {
        return null;
    }

    protected CanalNotificacao escolherCanal(D dados) {
        return CanalNotificacao.PUSH;
    }
}
