package org.morah.morah.encomenda.strategy;

import java.time.Instant;
import java.util.Optional;

import org.morah.morah.encomenda.modelo.AutorizacaoRetirada;
import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.encomenda.modelo.StatusAutorizacaoRetirada;
import org.morah.morah.encomenda.repositorio.AutorizacaoRetiradaRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Retirada por um terceiro com o codigo de uma {@link AutorizacaoRetirada} criada pelo morador.
 *
 * <p>Regras proprias desta forma (e o motivo de ela ser uma classe separada):
 * <ul>
 *   <li>a autorizacao precisa estar ATIVA e dentro da validade;</li>
 *   <li>o codigo e de uso unico: depois da retirada a autorizacao vira UTILIZADA
 *       (hook {@link #registrarUso});</li>
 *   <li>o nome e o documento do terceiro ja estao na autorizacao, entao o porteiro nao precisa
 *       digita-los de novo.</li>
 * </ul>
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class ValidacaoAutorizacaoDeTerceiroStrategy implements ValidacaoDeRetiradaStrategy {

    private final AutorizacaoRetiradaRepository autorizacaoRetiradaRepository;

    @Override
    public String forma() {
        return "autorizacao de terceiro";
    }

    @Override
    public Optional<RetiradaValidada> validar(Encomenda encomenda, String codigo, Instant agora) {
        return autorizacaoRetiradaRepository
                .findByEncomendaIdAndStatus(encomenda.getId(), StatusAutorizacaoRetirada.ATIVA)
                .stream()
                .filter(autorizacao -> autorizacao.getCodigo().equals(codigo))
                .filter(autorizacao -> autorizacao.valeEm(agora))
                .findFirst()
                .map(autorizacao -> new RetiradaValidada(
                        forma(), autorizacao.getId(), autorizacao.getNomeTerceiro(), autorizacao.getDocumento()));
    }

    /** Codigo de uso unico: a autorizacao usada nao vale mais. */
    @Override
    public void registrarUso(Encomenda encomenda, RetiradaValidada retirada, Instant agora) {
        autorizacaoRetiradaRepository.findById(retirada.autorizacaoId()).ifPresent(autorizacao -> {
            autorizacao.setStatus(StatusAutorizacaoRetirada.UTILIZADA);
            autorizacao.setUtilizadaEm(agora);
            autorizacaoRetiradaRepository.save(autorizacao);
        });
    }
}
