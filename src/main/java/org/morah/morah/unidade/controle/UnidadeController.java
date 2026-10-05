package org.morah.morah.unidade.controle;

import java.util.List;

import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.dto.DecisaoSolicitacaoVinculoRequest;
import org.morah.morah.unidade.dto.InquilinoCreateRequest;
import org.morah.morah.unidade.dto.SolicitacaoVinculoCreateRequest;
import org.morah.morah.unidade.dto.SolicitacaoVinculoResponse;
import org.morah.morah.unidade.dto.UnidadeResponse;
import org.morah.morah.unidade.dto.VinculoUnidadeResponse;
import org.morah.morah.unidade.servico.SolicitacaoVinculoService;
import org.morah.morah.unidade.servico.UnidadeService;
import org.morah.morah.unidade.servico.VinculoUnidadeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints da secao "Minha Unidade" do contrato.
 *
 * <p>As rotas {@code /minha/...} usam a unidade do token (morador/proprietario); as rotas
 * {@code /{unidadeId}/...} sao administrativas (sindico) e sempre filtram pelo condominio do
 * token. O Spring prefere o caminho literal {@code /minha} ao {@code /{unidadeId}}, entao as
 * duas convivem sem conflito.
 */
@Tag(name = "Minha Unidade", description = "Dados da unidade, vinculos e gestao de inquilinos")
@RestController
@RequestMapping("/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService unidadeService;
    private final VinculoUnidadeService vinculoUnidadeService;
    private final SolicitacaoVinculoService solicitacaoVinculoService;

    // ---------- morador / proprietario ----------

    @Operation(summary = "Consultar os dados da unidade vinculada ao contexto ativo")
    @GetMapping("/minha")
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public UnidadeResponse minhaUnidade(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return unidadeService.buscarMinha(usuario);
    }

    @Operation(summary = "Listar os vinculos (moradores e dependentes) da unidade ativa")
    @GetMapping("/minha/vinculos")
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public List<VinculoUnidadeResponse> vinculosDaMinhaUnidade(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return vinculoUnidadeService.listarDaMinhaUnidade(usuario);
    }

    @Operation(summary = "Solicitar a inclusao de um novo vinculo (ex. dependente) a unidade")
    @PostMapping("/minha/vinculos/solicitacoes")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('MORADOR','PROPRIETARIO')")
    public SolicitacaoVinculoResponse solicitarVinculo(@Valid @RequestBody SolicitacaoVinculoCreateRequest requisicao) {
        return solicitacaoVinculoService.criar(requisicao);
    }

    // ---------- somente proprietario ----------

    @Operation(summary = "Cadastrar um inquilino e vincular o contrato de locacao a unidade (somente proprietario)")
    @PostMapping("/minha/inquilinos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROPRIETARIO')")
    public VinculoUnidadeResponse cadastrarInquilino(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                     @Valid @RequestBody InquilinoCreateRequest requisicao) {
        return vinculoUnidadeService.cadastrarInquilino(usuario, requisicao);
    }

    @Operation(summary = "Encerrar antecipadamente o contrato de um inquilino (somente proprietario)")
    @DeleteMapping("/minha/inquilinos/{vinculoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PROPRIETARIO')")
    public void encerrarInquilino(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                  @PathVariable Long vinculoId) {
        vinculoUnidadeService.encerrarInquilino(usuario, vinculoId);
    }

    // ---------- sindico ----------

    @Operation(summary = "Consultar os dados de qualquer unidade do condominio (somente sindico)")
    @GetMapping("/{unidadeId}")
    @PreAuthorize("hasRole('SINDICO')")
    public UnidadeResponse detalhar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                    @PathVariable Long unidadeId) {
        return unidadeService.buscarDoCondominio(usuario, unidadeId);
    }

    @Operation(summary = "Listar solicitacoes de vinculo pendentes de aprovacao da unidade (somente sindico)")
    @GetMapping("/{unidadeId}/vinculos/solicitacoes")
    @PreAuthorize("hasRole('SINDICO')")
    public List<SolicitacaoVinculoResponse> solicitacoesPendentes(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                                  @PathVariable Long unidadeId) {
        return solicitacaoVinculoService.listarPendentes(usuario, unidadeId);
    }

    @Operation(summary = "Aprovar ou rejeitar uma solicitacao de vinculo (somente sindico)")
    @PatchMapping("/{unidadeId}/vinculos/solicitacoes/{solicitacaoId}")
    @PreAuthorize("hasRole('SINDICO')")
    public SolicitacaoVinculoResponse decidirSolicitacao(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                         @PathVariable Long unidadeId,
                                                         @PathVariable Long solicitacaoId,
                                                         @Valid @RequestBody DecisaoSolicitacaoVinculoRequest requisicao) {
        return solicitacaoVinculoService.decidir(usuario, unidadeId, solicitacaoId, requisicao);
    }
}
