package org.morah.morah.notificacao.template;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.usuario.modelo.Usuario;
import org.springframework.stereotype.Component;

/**
 * Avisa os moradores/proprietarios da unidade que um novo boleto foi gerado
 * (POST /financeiro/taxas/{taxaId}/gerar-cobrancas).
 */
@Component
public class NotificacaoDeCobrancaGerada extends NotificacaoTemplate<Cobranca> {

    private static final DateTimeFormatter DIA_MES_ANO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public NotificacaoDeCobrancaGerada(NotificacaoRepository repositorio, SeletorDeCanal seletorDeCanal) {
        super(repositorio, seletorDeCanal);
    }

    @Override
    protected TipoNotificacao tipo() {
        return TipoNotificacao.FINANCEIRO;
    }

    @Override
    protected String montarTitulo(Cobranca cobranca) {
        return "Novo boleto: " + cobranca.getTaxaDescricao() + " (" + cobranca.getCompetencia() + ")";
    }

    @Override
    protected String montarMensagem(Usuario destinatario, Cobranca cobranca) {
        return "Ola, %s! O boleto de %s vence em %s. Voce pode pagar com PIX pelo app."
                .formatted(destinatario.getNome(), emReais(cobranca.getValorOriginal()),
                        cobranca.getVencimento().format(DIA_MES_ANO));
    }

    @Override
    protected Long referenciaId(Cobranca cobranca) {
        return cobranca.getId();
    }

    /** Hook: boleto e o tipo de aviso que as pessoas costumam procurar no e-mail. */
    @Override
    protected CanalNotificacao escolherCanal(Cobranca cobranca) {
        return CanalNotificacao.EMAIL;
    }

    static String emReais(BigDecimal valor) {
        return "R$ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
