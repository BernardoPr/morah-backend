package org.morah.morah.notificacao.template;

import java.time.format.DateTimeFormatter;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Avisa a unidade que o pagamento de uma cobranca foi confirmado - pelo gateway (webhook) ou
 * pela baixa manual do sindico. Canal padrao do template (push).
 */
@Component
public class NotificacaoDePagamentoConfirmado extends NotificacaoTemplate<Cobranca> {

    private static final DateTimeFormatter DIA_MES_ANO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public NotificacaoDePagamentoConfirmado(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.FINANCEIRO;
    }

    @Override
    protected String montarTitulo(Cobranca cobranca) {
        return "Pagamento confirmado: " + cobranca.getTaxaDescricao() + " (" + cobranca.getCompetencia() + ")";
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Cobranca cobranca) {
        return "Ola, %s! Recebemos o pagamento de %s em %s (%s). Obrigado!"
                .formatted(destinatario.getNome(),
                        NotificacaoDeCobrancaGerada.emReais(cobranca.getValorPago()),
                        Datas.dataDe(cobranca.getPagoEm()).format(DIA_MES_ANO),
                        cobranca.getMeio());
    }

    @Override
    protected Long referenciaId(Cobranca cobranca) {
        return cobranca.getId();
    }
}
