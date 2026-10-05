package org.morah.morah.portaria.servico;

import java.util.Locale;

import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.portaria.dto.VisitanteInput;
import org.morah.morah.portaria.modelo.Visitante;
import org.morah.morah.portaria.repositorio.VisitanteRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Cadastro de visitantes do condominio. Nao tem endpoint proprio: e usado pelo registro de
 * visitante (autorizacoes) e pelo registro de acesso.
 */
@Service
@RequiredArgsConstructor
public class VisitanteService {

    private final VisitanteRepository visitanteRepository;

    /**
     * Cria o visitante ou reaproveita o cadastro existente.
     *
     * <p>Com documento: se ja existe alguem com o mesmo documento no condominio, o cadastro e
     * reaproveitado e o nome/telefone atualizados (o telefone so muda se vier preenchido, para nao
     * apagar um numero ja conhecido). Sem documento nao ha como reconhecer a pessoa com seguranca
     * (nomes se repetem), entao sempre cria um cadastro novo.
     */
    public Visitante registrar(Long condominioId, VisitanteInput dados) {
        String documento = normalizarDocumento(dados.documento());

        Visitante visitante = documento == null
                ? null
                : visitanteRepository.findFirstByCondominioIdAndDocumento(condominioId, documento).orElse(null);

        if (visitante == null) {
            visitante = new Visitante();
            visitante.setCondominioId(condominioId);
            visitante.setDocumento(documento);
        }

        visitante.setNome(dados.nome().trim());
        if (dados.telefone() != null && !dados.telefone().isBlank()) {
            visitante.setTelefone(dados.telefone().trim());
        }
        return visitanteRepository.save(visitante);
    }

    /** Visitante do condominio do token, ou 404. */
    public Visitante buscarDoCondominio(Long visitanteId, Long condominioId) {
        return visitanteRepository.findByIdAndCondominioId(visitanteId, condominioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Visitante", visitanteId));
    }

    /**
     * Deixa so letras e numeros, em maiusculo: "12.345.678-x" e "12345678X" sao o mesmo documento.
     * Devolve {@code null} quando nao sobra nada.
     */
    static String normalizarDocumento(String documento) {
        if (documento == null) {
            return null;
        }
        String limpo = documento.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
        return limpo.isEmpty() ? null : limpo;
    }
}
