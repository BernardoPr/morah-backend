package org.morah.morah.portaria;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.morah.morah.comum.modelo.Perfil;
import org.morah.morah.portaria.modelo.AutorizacaoVisita;
import org.morah.morah.portaria.modelo.DadosDoVisitante;
import org.morah.morah.portaria.modelo.StatusAutorizacaoVisita;
import org.morah.morah.portaria.modelo.Visitante;
import org.morah.morah.portaria.state.EstadoAutorizada;
import org.morah.morah.portaria.state.EstadoExpirada;
import org.morah.morah.portaria.state.EstadoPendente;
import org.morah.morah.portaria.state.EstadoRecusada;
import org.morah.morah.portaria.state.SeletorDeEstadoDaAutorizacao;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;

/** Dados de exemplo dos testes da portaria (mesma historia da carga inicial). */
final class PortariaFixtures {

    static final Long CONDOMINIO = 1L;
    static final Long OUTRO_CONDOMINIO = 2L;
    static final Long APTO_101 = 101L;
    static final Long APTO_102 = 102L;

    private PortariaFixtures() {
    }

    static UsuarioAutenticado porteira() {
        return new UsuarioAutenticado(3L, "Joana Reis", "33333333333", Perfil.PORTARIA,
                CONDOMINIO, "Residencial Morah", null, "jti-portaria");
    }

    static UsuarioAutenticado moradora101() {
        return new UsuarioAutenticado(1L, "Ana Souza", "11111111111", Perfil.MORADOR,
                CONDOMINIO, "Residencial Morah", APTO_101, "jti-morador");
    }

    static UsuarioAutenticado proprietario101() {
        return new UsuarioAutenticado(4L, "Bruno Alves", "44444444444", Perfil.PROPRIETARIO,
                CONDOMINIO, "Residencial Morah", APTO_101, "jti-proprietario");
    }

    static UsuarioAutenticado sindico() {
        return new UsuarioAutenticado(2L, "Carlos Lima", "22222222222", Perfil.SINDICO,
                CONDOMINIO, "Residencial Morah", null, "jti-sindico");
    }

    /** O seletor de verdade, com os quatro estados de verdade (eles nao dependem de banco). */
    static SeletorDeEstadoDaAutorizacao seletorDeEstado() {
        return new SeletorDeEstadoDaAutorizacao(List.of(
                new EstadoPendente(), new EstadoAutorizada(), new EstadoRecusada(), new EstadoExpirada()));
    }

    static Visitante visitante(Long id, String nome) {
        Visitante visitante = new Visitante();
        visitante.setId(id);
        visitante.setCondominioId(CONDOMINIO);
        visitante.setNome(nome);
        visitante.setDocumento("123456789");
        return visitante;
    }

    /** Autorizacao pendente da unidade 101, registrada agora e ainda no prazo. */
    static AutorizacaoVisita pendente(Long id) {
        Instant agora = Instant.now();
        AutorizacaoVisita autorizacao = new AutorizacaoVisita();
        autorizacao.setId(id);
        autorizacao.setCondominioId(CONDOMINIO);
        autorizacao.setUnidadeId(APTO_101);
        autorizacao.setUnidadeDescricao("Bloco A - Apto 101");
        autorizacao.setVisitanteId(10L);
        autorizacao.setVisitante(new DadosDoVisitante("Joao Visitante", "123456789", "11999990000", null));
        autorizacao.setMotivo("Visita");
        autorizacao.setStatus(StatusAutorizacaoVisita.PENDENTE);
        autorizacao.setSolicitadoEm(agora);
        autorizacao.abrirPrazoDeResposta(agora);
        return autorizacao;
    }

    /** Pendente cujo prazo de resposta acabou ha um minuto (ainda gravada como PENDENTE). */
    static AutorizacaoVisita pendenteVencida(Long id) {
        AutorizacaoVisita autorizacao = pendente(id);
        autorizacao.setSolicitadoEm(Instant.now().minus(Duration.ofMinutes(31)));
        autorizacao.setExpiraEm(Instant.now().minus(Duration.ofMinutes(1)));
        return autorizacao;
    }

    /** Autorizada ha 5 minutos e ainda nao usada. */
    static AutorizacaoVisita autorizada(Long id) {
        AutorizacaoVisita autorizacao = pendente(id);
        autorizacao.setStatus(StatusAutorizacaoVisita.AUTORIZADA);
        autorizacao.setRespondidoEm(Instant.now().minus(Duration.ofMinutes(5)));
        return autorizacao;
    }

    static AutorizacaoVisita comStatus(Long id, StatusAutorizacaoVisita status) {
        AutorizacaoVisita autorizacao = pendente(id);
        autorizacao.setStatus(status);
        return autorizacao;
    }
}
