package org.morah.morah.ocorrencia.modelo;

/** De onde a ocorrencia veio (o {@code referenciaId} aponta para o registro dessa origem). */
public enum OrigemOcorrencia {
    /** Extravio de encomenda: POST /encomendas/{id}/ocorrencias. */
    ENCOMENDA,
    /** Dano encontrado na vistoria: POST /reservas/{id}/vistoria. */
    RESERVA
}
