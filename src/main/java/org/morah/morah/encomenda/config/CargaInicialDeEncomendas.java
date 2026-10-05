package org.morah.morah.encomenda.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.morah.morah.encomenda.modelo.Encomenda;
import org.morah.morah.encomenda.modelo.StatusEncomenda;
import org.morah.morah.encomenda.repositorio.EncomendaRepository;
import org.morah.morah.encomenda.servico.GeradorDeCodigoDeRetirada;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.morah.morah.usuario.modelo.Usuario;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Encomendas de exemplo para testar o modulo logo depois de subir a aplicacao.
 *
 * <ul>
 *   <li>uma encomenda dos Correios AGUARDANDO_RETIRADA para o Apto 101 (Ana/Bruno) - o codigo
 *       de retirada sai no log, para testar o POST /encomendas/{id}/retirada com a Joana;</li>
 *   <li>uma encomenda ja RETIRADA pelo Carlos no Apto 202 (historico).</li>
 * </ul>
 *
 * <p>Roda depois da carga das unidades e usuarios ({@code @Order(1)}) e so cria os dados se o
 * condominio ainda nao tiver nenhuma encomenda (idempotente).
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "morah.carga-inicial", havingValue = "true")
public class CargaInicialDeEncomendas implements CommandLineRunner {

    private static final Long CONDOMINIO = 1L;
    private static final String CPF_DA_PORTARIA = "33333333333"; // Joana Reis

    private final EncomendaRepository encomendaRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeradorDeCodigoDeRetirada geradorDeCodigo;

    @Override
    public void run(String... args) {
        if (encomendaRepository.countByCondominioId(CONDOMINIO) > 0) {
            return; // ja existem encomendas, nao faz nada
        }
        if (!unidadeRepository.existsById(101L) || !unidadeRepository.existsById(202L)) {
            log.warn("Carga inicial: unidades 101/202 nao encontradas, encomendas de exemplo nao criadas");
            return;
        }

        Usuario porteira = usuarioRepository.findByCpf(CPF_DA_PORTARIA).orElse(null);
        Instant agora = Instant.now();

        Encomenda aguardando = encomenda(101L, "Livraria Exemplo", "Correios", "BR123456789BR",
                agora.minus(2, ChronoUnit.HOURS), porteira);

        Encomenda retirada = encomenda(202L, "Loja de Eletronicos", "Jadlog", "JD000111222",
                agora.minus(1, ChronoUnit.DAYS), porteira);
        retirada.setStatus(StatusEncomenda.RETIRADA);
        retirada.setRetiradaEm(agora.minus(20, ChronoUnit.HOURS));
        retirada.setRetiradaPorNome("Carlos Lima");
        if (porteira != null) {
            retirada.setEntreguePorId(porteira.getId());
            retirada.setEntreguePorNome(porteira.getNome());
        }

        List<Encomenda> salvas = encomendaRepository.saveAll(List.of(aguardando, retirada));
        log.info("Carga inicial: encomenda {} (Correios, Apto 101) aguardando retirada - codigo {}",
                salvas.get(0).getId(), salvas.get(0).getCodigoRetirada());
        log.info("Carga inicial: encomenda {} (Jadlog, Apto 202) ja retirada", salvas.get(1).getId());
    }

    private Encomenda encomenda(Long unidadeId, String remetente, String transportadora,
                                String codigoRastreio, Instant recebidaEm, Usuario porteira) {
        Encomenda encomenda = new Encomenda();
        encomenda.setCondominioId(CONDOMINIO);
        encomenda.setUnidadeId(unidadeId);
        encomenda.setRemetente(remetente);
        encomenda.setTransportadora(transportadora);
        encomenda.setCodigoRastreio(codigoRastreio);
        encomenda.setCodigoRetirada(geradorDeCodigo.gerar());
        encomenda.setRecebidaEm(recebidaEm);
        encomenda.setStatus(StatusEncomenda.AGUARDANDO_RETIRADA);
        if (porteira != null) {
            encomenda.setRecebidaPorId(porteira.getId());
            encomenda.setRecebidaPorNome(porteira.getNome());
        }
        return encomenda;
    }
}
