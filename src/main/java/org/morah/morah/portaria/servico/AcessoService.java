package org.morah.morah.portaria.servico;

import java.time.Instant;
import java.time.LocalDate;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.portaria.dto.AcessoCreateRequest;
import org.morah.morah.portaria.dto.AcessoResponse;
import org.morah.morah.portaria.modelo.Acesso;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.DadosDoVisitante;
import org.morah.morah.portaria.modelo.StatusAcesso;
import org.morah.morah.portaria.modelo.TipoAcesso;
import org.morah.morah.portaria.modelo.Visitante;
import org.morah.morah.portaria.repositorio.AcessoRepository;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Livro de entradas e saidas da portaria (GET e POST /portaria/acessos).
 *
 * <p>Um documento {@link Acesso} por visita: a entrada cria, a saida completa o mesmo documento.
 */
@Service
@RequiredArgsConstructor
public class AcessoService {

    private final AcessoRepository acessoRepository;
    private final VisitanteService visitanteService;
    private final AutorizacaoVisitaService autorizacaoVisitaService;

    /** Historico do condominio; com {@code data}, so as entradas daquele dia (fuso do condominio). */
    public PaginaResponse<AcessoResponse> listar(UsuarioAutenticado usuario, LocalDate data, Pageable paginacao) {
        Long condominioId = usuario.condominioId();
        Page<Acesso> pagina = data == null
                ? acessoRepository.findByCondominioId(condominioId, paginacao)
                : acessoRepository.buscarEntradasEntre(condominioId, Datas.inicioDoDia(data), Datas.fimDoDia(data),
                        paginacao);
        return PaginaResponse.de(pagina, AcessoResponse::de);
    }

    /** Registra a entrada ou a saida de um visitante do condominio (visitante de outro = 404). */
    public AcessoResponse registrar(UsuarioAutenticado porteiro, AcessoCreateRequest requisicao) {
        Visitante visitante = visitanteService.buscarDoCondominio(requisicao.visitanteId(), porteiro.condominioId());
        Instant agora = Instant.now();

        Acesso acesso = switch (requisicao.tipo()) {
            case ENTRADA -> registrarEntrada(porteiro, visitante, requisicao, agora);
            case SAIDA -> registrarSaida(porteiro, visitante, requisicao, agora);
        };
        return AcessoResponse.de(acesso);
    }

    /**
     * Entrada: o visitante nao pode estar dentro e precisa de uma autorizacao AUTORIZADA (quem
     * confere isso e o estado da autorizacao, via {@link AutorizacaoVisitaService#liberarEntrada}).
     */
    private Acesso registrarEntrada(UsuarioAutenticado porteiro, Visitante visitante,
                                    AcessoCreateRequest requisicao, Instant agora) {
        Long condominioId = porteiro.condominioId();
        if (acessoRepository.existsByCondominioIdAndVisitanteIdAndSaidaIsNull(condominioId, visitante.getId())) {
            throw new RegraDeNegocioException(
                    "Este visitante ja esta dentro do condominio. Registre a saida antes de uma nova entrada.");
        }

        AutorizacaoVisita autorizacao = autorizacaoVisitaService.liberarEntrada(
                condominioId, visitante.getId(), requisicao.autorizacaoId(), agora);

        Acesso acesso = new Acesso();
        acesso.setCondominioId(condominioId);
        acesso.setUnidadeId(autorizacao.getUnidadeId());
        acesso.setAutorizacaoId(autorizacao.getId());
        acesso.setVisitanteId(visitante.getId());
        acesso.setVisitante(DadosDoVisitante.de(visitante));
        acesso.setEntrada(agora);
        acesso.setTipo(TipoAcesso.ENTRADA);
        acesso.setStatus(StatusAcesso.EM_ANDAMENTO);
        acesso.setFotoEntradaUrl(requisicao.fotoUrl());
        acesso.setEntradaRegistradaPorId(porteiro.id());
        acesso.setEntradaRegistradaPorNome(porteiro.nome());
        return acessoRepository.save(acesso);
    }

    /** Saida: completa o acesso em aberto do visitante (sem entrada registrada = 409). */
    private Acesso registrarSaida(UsuarioAutenticado porteiro, Visitante visitante,
                                  AcessoCreateRequest requisicao, Instant agora) {
        Acesso acesso = acessoRepository
                .findFirstByCondominioIdAndVisitanteIdAndSaidaIsNullOrderByEntradaDesc(
                        porteiro.condominioId(), visitante.getId())
                .orElseThrow(() -> new RegraDeNegocioException(
                        "Nao ha entrada em aberto para este visitante: registre a entrada antes da saida."));

        acesso.setSaida(agora);
        acesso.setTipo(TipoAcesso.SAIDA);
        acesso.setStatus(StatusAcesso.ENCERRADO);
        acesso.setFotoSaidaUrl(requisicao.fotoUrl());
        acesso.setSaidaRegistradaPorId(porteiro.id());
        acesso.setSaidaRegistradaPorNome(porteiro.nome());
        return acessoRepository.save(acesso);
    }
}
