package org.morah.morah.unidade.template;

import org.morah.morah.comum.tempo.Datas;
import org.morah.morah.notificacao.template.NotificacaoDeDecisaoDeVinculo;
import org.morah.morah.unidade.modelo.SolicitacaoVinculo;
import org.morah.morah.unidade.modelo.Unidade;
import org.morah.morah.unidade.modelo.VinculoUnidade;
import org.morah.morah.unidade.repositorio.VinculoUnidadeRepository;
import org.morah.morah.usuario.repositorio.UsuarioRepository;
import org.springframework.stereotype.Component;

/**
 * Concessao de acesso quando o sindico APROVA uma solicitacao de vinculo
 * (PATCH /unidades/{unidadeId}/vinculos/solicitacoes/{id} com "decisao": "aprovada").
 *
 * <p>Preenche apenas os passos variaveis do {@link ConcessaoDeAcessoTemplate}:
 * <ul>
 *   <li>o vinculo nasce dos dados da solicitacao, com inicio hoje e sem data de fim;</li>
 *   <li>a pessoa e procurada pelo CPF (hook padrao): se ja tem usuario, ganha acesso ao app;
 *       se nao tem, entra so no cadastro da unidade - nao criamos usuario aqui porque a
 *       solicitacao nao traz e-mail para enviar o convite;</li>
 *   <li>quem e avisado e o morador que fez o pedido.</li>
 * </ul>
 */
@Component
public class ConcessaoPorSolicitacaoAprovada extends ConcessaoDeAcessoTemplate<SolicitacaoVinculo> {

    private final NotificacaoDeDecisaoDeVinculo notificacaoDeDecisao;

    public ConcessaoPorSolicitacaoAprovada(VinculoUnidadeRepository vinculoUnidadeRepository,
                                           UsuarioRepository usuarioRepository,
                                           NotificacaoDeDecisaoDeVinculo notificacaoDeDecisao) {
        super(vinculoUnidadeRepository, usuarioRepository);
        this.notificacaoDeDecisao = notificacaoDeDecisao;
    }

    @Override
    protected VinculoUnidade montarVinculo(Unidade unidade, SolicitacaoVinculo solicitacao) {
        VinculoUnidade vinculo = new VinculoUnidade();
        vinculo.setCondominioId(unidade.getCondominioId());
        vinculo.setUnidadeId(unidade.getId());
        vinculo.setPessoaNome(solicitacao.getNomeSolicitado());
        vinculo.setPessoaCpf(solicitacao.getCpf()); // ja gravado so com digitos
        vinculo.setTipoVinculo(solicitacao.getTipoVinculo());
        vinculo.setPrincipal(false);
        vinculo.setInicio(Datas.hoje());
        vinculo.setAtivo(true);
        return vinculo;
    }

    @Override
    protected void notificar(DestinoDoAcesso destino, PessoaLocalizada pessoa,
                             VinculoUnidade vinculo, SolicitacaoVinculo solicitacao) {
        if (solicitacao.getSolicitanteId() == null) {
            return;
        }
        usuarioRepository.findById(solicitacao.getSolicitanteId())
                .ifPresent(solicitante -> notificacaoDeDecisao.enviar(solicitante, solicitacao));
    }
}
