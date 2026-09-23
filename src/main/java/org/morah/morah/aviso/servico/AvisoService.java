package org.morah.morah.aviso.servico;

import java.time.Instant;

import org.morah.morah.aviso.dto.AvisoCreateRequest;
import org.morah.morah.aviso.dto.AvisoResponse;
import org.morah.morah.aviso.dto.AvisoUpdateRequest;
import org.morah.morah.aviso.modelo.Aviso;
import org.morah.morah.aviso.modelo.PrioridadeAviso;
import org.morah.morah.aviso.repositorio.AvisoRepository;
import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.servico.ServicoCrudTemplate;
import org.morah.morah.notificacao.template.NotificacaoDeAvisoPublicado;
import org.morah.morah.seguranca.jwt.ContextoDeSeguranca;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Exemplo de service que reaproveita o TEMPLATE METHOD {@link ServicoCrudTemplate}.
 *
 * <p>Repare no tamanho da classe: os metodos {@code criar}, {@code listar},
 * {@code buscarPorId} e {@code remover} vem prontos da classe pai. Aqui so escrevemos
 * o que e especifico do aviso.
 */
@Service
@RequiredArgsConstructor
public class AvisoService extends ServicoCrudTemplate<Aviso, AvisoCreateRequest, AvisoResponse> {

    private final AvisoRepository avisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoDeAvisoPublicado notificacaoDeAvisoPublicado;

    // ---------- passos obrigatorios do template ----------

    @Override
    protected MongoRepository<Aviso, Long> repositorio() {
        return avisoRepository;
    }

    @Override
    protected String nomeDoRecurso() {
        return "Aviso";
    }

    @Override
    protected Aviso converterParaEntidade(AvisoCreateRequest requisicao) {
        Aviso aviso = new Aviso();
        aviso.setTitulo(requisicao.titulo());
        aviso.setConteudo(requisicao.conteudo());
        aviso.setPrioridade(requisicao.prioridade() == null ? PrioridadeAviso.NORMAL : requisicao.prioridade());
        aviso.setPublicoAlvo(requisicao.publicoAlvo());
        aviso.setExpiraEm(requisicao.expiraEm());
        return aviso;
    }

    @Override
    protected AvisoResponse converterParaResposta(Aviso aviso) {
        return AvisoResponse.de(aviso);
    }

    // ---------- hooks ----------

    /** Preenche autor, condominio e data de publicacao a partir do token do sindico. */
    @Override
    protected void antesDeSalvar(Aviso aviso) {
        UsuarioAutenticado logado = ContextoDeSeguranca.usuarioLogado();
        aviso.setAutorId(logado.id());
        aviso.setAutorNome(logado.nome());
        aviso.setCondominioId(logado.condominioId());
        aviso.setPublicadoEm(Instant.now());
    }

    /** Depois de publicar, dispara a notificacao (Template Method + Strategy trabalhando juntos). */
    @Override
    protected void depoisDeSalvar(Aviso aviso) {
        // Base do projeto: notificamos o proprio autor como confirmacao.
        // Quando as regras forem implementadas, troque por uma consulta aos moradores
        // do publico alvo e chame o mesmo metodo dentro do laco.
        usuarioRepository.findById(aviso.getAutorId())
                .ifPresent(autor -> notificacaoDeAvisoPublicado.enviar(autor, aviso));
    }

    // ---------- operacoes especificas do aviso ----------

    /** Lista apenas os avisos do condominio ativo (GET /avisos). */
    public PaginaResponse<AvisoResponse> listarDoCondominioAtivo(PrioridadeAviso prioridade, Pageable paginacao) {
        Long condominioId = ContextoDeSeguranca.usuarioLogado().condominioId();

        var pagina = prioridade == null
                ? avisoRepository.findByCondominioId(condominioId, paginacao)
                : avisoRepository.findByCondominioIdAndPrioridade(condominioId, prioridade, paginacao);

        return PaginaResponse.de(pagina, AvisoResponse::de);
    }

    /** PATCH /avisos/{id}: altera somente os campos enviados. */
    public AvisoResponse atualizar(Long id, AvisoUpdateRequest requisicao) {
        Aviso aviso = buscarEntidade(id);

        if (requisicao.titulo() != null) {
            aviso.setTitulo(requisicao.titulo());
        }
        if (requisicao.conteudo() != null) {
            aviso.setConteudo(requisicao.conteudo());
        }
        if (requisicao.prioridade() != null) {
            aviso.setPrioridade(requisicao.prioridade());
        }
        if (requisicao.expiraEm() != null) {
            aviso.setExpiraEm(requisicao.expiraEm());
        }

        return AvisoResponse.de(avisoRepository.save(aviso));
    }
}
