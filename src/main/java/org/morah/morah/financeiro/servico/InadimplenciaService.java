package org.morah.morah.financeiro.servico;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.financeiro.dto.InadimplenciaResumoResponse;
import org.morah.morah.financeiro.modelo.Cobranca;
import org.morah.morah.financeiro.modelo.StatusCobranca;
import org.morah.morah.financeiro.repositorio.CobrancaRepository;
import org.morah.morah.unidade.repositorio.UnidadeRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Resumo de inadimplencia do condominio (GET /financeiro/inadimplencia e dashboard do sindico).
 *
 * <p>Inadimplente = unidade com pelo menos uma cobranca ATRASADA (pendente e vencida antes de
 * hoje). Uma unidade com tres boletos atrasados conta uma vez so.
 */
@Service
@RequiredArgsConstructor
public class InadimplenciaService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final CobrancaRepository cobrancaRepository;
    private final UnidadeRepository unidadeRepository;
    private final CalculadoraDeEncargos calculadoraDeEncargos;

    /**
     * Metodo publico para o DASHBOARD do sindico (e para o endpoint). Recebe o condominio por
     * parametro - nao le o token.
     */
    public InadimplenciaResumoResponse resumo(Long condominioId) {
        LocalDate hoje = Datas.hoje();

        long totalUnidades = unidadeRepository.countByCondominioId(condominioId);
        List<Cobranca> atrasadas = cobrancaRepository
                .findByCondominioIdAndStatusAndVencimentoBefore(condominioId, StatusCobranca.PENDENTE, hoje);

        long unidadesInadimplentes = atrasadas.stream().map(Cobranca::getUnidadeId).distinct().count();

        BigDecimal percentual = totalUnidades == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(unidadesInadimplentes).multiply(CEM)
                        .divide(BigDecimal.valueOf(totalUnidades), 2, RoundingMode.HALF_UP);

        // Valor em aberto COM multa e juros de hoje: e o que o condominio tem a receber.
        BigDecimal valorEmAberto = atrasadas.stream()
                .map(cobranca -> calculadoraDeEncargos.situacao(cobranca, hoje).valorTotal())
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        return new InadimplenciaResumoResponse(totalUnidades, unidadesInadimplentes, percentual, valorEmAberto);
    }
}
