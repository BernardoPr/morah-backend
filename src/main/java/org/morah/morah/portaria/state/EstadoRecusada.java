package org.morah.morah.portaria.state;

import java.time.Instant;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.springframework.stereotype.Component;

/** O morador negou a entrada. Estado final: nenhuma operacao e aceita. */
@Component
public class EstadoRecusada implements EstadoDaAutorizacao {

    @Override
    public StatusAutorizacaoVisita status() {
        return StatusAutorizacaoVisita.RECUSADA;
    }

    @Override
    public void registrarEntrada(AutorizacaoVisita autorizacao, Instant agora) {
        throw new RegraDeNegocioException("A unidade recusou a entrada deste visitante.");
    }
}
