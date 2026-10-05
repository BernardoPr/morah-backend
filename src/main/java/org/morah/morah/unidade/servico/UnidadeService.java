package org.morah.morah.unidade.servico;

import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.dto.UnidadeResponse;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Consulta das unidades. Tambem e o ponto unico onde os outros services do modulo buscam
 * "a unidade do token" ou "uma unidade do condominio", sempre filtrando pelo condominio ativo.
 */
@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;

    /** GET /unidades/minha: a unidade do contexto ativo (morador/proprietario). */
    public UnidadeResponse buscarMinha(UsuarioAutenticado logado) {
        return UnidadeResponse.de(buscarMinhaEntidade(logado));
    }

    /** GET /unidades/{unidadeId}: qualquer unidade do condominio do sindico. */
    public UnidadeResponse buscarDoCondominio(UsuarioAutenticado sindico, Long unidadeId) {
        return UnidadeResponse.de(buscarEntidadeDoCondominio(unidadeId, sindico.condominioId()));
    }

    /**
     * Usado pelo dashboard do morador (campo "unidade"). Recebe tudo por parametro e devolve
     * {@code null} em vez de lancar 404: a tela inicial nao pode quebrar porque a unidade sumiu.
     */
    public UnidadeResponse buscarResposta(Long unidadeId) {
        if (unidadeId == null) {
            return null;
        }
        return unidadeRepository.findById(unidadeId)
                .map(UnidadeResponse::de)
                .orElse(null);
    }

    // ---------- apoio para os outros services do modulo ----------

    /** Unidade do token; 404 se o contexto nao tem unidade ou se ela nao e do condominio ativo. */
    public Unidade buscarMinhaEntidade(UsuarioAutenticado logado) {
        if (logado.unidadeId() == null) {
            throw new RecursoNaoEncontradoException("O contexto ativo nao tem uma unidade vinculada.");
        }
        return buscarEntidadeDoCondominio(logado.unidadeId(), logado.condominioId());
    }

    /** Unidade de outro condominio responde 404, como se nao existisse. */
    public Unidade buscarEntidadeDoCondominio(Long unidadeId, Long condominioId) {
        return unidadeRepository.findByIdAndCondominioId(unidadeId, condominioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", unidadeId));
    }
}
