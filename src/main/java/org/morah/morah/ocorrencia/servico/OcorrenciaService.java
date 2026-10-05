package org.morah.morah.ocorrencia.servico;

import java.time.Instant;
import java.util.List;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.notificacao.template.NotificacaoDeOcorrenciaAberta;
import org.morah.morah.ocorrencia.dto.NovaOcorrencia;
import org.morah.morah.ocorrencia.dto.OcorrenciaResponse;
import org.morah.morah.ocorrencia.modelo.Ocorrencia;
import org.morah.morah.ocorrencia.modelo.StatusOcorrencia;
import org.morah.morah.ocorrencia.repositorio.OcorrenciaRepository;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Ocorrencias do condominio. Nao tem controller proprio: os modulos de Encomendas e Reservas
 * abrem ocorrencias por aqui, e o dashboard do sindico conta as abertas.
 */
@Service
@RequiredArgsConstructor
public class OcorrenciaService {

    private static final List<StatusOcorrencia> NAO_ENCERRADAS =
            List.of(StatusOcorrencia.ABERTA, StatusOcorrencia.EM_ANDAMENTO);

    private final OcorrenciaRepository ocorrenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoDeOcorrenciaAberta notificacaoDeOcorrenciaAberta;

    /** Grava a ocorrencia como "aberta" e avisa os sindicos do condominio. */
    public OcorrenciaResponse abrir(NovaOcorrencia dados) {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setCondominioId(dados.condominioId());
        ocorrencia.setUnidadeId(dados.unidadeId());
        ocorrencia.setOrigem(dados.origem());
        ocorrencia.setReferenciaId(dados.referenciaId());
        ocorrencia.setTitulo(dados.titulo());
        ocorrencia.setDescricao(dados.descricao());
        ocorrencia.setStatus(StatusOcorrencia.ABERTA);
        ocorrencia.setAbertaPorId(dados.abertaPorId());
        ocorrencia.setAbertaPorNome(dados.abertaPorNome());
        ocorrencia.setAbertaEm(Instant.now());

        Ocorrencia salva = ocorrenciaRepository.save(ocorrencia);

        usuarioRepository.listarPorPerfilNoCondominio(dados.condominioId(), Perfil.SINDICO)
                .forEach(sindico -> notificacaoDeOcorrenciaAberta.enviar(sindico, salva));

        return OcorrenciaResponse.de(salva);
    }

    /** Ocorrencias ainda nao encerradas (campo ocorrenciasAbertas do dashboard do sindico). */
    public long contarAbertas(Long condominioId) {
        return ocorrenciaRepository.countByCondominioIdAndStatusIn(condominioId, NAO_ENCERRADAS);
    }
}
