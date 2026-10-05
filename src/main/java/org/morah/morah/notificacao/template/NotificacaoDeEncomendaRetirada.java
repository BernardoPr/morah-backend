package org.morah.morah.notificacao.template;

import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Avisa os moradores da unidade que a encomenda saiu da portaria e com quem.
 *
 * <p>Serve de recibo: se ninguem da unidade reconhecer quem retirou, o morador abre uma
 * ocorrencia (POST /encomendas/{id}/ocorrencias).
 */
@Component
public class NotificacaoDeEncomendaRetirada extends NotificacaoTemplate<Encomenda> {

    public NotificacaoDeEncomendaRetirada(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.ENCOMENDA;
    }

    @Override
    protected String montarTitulo(Encomenda encomenda) {
        return "Encomenda retirada";
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Encomenda encomenda) {
        String quem = encomenda.getRetiradaPorNome() == null || encomenda.getRetiradaPorNome().isBlank()
                ? "quem apresentou o codigo de retirada"
                : encomenda.getRetiradaPorNome();
        return "Encomenda retirada por %s.".formatted(quem);
    }

    @Override
    protected Long referenciaId(Encomenda encomenda) {
        return encomenda.getId();
    }
}
