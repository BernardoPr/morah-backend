package org.morah.morah.encomenda.dto;

import java.time.Instant;

import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.encomenda.modelo.StatusEncomenda;

/** Resposta do contrato (schema Encomenda). */
public record EncomendaResponse(
        Long id,
        Long unidadeId,
        String remetente,
        String transportadora,
        String codigoRastreio,
        String codigoRetirada,
        String fotoUrl,
        Instant recebidaEm,
        Instant retiradaEm,
        StatusEncomenda status) {

    /**
     * Monta a resposta decidindo se o codigo de retirada vai junto.
     *
     * <p>Nao existe a versao "so com a encomenda" de proposito: quem monta a resposta e obrigado
     * a pensar se o leitor pode ver o codigo. A regra e: so o morador/proprietario da propria
     * unidade ve o codigo. Para portaria e sindico ele vai {@code null} - se o porteiro visse o
     * codigo, pedir o codigo no balcao nao provaria que quem esta retirando e da unidade.
     *
     * @param exibirCodigo {@code true} apenas para morador/proprietario da unidade da encomenda
     */
    public static EncomendaResponse de(Encomenda encomenda, boolean exibirCodigo) {
        return new EncomendaResponse(
                encomenda.getId(),
                encomenda.getUnidadeId(),
                encomenda.getRemetente(),
                encomenda.getTransportadora(),
                encomenda.getCodigoRastreio(),
                exibirCodigo ? encomenda.getCodigoRetirada() : null,
                encomenda.getFotoUrl(),
                encomenda.getRecebidaEm(),
                encomenda.getRetiradaEm(),
                encomenda.getStatus());
    }
}
