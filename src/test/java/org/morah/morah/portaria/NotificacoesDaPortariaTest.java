package org.morah.morah.portaria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.morah.morah.notificacao.modelo.CanalNotificacao;
import org.morah.morah.notificacao.modelo.Notificacao;
import org.morah.morah.notificacao.modelo.TipoNotificacao;
import org.morah.morah.notificacao.repositorio.NotificacaoRepository;
import org.morah.morah.notificacao.strategy.CanalDeEnvioStrategy;
import org.morah.morah.notificacao.strategy.SeletorDeCanal;
import org.morah.morah.notificacao.template.NotificacaoDeDecisaoDeVisita;
import org.morah.morah.notificacao.template.NotificacaoDeVisitanteAguardando;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.morah.morah.usuario.modelo.Usuario;

/** Os dois TEMPLATE METHODs de notificacao da portaria: tipo, textos, referencia e canal. */
class NotificacoesDaPortariaTest {

    private final NotificacaoRepository repositorio = mock(NotificacaoRepository.class);
    private final CanalDeEnvioStrategy canalPush = mock(CanalDeEnvioStrategy.class);
    private SeletorDeCanal seletorDeCanal;
    private final Usuario destinatario = new Usuario();

    @BeforeEach
    void preparar() {
        when(canalPush.canal()).thenReturn(CanalNotificacao.PUSH);
        seletorDeCanal = new SeletorDeCanal(List.of(canalPush));
        when(repositorio.save(any(Notificacao.class))).thenAnswer(chamada -> chamada.getArgument(0));
        destinatario.setId(1L);
    }

    @Test
    @DisplayName("visitante aguardando: tipo portaria, push, com o nome do visitante e a hora limite")
    void visitanteAguardando() {
        AutorizacaoVisita autorizacao = PortariaFixtures.pendente(7L);
        // 13:30 em Sao Paulo (UTC-3)
        autorizacao.setExpiraEm(Instant.parse("2026-10-04T16:30:00Z"));

        Notificacao notificacao = new NotificacaoDeVisitanteAguardando(repositorio, seletorDeCanal)
                .enviar(destinatario, autorizacao);

        assertThat(notificacao.getTipo()).isEqualTo(TipoNotificacao.PORTARIA);
        assertThat(notificacao.getCanal()).isEqualTo(CanalNotificacao.PUSH);
        assertThat(notificacao.getTitulo()).isEqualTo("Visitante Joao Visitante aguardando autorizacao");
        assertThat(notificacao.getMensagem()).contains("motivo: Visita").contains("ate as 13:30");
        assertThat(notificacao.getReferenciaId()).isEqualTo(7L);
        verify(canalPush).enviar(notificacao, destinatario);
    }

    @Test
    @DisplayName("decisao: avisa a portaria com a unidade e a justificativa")
    void decisaoDeVisita() {
        AutorizacaoVisita autorizacao = PortariaFixtures.pendente(7L);
        autorizacao.setStatus(StatusAutorizacaoVisita.RECUSADA);
        autorizacao.setJustificativa("Nao conheco");

        Notificacao notificacao = new NotificacaoDeDecisaoDeVisita(repositorio, seletorDeCanal)
                .enviar(destinatario, autorizacao);

        assertThat(notificacao.getTipo()).isEqualTo(TipoNotificacao.PORTARIA);
        assertThat(notificacao.getTitulo()).isEqualTo("Entrada de Joao Visitante recusada");
        assertThat(notificacao.getMensagem())
                .isEqualTo("Entrada de Joao Visitante recusada pela unidade Bloco A - Apto 101. Justificativa: Nao conheco");
        assertThat(notificacao.getReferenciaId()).isEqualTo(7L);
    }
}
