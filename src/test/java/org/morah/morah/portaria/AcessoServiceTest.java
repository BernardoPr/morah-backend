package org.morah.morah.portaria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.morah.morah.comum.erro.RecursoNaoEncontradoException;
import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.template.NotificacaoDeDecisaoDeVisita;
import org.morah.morah.notificacao.template.NotificacaoDeVisitanteAguardando;
import org.morah.morah.portaria.dto.AcessoCreateRequest;
import org.morah.morah.portaria.dto.AcessoResponse;
import org.morah.morah.portaria.modelo.Acesso;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.StatusAcesso;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.morah.morah.portaria.modelo.TipoAcesso;
import org.morah.morah.portaria.repositorio.AcessoRepository;
import org.morah.morah.portaria.repositorio.AutorizacaoVisitaRepository;
import org.morah.morah.portaria.servico.AcessoService;
import org.morah.morah.portaria.servico.AutorizacaoVisitaService;
import org.morah.morah.portaria.servico.VisitanteService;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Regras do registro de entrada/saida. O {@link AutorizacaoVisitaService} usado aqui e o de
 * verdade (com repositorios mockados), para exercitar junto o STATE que libera a entrada.
 */
@ExtendWith(MockitoExtension.class)
class AcessoServiceTest {

    private static final Long VISITANTE = 10L;

    @Mock
    private AcessoRepository acessoRepository;
    @Mock
    private AutorizacaoVisitaRepository autorizacaoRepository;
    @Mock
    private VisitanteService visitanteService;
    @Mock
    private UnidadeRepository unidadeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacaoDeVisitanteAguardando notificacaoDeVisitanteAguardando;
    @Mock
    private NotificacaoDeDecisaoDeVisita notificacaoDeDecisaoDeVisita;

    private AcessoService service;

    @BeforeEach
    void montarService() {
        var autorizacaoVisitaService = new AutorizacaoVisitaService(autorizacaoRepository, unidadeRepository,
                usuarioRepository, visitanteService, PortariaFixtures.seletorDeEstado(),
                notificacaoDeVisitanteAguardando, notificacaoDeDecisaoDeVisita);
        service = new AcessoService(acessoRepository, visitanteService, autorizacaoVisitaService);
    }

    private void visitanteDoCondominio() {
        when(visitanteService.buscarDoCondominio(VISITANTE, PortariaFixtures.CONDOMINIO))
                .thenReturn(PortariaFixtures.visitante(VISITANTE, "Joao Visitante"));
    }

    private void visitanteAindaNaoEntrou() {
        when(acessoRepository.existsByCondominioIdAndVisitanteIdAndSaidaIsNull(PortariaFixtures.CONDOMINIO, VISITANTE))
                .thenReturn(false);
    }

    private static AcessoCreateRequest entrada(Long autorizacaoId) {
        return new AcessoCreateRequest(autorizacaoId, VISITANTE, TipoAcesso.ENTRADA, null);
    }

    private static AcessoCreateRequest saida() {
        return new AcessoCreateRequest(null, VISITANTE, TipoAcesso.SAIDA, "https://fotos/saida.jpg");
    }

    // ---------- entrada ----------

    @Test
    @DisplayName("entrada com autorizacao concedida: abre o acesso e marca a autorizacao como usada")
    void entradaAutorizada() {
        AutorizacaoVisita autorizacao = PortariaFixtures.autorizada(5L);
        visitanteDoCondominio();
        visitanteAindaNaoEntrou();
        when(autorizacaoRepository.findByIdAndCondominioId(5L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.of(autorizacao));
        when(autorizacaoRepository.save(autorizacao)).thenReturn(autorizacao);
        when(acessoRepository.save(any(Acesso.class))).thenAnswer(chamada -> chamada.getArgument(0));

        AcessoResponse resposta = service.registrar(PortariaFixtures.porteira(), entrada(5L));

        assertThat(resposta.tipo()).isEqualTo(TipoAcesso.ENTRADA);
        assertThat(resposta.status()).isEqualTo(StatusAcesso.EM_ANDAMENTO);
        assertThat(resposta.autorizacaoId()).isEqualTo(5L);
        assertThat(resposta.entrada()).isNotNull();
        assertThat(resposta.saida()).isNull();
        assertThat(resposta.visitante().nome()).isEqualTo("Joao Visitante");
        assertThat(autorizacao.getEntradaEm()).isEqualTo(resposta.entrada());
    }

    @Test
    @DisplayName("entrada sem autorizacaoId: usa a autorizacao concedida mais recente do visitante")
    void entradaSemInformarAutorizacao() {
        AutorizacaoVisita autorizacao = PortariaFixtures.autorizada(7L);
        visitanteDoCondominio();
        visitanteAindaNaoEntrou();
        when(autorizacaoRepository.findFirstByCondominioIdAndVisitanteIdAndStatusAndEntradaEmIsNullOrderByRespondidoEmDesc(
                PortariaFixtures.CONDOMINIO, VISITANTE, StatusAutorizacaoVisita.AUTORIZADA))
                .thenReturn(Optional.of(autorizacao));
        when(autorizacaoRepository.save(autorizacao)).thenReturn(autorizacao);
        when(acessoRepository.save(any(Acesso.class))).thenAnswer(chamada -> chamada.getArgument(0));

        assertThat(service.registrar(PortariaFixtures.porteira(), entrada(null)).autorizacaoId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("entrada sem nenhuma autorizacao concedida -> 409")
    void entradaSemAutorizacao() {
        visitanteDoCondominio();
        visitanteAindaNaoEntrou();
        when(autorizacaoRepository.findFirstByCondominioIdAndVisitanteIdAndStatusAndEntradaEmIsNullOrderByRespondidoEmDesc(
                PortariaFixtures.CONDOMINIO, VISITANTE, StatusAutorizacaoVisita.AUTORIZADA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(PortariaFixtures.porteira(), entrada(null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nao tem autorizacao");
        verify(acessoRepository, never()).save(any());
    }

    @Test
    @DisplayName("entrada com autorizacao ainda pendente -> 409 (o estado PENDENTE nao libera)")
    void entradaComAutorizacaoPendente() {
        visitanteDoCondominio();
        visitanteAindaNaoEntrou();
        when(autorizacaoRepository.findByIdAndCondominioId(5L, PortariaFixtures.CONDOMINIO))
                .thenReturn(Optional.of(PortariaFixtures.pendente(5L)));

        assertThatThrownBy(() -> service.registrar(PortariaFixtures.porteira(), entrada(5L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ainda nao respondeu");
        verify(acessoRepository, never()).save(any());
    }

    @Test
    @DisplayName("entrada com autorizacao de outro visitante -> 409")
    void entradaComAutorizacaoDeOutroVisitante() {
        AutorizacaoVisita deOutro = PortariaFixtures.autorizada(5L);
        deOutro.setVisitanteId(99L);
        visitanteDoCondominio();
        visitanteAindaNaoEntrou();
        when(autorizacaoRepository.findByIdAndCondominioId(5L, PortariaFixtures.CONDOMINIO)).thenReturn(Optional.of(deOutro));

        assertThatThrownBy(() -> service.registrar(PortariaFixtures.porteira(), entrada(5L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("outro visitante");
    }

    @Test
    @DisplayName("entrada de visitante que ja esta dentro -> 409")
    void entradaDuplicada() {
        visitanteDoCondominio();
        when(acessoRepository.existsByCondominioIdAndVisitanteIdAndSaidaIsNull(PortariaFixtures.CONDOMINIO, VISITANTE))
                .thenReturn(true);

        assertThatThrownBy(() -> service.registrar(PortariaFixtures.porteira(), entrada(5L)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ja esta dentro");
        verify(autorizacaoRepository, never()).save(any());
        verify(acessoRepository, never()).save(any());
    }

    @Test
    @DisplayName("visitante de outro condominio -> 404")
    void visitanteDeOutroCondominio() {
        when(visitanteService.buscarDoCondominio(VISITANTE, PortariaFixtures.CONDOMINIO))
                .thenThrow(new RecursoNaoEncontradoException("Visitante", VISITANTE));

        assertThatThrownBy(() -> service.registrar(PortariaFixtures.porteira(), entrada(null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- saida ----------

    @Test
    @DisplayName("saida: completa o acesso em aberto do visitante")
    void saidaFechaOAcessoAberto() {
        Acesso aberto = new Acesso();
        aberto.setId(20L);
        aberto.setVisitanteId(VISITANTE);
        aberto.setAutorizacaoId(5L);
        aberto.setEntrada(Instant.now().minusSeconds(3600));
        aberto.setTipo(TipoAcesso.ENTRADA);
        aberto.setStatus(StatusAcesso.EM_ANDAMENTO);
        visitanteDoCondominio();
        when(acessoRepository.findFirstByCondominioIdAndVisitanteIdAndSaidaIsNullOrderByEntradaDesc(
                PortariaFixtures.CONDOMINIO, VISITANTE)).thenReturn(Optional.of(aberto));
        when(acessoRepository.save(aberto)).thenReturn(aberto);

        AcessoResponse resposta = service.registrar(PortariaFixtures.porteira(), saida());

        assertThat(resposta.id()).isEqualTo(20L);
        assertThat(resposta.tipo()).isEqualTo(TipoAcesso.SAIDA);
        assertThat(resposta.status()).isEqualTo(StatusAcesso.ENCERRADO);
        assertThat(resposta.saida()).isAfter(resposta.entrada());
        assertThat(aberto.getFotoSaidaUrl()).isEqualTo("https://fotos/saida.jpg");
        assertThat(aberto.getSaidaRegistradaPorNome()).isEqualTo("Joana Reis");
    }

    @Test
    @DisplayName("saida sem entrada registrada -> 409")
    void saidaSemEntrada() {
        visitanteDoCondominio();
        when(acessoRepository.findFirstByCondominioIdAndVisitanteIdAndSaidaIsNullOrderByEntradaDesc(
                PortariaFixtures.CONDOMINIO, VISITANTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(PortariaFixtures.porteira(), saida()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Nao ha entrada em aberto");
        verify(acessoRepository, never()).save(any());
    }

    // ---------- historico ----------

    @Test
    @DisplayName("historico com ?data= busca as entradas do dia no fuso do condominio")
    void historicoDoDia() {
        LocalDate dia = LocalDate.of(2026, 10, 4);
        Pageable paginacao = PageRequest.of(0, 20);
        when(acessoRepository.buscarEntradasEntre(PortariaFixtures.CONDOMINIO,
                Datas.inicioDoDia(dia), Datas.fimDoDia(dia), paginacao))
                .thenReturn(new PageImpl<>(List.of()));
        when(acessoRepository.findByCondominioId(PortariaFixtures.CONDOMINIO, paginacao))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.listar(PortariaFixtures.sindico(), dia, paginacao).content()).isEmpty();
        assertThat(service.listar(PortariaFixtures.porteira(), null, paginacao).content()).isEmpty();
    }
}
