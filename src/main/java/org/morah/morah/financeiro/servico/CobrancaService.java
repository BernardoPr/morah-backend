package org.morah.morah.financeiro.servico;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.dto.BaixaManualRequest;
import org.morah.morah.financeiro.dto.CobrancaResponse;
import org.morah.morah.financeiro.dto.NovoPagamento;
import org.morah.morah.financeiro.dto.SituacaoDaCobranca;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.notificacao.template.NotificacaoDePagamentoConfirmado;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Regras das cobrancas (boletos): consulta, baixa manual e o registro de pagamento usado tambem
 * pelo webhook do gateway.
 *
 * <p><b>Quem ve o que:</b> morador e proprietario so enxergam a unidade do proprio token; o
 * sindico enxerga o condominio inteiro. Outro condominio = 404 (nem confirmamos que existe);
 * outra unidade do mesmo condominio = 403.
 */
@Service
@RequiredArgsConstructor
public class CobrancaService {

    private final CobrancaRepository cobrancaRepository;
    private final MongoTemplate mongoTemplate;
    private final UsuarioRepository usuarioRepository;
    private final CalculadoraDeEncargos calculadoraDeEncargos;
    private final NotificacaoDePagamentoConfirmado notificacaoDePagamentoConfirmado;

    // ---------- consultas ----------

    /**
     * GET /financeiro/cobrancas. Os filtros sao todos opcionais e combinaveis, por isso a consulta
     * e montada peca por peca com {@link Criteria} (com metodos de repositorio seriam 2^3 metodos).
     *
     * @param unidadeId so vale para o sindico; para morador/proprietario e ignorado e trocado
     *                  pela unidade do token (ninguem lista boleto de outra unidade mudando a URL)
     */
    public PaginaResponse<CobrancaResponse> listar(UsuarioAutenticado usuario, StatusCobranca status,
                                                   String competencia, Long unidadeId, Pageable paginacao) {
        LocalDate hoje = Datas.hoje();

        Criteria filtro = Criteria.where("condominioId").is(usuario.condominioId());

        if (!ehSindico(usuario)) {
            // Sempre filtra, mesmo se o token vier sem unidade: nesse caso nao aparece nada
            // (em vez de, por descuido, aparecer o condominio inteiro).
            filtro = filtro.and("unidadeId").is(usuario.unidadeId());
        } else if (unidadeId != null) {
            filtro = filtro.and("unidadeId").is(unidadeId);
        }
        if (competencia != null && !competencia.isBlank()) {
            filtro = filtro.and("competencia").is(competencia);
        }
        if (status != null) {
            filtro = filtrarPorStatus(filtro, status, hoje);
        }

        Query consulta = new Query(filtro);
        long total = mongoTemplate.count(consulta, Cobranca.class);
        List<Cobranca> pagina = mongoTemplate.find(Query.of(consulta).with(paginacao), Cobranca.class);

        return PaginaResponse.de(new PageImpl<>(pagina, paginacao, total), cobranca -> paraResposta(cobranca, hoje));
    }

    /** GET /financeiro/cobrancas/{cobrancaId}. */
    public CobrancaResponse detalhar(UsuarioAutenticado usuario, Long cobrancaId) {
        return paraResposta(buscarVisivelPara(usuario, cobrancaId), Datas.hoje());
    }

    /**
     * Metodo publico para o DASHBOARD do morador ("proximos boletos"): cobrancas ainda nao pagas
     * da unidade (inclui as atrasadas), da que vence primeiro para a ultima.
     * Recebe tudo por parametro - nao le o token.
     */
    public List<CobrancaResponse> proximasDaUnidade(Long unidadeId, int limite) {
        if (unidadeId == null || limite <= 0) {
            return List.of();
        }
        LocalDate hoje = Datas.hoje();
        return cobrancaRepository
                .findByUnidadeIdAndStatusOrderByVencimentoAsc(unidadeId, StatusCobranca.PENDENTE, PageRequest.of(0, limite))
                .stream()
                .map(cobranca -> paraResposta(cobranca, hoje))
                .toList();
    }

    // ---------- pagamento ----------

    /** POST /financeiro/cobrancas/{cobrancaId}/baixa-manual (somente sindico). */
    public CobrancaResponse baixarManualmente(UsuarioAutenticado sindico, Long cobrancaId,
                                              BaixaManualRequest requisicao) {
        Cobranca cobranca = cobrancaRepository.findByIdAndCondominioId(cobrancaId, sindico.condominioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobranca", cobrancaId));

        exigirEmAberto(cobranca);

        Cobranca paga = registrarPagamento(cobranca, new NovoPagamento(
                requisicao.pagoEm(),
                requisicao.valor(),
                requisicao.meio().trim().toLowerCase(Locale.ROOT),
                requisicao.observacao(),
                null,
                sindico.id()));

        return paraResposta(paga, Datas.hoje());
    }

    /**
     * Marca a cobranca como PAGA e avisa a unidade. Unico lugar que "quita" uma cobranca: usado
     * pela baixa manual e pelo webhook do gateway.
     *
     * <p>A multa e os juros devidos NO DIA DO PAGAMENTO sao gravados ("congelados"): assim o
     * historico da cobranca paga nao muda se os percentuais do condominio mudarem depois.
     */
    public Cobranca registrarPagamento(Cobranca cobranca, NovoPagamento pagamento) {
        SituacaoDaCobranca noDiaDoPagamento =
                calculadoraDeEncargos.emAberto(cobranca, Datas.dataDe(pagamento.pagoEm()));

        cobranca.setStatus(StatusCobranca.PAGO);
        cobranca.setPagoEm(pagamento.pagoEm());
        cobranca.setValorPago(pagamento.valor() != null
                ? pagamento.valor().setScale(2, RoundingMode.HALF_UP)
                : noDiaDoPagamento.valorTotal());
        cobranca.setMultaPaga(noDiaDoPagamento.multa());
        cobranca.setJurosPagos(noDiaDoPagamento.juros());
        cobranca.setMeio(pagamento.meio());
        cobranca.setObservacao(pagamento.observacao());
        cobranca.setTransacaoId(pagamento.transacaoId());
        cobranca.setBaixadoPorId(pagamento.baixadoPorId());
        // Depois de paga, o PIX antigo nao deve mais ser oferecido.
        cobranca.setCodigoPix(null);
        cobranca.setPixExpiraEm(null);

        Cobranca paga = cobrancaRepository.save(cobranca);

        usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(paga.getUnidadeId())
                .forEach(usuario -> notificacaoDePagamentoConfirmado.enviar(usuario, paga));
        return paga;
    }

    // ---------- apoio (usado tambem pelo PixService) ----------

    /** Busca respeitando "quem ve o que": 404 se for de outro condominio, 403 se for de outra unidade. */
    public Cobranca buscarVisivelPara(UsuarioAutenticado usuario, Long cobrancaId) {
        Cobranca cobranca = cobrancaRepository.findByIdAndCondominioId(cobrancaId, usuario.condominioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cobranca", cobrancaId));

        if (!ehSindico(usuario) && !Objects.equals(cobranca.getUnidadeId(), usuario.unidadeId())) {
            throw new AcessoNegadoException("Essa cobranca e de outra unidade.");
        }
        return cobranca;
    }

    /** 409 se a cobranca ja foi paga ou cancelada (nao da para pagar de novo nem gerar PIX). */
    public static void exigirEmAberto(Cobranca cobranca) {
        if (cobranca.getStatus() == StatusCobranca.PAGO) {
            throw new RegraDeNegocioException("A cobranca " + cobranca.getId() + " ja esta paga.");
        }
        if (cobranca.getStatus() == StatusCobranca.CANCELADO) {
            throw new RegraDeNegocioException("A cobranca " + cobranca.getId() + " foi cancelada.");
        }
    }

    public CobrancaResponse paraResposta(Cobranca cobranca, LocalDate hoje) {
        return CobrancaResponse.de(cobranca, calculadoraDeEncargos.situacao(cobranca, hoje));
    }

    private static boolean ehSindico(UsuarioAutenticado usuario) {
        return usuario.perfil() == Perfil.SINDICO;
    }

    /**
     * Traduz o status do contrato para a consulta. "pendente" e "atrasado" sao gravados do mesmo
     * jeito (PENDENTE); o que separa os dois e o vencimento comparado com hoje.
     */
    private static Criteria filtrarPorStatus(Criteria filtro, StatusCobranca status, LocalDate hoje) {
        return switch (status) {
            case PENDENTE -> filtro.and("status").is(StatusCobranca.PENDENTE).and("vencimento").gte(hoje);
            case ATRASADO -> filtro.and("status").is(StatusCobranca.PENDENTE).and("vencimento").lt(hoje);
            case PAGO, CANCELADO -> filtro.and("status").is(status);
        };
    }
}
