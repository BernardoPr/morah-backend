package org.morah.morah.financeiro.servico;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.morah.morah.comum.erro.RegraDeNegocioException;
import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.config.PropriedadesFinanceiro;
import org.morah.morah.financeiro.dto.PixResponse;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.seguranca.jwt.UsuarioAutenticado;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * GET /financeiro/cobrancas/{cobrancaId}/pix: entrega o codigo PIX copia-e-cola da cobranca.
 *
 * <p>O codigo e SIMULADO (veja {@link GeradorDeCodigoPix}): formato BR Code valido, mas sem banco
 * por tras. O pagamento "acontece" quando o gateway chama o webhook.
 *
 * <p>Enquanto o codigo gerado nao expira, ele e reaproveitado: o morador pode abrir a tela varias
 * vezes e recebe sempre o mesmo codigo (como num app de banco).
 */
@Service
@RequiredArgsConstructor
public class PixService {

    /** O BR Code exige a cidade do recebedor; nao temos esse dado do condominio, entao e fixa. */
    static final String CIDADE_DO_RECEBEDOR = "SAO PAULO";
    private static final String NOME_PADRAO_DO_RECEBEDOR = "CONDOMINIO MORAH";
    private static final int TAMANHO_MAXIMO_DO_NOME = 25;
    private static final String PREFIXO_DO_TXID = "MORAH";

    private final CobrancaService cobrancaService;
    private final CobrancaRepository cobrancaRepository;
    private final CalculadoraDeEncargos calculadoraDeEncargos;
    private final GeradorDeCodigoPix geradorDeCodigoPix;
    private final PropriedadesFinanceiro propriedades;

    public PixResponse obterPix(UsuarioAutenticado usuario, Long cobrancaId) {
        Cobranca cobranca = cobrancaService.buscarVisivelPara(usuario, cobrancaId); // 404 / 403
        CobrancaService.exigirEmAberto(cobranca);                                   // 409

        Instant agora = Instant.now();
        if (cobranca.getCodigoPix() != null && cobranca.getPixExpiraEm() != null
                && cobranca.getPixExpiraEm().isAfter(agora)) {
            return new PixResponse(cobranca.getCodigoPix(), cobranca.getPixExpiraEm());
        }

        if (propriedades.chavePix() == null || propriedades.chavePix().isBlank()) {
            throw new RegraDeNegocioException("O condominio ainda nao configurou uma chave PIX.");
        }

        // Valor de hoje: se a cobranca esta atrasada, o PIX ja inclui multa e juros.
        BigDecimal valor = calculadoraDeEncargos.situacao(cobranca, Datas.hoje()).valorTotal();

        String codigo = geradorDeCodigoPix.gerar(
                propriedades.chavePix(),
                valor,
                nomeDoRecebedor(usuario.condominioNome()),
                CIDADE_DO_RECEBEDOR,
                PREFIXO_DO_TXID + cobranca.getId());

        cobranca.setCodigoPix(codigo);
        cobranca.setPixExpiraEm(agora.plus(Duration.ofMinutes(propriedades.minutosValidadePix())));
        cobrancaRepository.save(cobranca);

        return new PixResponse(cobranca.getCodigoPix(), cobranca.getPixExpiraEm());
    }

    private static String nomeDoRecebedor(String nomeDoCondominio) {
        String nome = GeradorDeCodigoPix.textoSimples(nomeDoCondominio, TAMANHO_MAXIMO_DO_NOME);
        return nome.isEmpty() ? NOME_PADRAO_DO_RECEBEDOR : nome;
    }
}
