package org.morah.morah.financeiro.servico;

import java.time.Instant;

import org.morah.morah.comum.dto.PaginaResponse;
import org.morah.morah.comum.servico.ServicoCrudTemplate;
import org.morah.morah.financeiro.dto.DocumentoFinanceiroCreateRequest;
import org.morah.morah.financeiro.dto.DocumentoFinanceiroResponse;
import org.morah.morah.financeiro.modelo.DocumentoFinanceiro;
import org.morah.morah.financeiro.repositorio.DocumentoFinanceiroRepository;
import org.morah.morah.seguranca.jwt.ContextoDeSeguranca;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Documentos financeiros (notas fiscais, prestacao de contas...). Reaproveita o TEMPLATE METHOD
 * {@link ServicoCrudTemplate} na publicacao, igual ao {@code AvisoService}.
 *
 * <p>Os metodos {@code listar}/{@code buscarPorId} herdados NAO filtram por condominio, por isso
 * o controller usa {@link #listarDoCondominio}.
 */
@Service
@RequiredArgsConstructor
public class DocumentoFinanceiroService
        extends ServicoCrudTemplate<DocumentoFinanceiro, DocumentoFinanceiroCreateRequest, DocumentoFinanceiroResponse> {

    private final DocumentoFinanceiroRepository documentoFinanceiroRepository;

    // ---------- passos obrigatorios do template ----------

    @Override
    protected MongoRepository<DocumentoFinanceiro, Long> repositorio() {
        return documentoFinanceiroRepository;
    }

    @Override
    protected String nomeDoRecurso() {
        return "Documento financeiro";
    }

    @Override
    protected DocumentoFinanceiro converterParaEntidade(DocumentoFinanceiroCreateRequest requisicao) {
        DocumentoFinanceiro documento = new DocumentoFinanceiro();
        documento.setTipo(requisicao.tipo().trim());
        documento.setTitulo(requisicao.titulo().trim());
        documento.setArquivoUrl(requisicao.arquivoUrl().trim());
        documento.setCompetencia(requisicao.competencia());
        return documento;
    }

    @Override
    protected DocumentoFinanceiroResponse converterParaResposta(DocumentoFinanceiro documento) {
        return DocumentoFinanceiroResponse.de(documento);
    }

    // ---------- hooks ----------

    /** Condominio, autor e data de publicacao vem do token do sindico, nunca do corpo. */
    @Override
    protected void antesDeSalvar(DocumentoFinanceiro documento) {
        UsuarioAutenticado sindico = ContextoDeSeguranca.usuarioLogado();
        documento.setCondominioId(sindico.condominioId());
        documento.setPublicadoPorId(sindico.id());
        documento.setPublicadoEm(Instant.now());
    }

    // ---------- consulta filtrada ----------

    /** GET /financeiro/documentos: so os documentos do condominio do token. */
    public PaginaResponse<DocumentoFinanceiroResponse> listarDoCondominio(Long condominioId, Pageable paginacao) {
        return PaginaResponse.de(
                documentoFinanceiroRepository.findByCondominioId(condominioId, paginacao),
                DocumentoFinanceiroResponse::de);
    }
}
