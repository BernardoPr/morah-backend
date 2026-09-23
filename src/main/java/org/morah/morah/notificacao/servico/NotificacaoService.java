package org.morah.morah.notificacao.servico;

import java.time.Instant;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.notificacao.dto.NotificacaoResponse;
import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** Consulta das notificacoes geradas pelo {@code NotificacaoTemplate}. */
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    public PaginaResponse<NotificacaoResponse> listarDoUsuario(UsuarioAutenticado usuario, Pageable paginacao) {
        var pagina = notificacaoRepository.findByDestinatarioIdOrderByEnviadaEmDesc(usuario.id(), paginacao);
        return PaginaResponse.de(pagina, NotificacaoResponse::de);
    }

    public NotificacaoResponse marcarComoLida(UsuarioAutenticado usuario, Long notificacaoId) {
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificacao", notificacaoId));

        if (!notificacao.getDestinatarioId().equals(usuario.id())) {
            throw new AcessoNegadoException("Essa notificacao e de outro usuario.");
        }

        if (notificacao.getLidaEm() == null) {
            notificacao.setLidaEm(Instant.now());
            notificacao = notificacaoRepository.save(notificacao);
        }
        return NotificacaoResponse.de(notificacao);
    }
}
