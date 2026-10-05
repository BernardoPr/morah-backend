package org.morah.morah.encomenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.AcessoNegadoException;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.encomenda.dto.AutorizacaoRetiradaCreateRequest;
import org.morah.morah.encomenda.dto.AutorizacaoRetiradaResponse;
import org.morah.morah.encomenda.dto.EncomendaCreateRequest;
import org.morah.morah.encomenda.dto.EncomendaResponse;
import org.morah.morah.encomenda.dto.OcorrenciaCreateRequest;
import org.morah.morah.encomenda.dto.RetiradaConfirmRequest;
import org.morah.morah.encomenda.modelo.AutorizacaoRetirada;
import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.encomenda.modelo.StatusAutorizacaoRetirada;
import org.morah.morah.encomenda.modelo.StatusEncomenda;
import org.morah.morah.encomenda.repositorio.AutorizacaoRetiradaRepository;
import org.morah.morah.encomenda.repositorio.EncomendaRepository;
import org.morah.morah.encomenda.servico.EncomendaService;
import org.morah.morah.encomenda.servico.GeradorDeCodigoDeRetirada;
import org.morah.morah.encomenda.strategy.ValidacaoAutorizacaoDeTerceiroStrategy;
import org.morah.morah.encomenda.strategy.ValidacaoCodigoDoMoradorStrategy;
import org.morah.morah.notificacao.template.NotificacaoDeEncomendaRecebida;
import org.morah.morah.notificacao.template.NotificacaoDeEncomendaRetirada;
import org.morah.morah.ocorrencia.dto.NovaOcorrencia;
import org.morah.morah.ocorrencia.dto.OcorrenciaResponse;
import org.morah.morah.ocorrencia.modelo.Ocorrencia;
import org.morah.morah.ocorrencia.modelo.OrigemOcorrencia;
import org.morah.morah.ocorrencia.modelo.StatusOcorrencia;
import org.morah.morah.ocorrencia.repositorio.OcorrenciaRepository;
import org.morah.morah.ocorrencia.servico.OcorrenciaService;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Regras do {@link EncomendaService} com os repositorios "de mentira" (Mockito): roda sem banco.
 *
 * <p>As duas estrategias de retirada sao as classes reais - assim o teste tambem prova que o
 * service percorre a lista de {@code ValidacaoDeRetiradaStrategy} do jeito certo.
 */
@ExtendWith(MockitoExtension.class)
class EncomendaServiceTest {

    private static final Long CONDOMINIO = 1L;
    private static final Long UNIDADE = 101L;
    private static final Long OUTRA_UNIDADE = 202L;
    private static final String CODIGO_DO_MORADOR = "123456";

    @Mock private EncomendaRepository encomendaRepository;
    @Mock private AutorizacaoRetiradaRepository autorizacaoRetiradaRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private OcorrenciaRepository ocorrenciaRepository;
    @Mock private OcorrenciaService ocorrenciaService;
    @Mock private NotificacaoDeEncomendaRecebida notificacaoDeEncomendaRecebida;
    @Mock private NotificacaoDeEncomendaRetirada notificacaoDeEncomendaRetirada;

    private EncomendaService service;

    @BeforeEach
    void montarService() {
        service = new EncomendaService(
                encomendaRepository,
                autorizacaoRetiradaRepository,
                unidadeRepository,
                usuarioRepository,
                ocorrenciaRepository,
                ocorrenciaService,
                new GeradorDeCodigoDeRetirada(),
                notificacaoDeEncomendaRecebida,
                notificacaoDeEncomendaRetirada,
                List.of(new ValidacaoCodigoDoMoradorStrategy(),
                        new ValidacaoAutorizacaoDeTerceiroStrategy(autorizacaoRetiradaRepository)));
    }

    @AfterEach
    void deslogar() {
        SecurityContextHolder.clearContext();
    }

    // ---------- registro ----------

    @Nested
    class Registrar {

        @Test
        @DisplayName("unidade de outro condominio devolve 404 e nada e gravado")
        void unidadeDeOutroCondominio() {
            logarComoPortaria();
            when(unidadeRepository.findByIdAndCondominioId(999L, CONDOMINIO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.criar(new EncomendaCreateRequest(999L, null, "Correios", null, null)))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("Unidade");
            verify(encomendaRepository, never()).save(any());
        }

        @Test
        @DisplayName("gera o codigo, grava quem recebeu, notifica a unidade e esconde o codigo da portaria")
        void registraENotifica() {
            logarComoPortaria();
            when(unidadeRepository.findByIdAndCondominioId(UNIDADE, CONDOMINIO)).thenReturn(Optional.of(new Unidade()));
            when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(chamada -> {
                Encomenda encomenda = chamada.getArgument(0);
                encomenda.setId(10L);
                return encomenda;
            });
            Usuario ana = usuario(1L);
            Usuario bruno = usuario(4L);
            when(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(UNIDADE)).thenReturn(List.of(ana, bruno));

            EncomendaResponse resposta = service.criar(
                    new EncomendaCreateRequest(UNIDADE, "Loja X", " Correios ", "BR1", null));

            ArgumentCaptor<Encomenda> gravada = ArgumentCaptor.forClass(Encomenda.class);
            verify(encomendaRepository).save(gravada.capture());
            Encomenda encomenda = gravada.getValue();
            assertThat(encomenda.getCodigoRetirada()).matches("\\d{6}");
            assertThat(encomenda.getCondominioId()).isEqualTo(CONDOMINIO);
            assertThat(encomenda.getRecebidaPorId()).isEqualTo(3L);
            assertThat(encomenda.getRecebidaPorNome()).isEqualTo("Joana Reis");
            assertThat(encomenda.getRecebidaEm()).isNotNull();
            assertThat(encomenda.getTransportadora()).isEqualTo("Correios");

            verify(notificacaoDeEncomendaRecebida).enviar(ana, encomenda);
            verify(notificacaoDeEncomendaRecebida).enviar(bruno, encomenda);

            assertThat(resposta.id()).isEqualTo(10L);
            assertThat(resposta.status()).isEqualTo(StatusEncomenda.AGUARDANDO_RETIRADA);
            assertThat(resposta.codigoRetirada()).as("a portaria nunca ve o codigo").isNull();
        }
    }

    // ---------- consulta ----------

    @Nested
    class Consultar {

        @Test
        @DisplayName("portaria detalha a encomenda sem o codigo de retirada")
        void portariaNaoVeOCodigo() {
            logarComoPortaria();
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)));

            assertThat(service.detalhar(10L).codigoRetirada()).isNull();
        }

        @Test
        @DisplayName("sindico tambem nao ve o codigo")
        void sindicoNaoVeOCodigo() {
            logarComo(Perfil.SINDICO, null);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)));

            assertThat(service.detalhar(10L).codigoRetirada()).isNull();
        }

        @Test
        @DisplayName("morador da propria unidade ve o codigo")
        void moradorVeOCodigo() {
            logarComo(Perfil.MORADOR, UNIDADE);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)));

            assertThat(service.detalhar(10L).codigoRetirada()).isEqualTo(CODIGO_DO_MORADOR);
        }

        @Test
        @DisplayName("morador de outra unidade recebe 403")
        void moradorDeOutraUnidade() {
            logarComo(Perfil.MORADOR, OUTRA_UNIDADE);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)));

            assertThatThrownBy(() -> service.detalhar(10L)).isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        @DisplayName("encomenda de outro condominio devolve 404")
        void outroCondominio() {
            logarComoPortaria();
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.detalhar(10L)).isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        @DisplayName("morador lista so a unidade do token, com o codigo")
        void moradorListaASuaUnidade() {
            logarComo(Perfil.PROPRIETARIO, UNIDADE);
            Pageable paginacao = PageRequest.of(0, 20);
            Page<Encomenda> pagina = new PageImpl<>(List.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)), paginacao, 1);
            when(encomendaRepository.findByCondominioIdAndUnidadeIdAndStatus(
                    CONDOMINIO, UNIDADE, StatusEncomenda.AGUARDANDO_RETIRADA, paginacao)).thenReturn(pagina);

            var resposta = service.listarDoContextoAtivo(StatusEncomenda.AGUARDANDO_RETIRADA, paginacao);

            assertThat(resposta.content()).singleElement()
                    .extracting(EncomendaResponse::codigoRetirada).isEqualTo(CODIGO_DO_MORADOR);
            assertThat(resposta.page().totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("portaria lista o condominio inteiro, sem os codigos")
        void portariaListaOCondominio() {
            logarComoPortaria();
            Pageable paginacao = PageRequest.of(0, 20);
            when(encomendaRepository.findByCondominioId(CONDOMINIO, paginacao))
                    .thenReturn(new PageImpl<>(List.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)), paginacao, 1));

            var resposta = service.listarDoContextoAtivo(null, paginacao);

            assertThat(resposta.content()).singleElement()
                    .extracting(EncomendaResponse::codigoRetirada).isNull();
        }
    }

    // ---------- autorizacao de terceiro ----------

    @Nested
    class AutorizarTerceiro {

        @Test
        @DisplayName("encomenda ja retirada devolve 409")
        void encomendaJaRetirada() {
            logarComo(Perfil.MORADOR, UNIDADE);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.RETIRADA)));

            assertThatThrownBy(() -> service.autorizarRetiradaDeTerceiro(10L,
                    new AutorizacaoRetiradaCreateRequest("Marcos", "RG 123", null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nao esta aguardando retirada");
            verify(autorizacaoRetiradaRepository, never()).save(any());
        }

        @Test
        @DisplayName("morador de outra unidade nao pode autorizar (403)")
        void outraUnidade() {
            logarComo(Perfil.MORADOR, OUTRA_UNIDADE);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)));

            assertThatThrownBy(() -> service.autorizarRetiradaDeTerceiro(10L,
                    new AutorizacaoRetiradaCreateRequest("Marcos", "RG 123", null)))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        @DisplayName("gera codigo proprio (diferente do morador), validade padrao de 24h e status ativa")
        void criaAutorizacao() {
            logarComo(Perfil.MORADOR, UNIDADE);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)));
            when(autorizacaoRetiradaRepository.findByEncomendaIdAndStatus(10L, StatusAutorizacaoRetirada.ATIVA))
                    .thenReturn(List.of());
            when(autorizacaoRetiradaRepository.save(any(AutorizacaoRetirada.class))).thenAnswer(chamada -> {
                AutorizacaoRetirada autorizacao = chamada.getArgument(0);
                autorizacao.setId(50L);
                return autorizacao;
            });

            AutorizacaoRetiradaResponse resposta = service.autorizarRetiradaDeTerceiro(10L,
                    new AutorizacaoRetiradaCreateRequest(" Marcos ", "RG 123", null));

            assertThat(resposta.id()).isEqualTo(50L);
            assertThat(resposta.encomendaId()).isEqualTo(10L);
            assertThat(resposta.nomeTerceiro()).isEqualTo("Marcos");
            assertThat(resposta.codigo()).matches("\\d{6}").isNotEqualTo(CODIGO_DO_MORADOR);
            assertThat(resposta.status()).isEqualTo(StatusAutorizacaoRetirada.ATIVA);
            assertThat(resposta.validade()).isCloseTo(Instant.now().plus(24, ChronoUnit.HOURS), within(1, ChronoUnit.MINUTES));
        }
    }

    // ---------- retirada na portaria (Strategy) ----------

    @Nested
    class ConfirmarRetirada {

        @Test
        @DisplayName("codigo do morador: marca retirada, grava quem entregou e invalida as autorizacoes que sobraram")
        void comCodigoDoMorador() {
            logarComoPortaria();
            Encomenda encomenda = encomenda(StatusEncomenda.AGUARDANDO_RETIRADA);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO)).thenReturn(Optional.of(encomenda));
            when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(chamada -> chamada.getArgument(0));
            AutorizacaoRetirada sobrando = autorizacao(50L, "654321", Instant.now().plus(5, ChronoUnit.HOURS));
            when(autorizacaoRetiradaRepository.findByEncomendaIdAndStatus(10L, StatusAutorizacaoRetirada.ATIVA))
                    .thenReturn(List.of(sobrando));
            Usuario ana = usuario(1L);
            when(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(UNIDADE)).thenReturn(List.of(ana));

            EncomendaResponse resposta = service.confirmarRetirada(10L,
                    new RetiradaConfirmRequest(" 123456 ", "Ana Souza", "MG 1"));

            assertThat(resposta.status()).isEqualTo(StatusEncomenda.RETIRADA);
            assertThat(resposta.retiradaEm()).isNotNull();
            assertThat(resposta.codigoRetirada()).isNull();
            assertThat(encomenda.getRetiradaPorNome()).isEqualTo("Ana Souza");
            assertThat(encomenda.getRetiradaPorDocumento()).isEqualTo("MG 1");
            assertThat(encomenda.getEntreguePorId()).isEqualTo(3L);
            assertThat(encomenda.getAutorizacaoRetiradaId()).isNull();
            assertThat(sobrando.getStatus()).as("a autorizacao que sobrou deixa de valer")
                    .isEqualTo(StatusAutorizacaoRetirada.EXPIRADA);
            verify(autorizacaoRetiradaRepository).saveAll(List.of(sobrando));
            verify(notificacaoDeEncomendaRetirada).enviar(ana, encomenda);
        }

        @Test
        @DisplayName("codigo de terceiro: autorizacao vira UTILIZADA e nome/documento vem dela")
        void comCodigoDeTerceiro() {
            logarComoPortaria();
            Encomenda encomenda = encomenda(StatusEncomenda.AGUARDANDO_RETIRADA);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO)).thenReturn(Optional.of(encomenda));
            when(encomendaRepository.save(any(Encomenda.class))).thenAnswer(chamada -> chamada.getArgument(0));
            AutorizacaoRetirada autorizacao = autorizacao(50L, "654321", Instant.now().plus(5, ChronoUnit.HOURS));
            when(autorizacaoRetiradaRepository.findByEncomendaIdAndStatus(10L, StatusAutorizacaoRetirada.ATIVA))
                    .thenReturn(List.of(autorizacao));
            when(autorizacaoRetiradaRepository.findById(50L)).thenReturn(Optional.of(autorizacao));
            when(usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(UNIDADE)).thenReturn(List.of());

            EncomendaResponse resposta = service.confirmarRetirada(10L,
                    new RetiradaConfirmRequest("654321", null, " "));

            assertThat(resposta.status()).isEqualTo(StatusEncomenda.RETIRADA);
            assertThat(autorizacao.getStatus()).isEqualTo(StatusAutorizacaoRetirada.UTILIZADA);
            assertThat(autorizacao.getUtilizadaEm()).isNotNull();
            verify(autorizacaoRetiradaRepository).save(autorizacao);
            assertThat(encomenda.getRetiradaPorNome()).isEqualTo("Marcos Terceiro");
            assertThat(encomenda.getRetiradaPorDocumento()).isEqualTo("RG 123");
            assertThat(encomenda.getAutorizacaoRetiradaId()).isEqualTo(50L);
            verify(autorizacaoRetiradaRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("codigo errado devolve 409 e nada muda")
        void codigoErrado() {
            logarComoPortaria();
            Encomenda encomenda = encomenda(StatusEncomenda.AGUARDANDO_RETIRADA);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO)).thenReturn(Optional.of(encomenda));
            when(autorizacaoRetiradaRepository.findByEncomendaIdAndStatus(10L, StatusAutorizacaoRetirada.ATIVA))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.confirmarRetirada(10L, new RetiradaConfirmRequest("000000", null, null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Codigo de retirada invalido ou expirado");
            assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.AGUARDANDO_RETIRADA);
            verify(encomendaRepository, never()).save(any());
        }

        @Test
        @DisplayName("codigo de terceiro com a validade vencida devolve 409")
        void codigoDeTerceiroExpirado() {
            logarComoPortaria();
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)));
            when(autorizacaoRetiradaRepository.findByEncomendaIdAndStatus(10L, StatusAutorizacaoRetirada.ATIVA))
                    .thenReturn(List.of(autorizacao(50L, "654321", Instant.now().minus(1, ChronoUnit.HOURS))));

            assertThatThrownBy(() -> service.confirmarRetirada(10L, new RetiradaConfirmRequest("654321", null, null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Codigo de retirada invalido ou expirado");
            verify(encomendaRepository, never()).save(any());
        }

        @Test
        @DisplayName("encomenda que nao esta aguardando retirada devolve 409 antes de conferir o codigo")
        void encomendaJaRetirada() {
            logarComoPortaria();
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.RETIRADA)));

            assertThatThrownBy(() -> service.confirmarRetirada(10L, new RetiradaConfirmRequest(CODIGO_DO_MORADOR, null, null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nao esta aguardando retirada");
        }
    }

    // ---------- ocorrencia de extravio ----------

    @Nested
    class AbrirOcorrencia {

        private final OcorrenciaCreateRequest requisicao =
                new OcorrenciaCreateRequest("Pacote sumiu", "Nao estava na portaria");

        @Test
        @DisplayName("encomenda aguardando retirada vira EXTRAVIADA e guarda o id da ocorrencia")
        void mudaParaExtraviada() {
            logarComo(Perfil.MORADOR, UNIDADE);
            Encomenda encomenda = encomenda(StatusEncomenda.AGUARDANDO_RETIRADA);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO)).thenReturn(Optional.of(encomenda));
            when(ocorrenciaRepository.findByOrigemAndReferenciaId(OrigemOcorrencia.ENCOMENDA, 10L)).thenReturn(List.of());
            when(ocorrenciaService.abrir(any(NovaOcorrencia.class))).thenReturn(
                    new OcorrenciaResponse(77L, "Pacote sumiu", "Nao estava na portaria",
                            StatusOcorrencia.ABERTA, Instant.now(), null));

            OcorrenciaResponse resposta = service.abrirOcorrencia(10L, requisicao);

            ArgumentCaptor<NovaOcorrencia> dados = ArgumentCaptor.forClass(NovaOcorrencia.class);
            verify(ocorrenciaService).abrir(dados.capture());
            assertThat(dados.getValue().origem()).isEqualTo(OrigemOcorrencia.ENCOMENDA);
            assertThat(dados.getValue().referenciaId()).isEqualTo(10L);
            assertThat(dados.getValue().unidadeId()).isEqualTo(UNIDADE);
            assertThat(dados.getValue().abertaPorId()).isEqualTo(1L);

            assertThat(resposta.id()).isEqualTo(77L);
            assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.EXTRAVIADA);
            assertThat(encomenda.getOcorrenciaId()).isEqualTo(77L);
            verify(encomendaRepository).save(encomenda);
        }

        @Test
        @DisplayName("ja existe ocorrencia aberta para a encomenda: 409")
        void ocorrenciaDuplicada() {
            logarComo(Perfil.MORADOR, UNIDADE);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO))
                    .thenReturn(Optional.of(encomenda(StatusEncomenda.EXTRAVIADA)));
            when(ocorrenciaRepository.findByOrigemAndReferenciaId(OrigemOcorrencia.ENCOMENDA, 10L))
                    .thenReturn(List.of(ocorrencia(StatusOcorrencia.EM_ANDAMENTO)));

            assertThatThrownBy(() -> service.abrirOcorrencia(10L, requisicao))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Ja existe uma ocorrencia aberta");
            verify(ocorrenciaService, never()).abrir(any());
        }

        @Test
        @DisplayName("encomenda ja retirada: aceita como contestacao e o status continua RETIRADA")
        void contestacaoDeEncomendaRetirada() {
            logarComo(Perfil.MORADOR, UNIDADE);
            Encomenda encomenda = encomenda(StatusEncomenda.RETIRADA);
            when(encomendaRepository.findByIdAndCondominioId(10L, CONDOMINIO)).thenReturn(Optional.of(encomenda));
            // a ocorrencia anterior ja foi encerrada: pode abrir outra
            when(ocorrenciaRepository.findByOrigemAndReferenciaId(OrigemOcorrencia.ENCOMENDA, 10L))
                    .thenReturn(List.of(ocorrencia(StatusOcorrencia.ENCERRADA)));
            when(ocorrenciaService.abrir(any(NovaOcorrencia.class))).thenReturn(
                    new OcorrenciaResponse(78L, "Nao fui eu", "Nao reconheco quem retirou",
                            StatusOcorrencia.ABERTA, Instant.now(), null));

            service.abrirOcorrencia(10L, requisicao);

            assertThat(encomenda.getStatus()).isEqualTo(StatusEncomenda.RETIRADA);
            assertThat(encomenda.getOcorrenciaId()).isEqualTo(78L);
        }
    }

    // ---------- metodos do dashboard ----------

    @Nested
    class Dashboard {

        @Test
        @DisplayName("pendentes da unidade vem com o codigo, mais recentes primeiro e no limite pedido")
        void pendentesDaUnidade() {
            when(encomendaRepository.findByUnidadeIdAndStatus(eq(UNIDADE), eq(StatusEncomenda.AGUARDANDO_RETIRADA), any()))
                    .thenReturn(List.of(encomenda(StatusEncomenda.AGUARDANDO_RETIRADA)));

            List<EncomendaResponse> pendentes = service.pendentesDaUnidade(UNIDADE, 3);

            assertThat(pendentes).singleElement()
                    .extracting(EncomendaResponse::codigoRetirada).isEqualTo(CODIGO_DO_MORADOR);
            ArgumentCaptor<Pageable> paginacao = ArgumentCaptor.forClass(Pageable.class);
            verify(encomendaRepository).findByUnidadeIdAndStatus(eq(UNIDADE), eq(StatusEncomenda.AGUARDANDO_RETIRADA),
                    paginacao.capture());
            assertThat(paginacao.getValue().getPageSize()).isEqualTo(3);
            assertThat(paginacao.getValue().getSort().getOrderFor("recebidaEm").getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("limite zero nao consulta o banco")
        void limiteZero() {
            assertThat(service.pendentesDaUnidade(UNIDADE, 0)).isEmpty();
            verify(encomendaRepository, never()).findByUnidadeIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("recebidas hoje conta do inicio do dia (inclusivo) ao inicio do dia seguinte (exclusivo)")
        void recebidasHoje() {
            when(encomendaRepository.contarRecebidasEntre(eq(CONDOMINIO), any(), any())).thenReturn(4L);

            assertThat(service.contarRecebidasHoje(CONDOMINIO)).isEqualTo(4L);

            ArgumentCaptor<Instant> inicio = ArgumentCaptor.forClass(Instant.class);
            ArgumentCaptor<Instant> fim = ArgumentCaptor.forClass(Instant.class);
            verify(encomendaRepository).contarRecebidasEntre(eq(CONDOMINIO), inicio.capture(), fim.capture());
            var dia = Datas.dataDe(inicio.getValue());
            assertThat(inicio.getValue()).isEqualTo(Datas.inicioDoDia(dia));
            assertThat(fim.getValue()).isEqualTo(Datas.fimDoDia(dia));
            assertThat(Instant.now()).isBetween(inicio.getValue(), fim.getValue());
        }
    }

    // ---------- apoio ----------

    private void logarComoPortaria() {
        var joana = new UsuarioAutenticado(3L, "Joana Reis", "33333333333",
                Perfil.PORTARIA, CONDOMINIO, "Residencial Morah", null, "jti");
        autenticar(joana);
    }

    private void logarComo(Perfil perfil, Long unidadeId) {
        autenticar(new UsuarioAutenticado(1L, "Ana Souza", "11111111111",
                perfil, CONDOMINIO, "Residencial Morah", unidadeId, "jti"));
    }

    private void autenticar(UsuarioAutenticado usuario) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    private static Encomenda encomenda(StatusEncomenda status) {
        Encomenda encomenda = new Encomenda();
        encomenda.setId(10L);
        encomenda.setCondominioId(CONDOMINIO);
        encomenda.setUnidadeId(UNIDADE);
        encomenda.setTransportadora("Correios");
        encomenda.setCodigoRetirada(CODIGO_DO_MORADOR);
        encomenda.setRecebidaEm(Instant.now().minus(1, ChronoUnit.HOURS));
        encomenda.setStatus(status);
        return encomenda;
    }

    private static AutorizacaoRetirada autorizacao(Long id, String codigo, Instant validade) {
        AutorizacaoRetirada autorizacao = new AutorizacaoRetirada();
        autorizacao.setId(id);
        autorizacao.setEncomendaId(10L);
        autorizacao.setNomeTerceiro("Marcos Terceiro");
        autorizacao.setDocumento("RG 123");
        autorizacao.setCodigo(codigo);
        autorizacao.setValidade(validade);
        autorizacao.setStatus(StatusAutorizacaoRetirada.ATIVA);
        return autorizacao;
    }

    private static Ocorrencia ocorrencia(StatusOcorrencia status) {
        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.setId(70L);
        ocorrencia.setStatus(status);
        return ocorrencia;
    }

    private static Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        return usuario;
    }
}
