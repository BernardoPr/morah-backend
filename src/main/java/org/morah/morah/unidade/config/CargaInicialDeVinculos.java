package org.morah.morah.unidade.config;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.StatusSolicitacaoVinculo;
import org.morah.morah.unidade.modelo.TipoVinculo;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.SolicitacaoVinculoRepository;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dados de demonstracao do modulo Minha Unidade.
 *
 * <p>Roda depois da carga principal ({@code @Order(2)}), porque usa as unidades e os usuarios
 * criados por ela. Os usuarios sao buscados pelo CPF (os ids nao sao fixos). Idempotente: cada
 * colecao so e preenchida se estiver vazia.
 *
 * <ul>
 *   <li>Apto 101: Bruno (proprietario principal desde 2020) e Ana (inquilina, contrato de
 *       01/03/2025 a 28/02/2027);</li>
 *   <li>Apto 202: Carlos (proprietario principal desde 2019, mora la e e o sindico);</li>
 *   <li>uma solicitacao PENDENTE de dependente no Apto 202, para o contador do dashboard do
 *       sindico nao ficar zerado (e para testar o PATCH de aprovacao).</li>
 * </ul>
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "morah.carga-inicial", havingValue = "true")
public class CargaInicialDeVinculos implements CommandLineRunner {

    private static final Long CONDOMINIO = 1L;

    private final VinculoUnidadeRepository vinculoUnidadeRepository;
    private final SolicitacaoVinculoRepository solicitacaoVinculoRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        criarVinculos();
        criarSolicitacoes();
    }

    private void criarVinculos() {
        if (vinculoUnidadeRepository.count() > 0) {
            return; // ja existem vinculos, nao faz nada
        }

        List<VinculoUnidade> vinculos = new ArrayList<>();
        vinculo(101L, "44444444444", TipoVinculo.PROPRIETARIO, true, LocalDate.of(2020, 1, 1), null)
                .ifPresent(vinculos::add);
        vinculo(101L, "11111111111", TipoVinculo.INQUILINO, false, LocalDate.of(2025, 3, 1), LocalDate.of(2027, 2, 28))
                .ifPresent(vinculos::add);
        vinculo(202L, "22222222222", TipoVinculo.PROPRIETARIO, true, LocalDate.of(2019, 6, 1), null)
                .ifPresent(vinculos::add);

        vinculoUnidadeRepository.saveAll(vinculos);
        log.info("Carga inicial: {} vinculos de unidade criados (Apto 101 e Apto 202)", vinculos.size());
    }

    private void criarSolicitacoes() {
        if (solicitacaoVinculoRepository.count() > 0) {
            return;
        }

        Optional<Usuario> carlos = usuarioRepository.findByCpf("22222222222");
        if (carlos.isEmpty()) {
            log.warn("Carga inicial: usuario 22222222222 nao encontrado; solicitacao de exemplo nao criada");
            return;
        }

        SolicitacaoVinculo solicitacao = new SolicitacaoVinculo();
        solicitacao.setCondominioId(CONDOMINIO);
        solicitacao.setUnidadeId(202L);
        solicitacao.setUnidadeIdentificacao("Apto 202");
        solicitacao.setTipoVinculo(TipoVinculo.DEPENDENTE);
        solicitacao.setNomeSolicitado("Lucas Lima");
        solicitacao.setObservacao("Filho do proprietario, 12 anos (ainda sem acesso ao app).");
        solicitacao.setStatus(StatusSolicitacaoVinculo.PENDENTE);
        solicitacao.setSolicitadoEm(Instant.now());
        solicitacao.setSolicitanteId(carlos.get().getId());
        solicitacao.setSolicitanteNome(carlos.get().getNome());

        solicitacaoVinculoRepository.save(solicitacao);
        log.info("Carga inicial: solicitacao de vinculo pendente criada (dependente no Apto 202)");
    }

    /** Monta o vinculo a partir do usuario ja cadastrado; se o CPF nao existir, pula. */
    private Optional<VinculoUnidade> vinculo(Long unidadeId, String cpf, TipoVinculo tipo, boolean principal,
                                             LocalDate inicio, LocalDate fim) {
        Optional<Usuario> usuario = usuarioRepository.findByCpf(cpf);
        if (usuario.isEmpty()) {
            log.warn("Carga inicial: usuario {} nao encontrado; vinculo com a unidade {} nao criado", cpf, unidadeId);
            return Optional.empty();
        }

        VinculoUnidade vinculo = new VinculoUnidade();
        vinculo.setCondominioId(CONDOMINIO);
        vinculo.setUnidadeId(unidadeId);
        vinculo.setPessoaId(usuario.get().getId());
        vinculo.setPessoaNome(usuario.get().getNome());
        vinculo.setPessoaCpf(cpf);
        vinculo.setPessoaTelefone(usuario.get().getTelefone());
        vinculo.setTipoVinculo(tipo);
        vinculo.setPrincipal(principal);
        vinculo.setInicio(inicio);
        vinculo.setFim(fim);
        vinculo.setAtivo(true);
        return Optional.of(vinculo);
    }
}
