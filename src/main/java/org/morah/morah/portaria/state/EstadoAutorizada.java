package org.morah.morah.portaria.state;

import java.time.Duration;
import java.time.Instant;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.springframework.stereotype.Component;

/**
 * O morador liberou a entrada. Estado final para decisao e reenvio (herda o 409 padrao); a unica
 * operacao aceita e registrar a entrada do visitante.
 *
 * <p>Regras da entrada (o contrato e omisso; escolhidas por seguranca da portaria):
 * <ul>
 *   <li>cada autorizacao vale para <b>uma</b> entrada - voltou outro dia, pede de novo;</li>
 *   <li>a entrada precisa acontecer em ate {@link #PRAZO_PARA_ENTRADA} depois da resposta - uma
 *       autorizacao esquecida de semanas atras nao abre o portao.</li>
 * </ul>
 */
@Component
public class EstadoAutorizada implements EstadoDaAutorizacao {

    public static final Duration PRAZO_PARA_ENTRADA = Duration.ofHours(12);

    @Override
    public StatusAutorizacaoVisita status() {
        return StatusAutorizacaoVisita.AUTORIZADA;
    }

    @Override
    public void registrarEntrada(AutorizacaoVisita autorizacao, Instant agora) {
        if (autorizacao.getEntradaEm() != null) {
            throw new RegraDeNegocioException(
                    "Esta autorizacao ja foi usada em uma entrada. Para uma nova visita, registre o visitante novamente.");
        }

        Instant liberadaEm = autorizacao.getRespondidoEm() != null
                ? autorizacao.getRespondidoEm()
                : autorizacao.getSolicitadoEm();
        if (liberadaEm != null && agora.isAfter(liberadaEm.plus(PRAZO_PARA_ENTRADA))) {
            throw new RegraDeNegocioException(
                    "Esta autorizacao venceu: ela vale por " + PRAZO_PARA_ENTRADA.toHours()
                            + " horas apos a resposta da unidade. Registre o visitante novamente.");
        }

        // A autorizacao continua "autorizada" (o contrato nao tem um status "utilizada");
        // a marca de uso fica no campo entradaEm.
        autorizacao.setEntradaEm(agora);
    }
}
