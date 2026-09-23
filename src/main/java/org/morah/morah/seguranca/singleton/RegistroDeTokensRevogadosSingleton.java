package org.morah.morah.seguranca.singleton;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PADRAO DE PROJETO: SINGLETON (exemplo 2 de 2) - versao "sob demanda" (lazy).
 *
 * <p><b>Problema:</b> um JWT continua valido ate expirar, mesmo depois do logout.
 * Precisamos de uma lista de tokens cancelados compartilhada por toda a aplicacao
 * (o filtro de seguranca consulta, o logout escreve).
 *
 * <p><b>Solucao:</b> construtor privado + {@link #getInstancia()} {@code synchronized}, que
 * cria a instancia apenas na primeira chamada. Compare com o
 * {@code ContadorDeRequisicoesSingleton}, que usa a versao adiantada (eager): as duas formas
 * garantem instancia unica, mudando so o momento da criacao.
 *
 * <p><b>Quem usa:</b> {@code LoginService} (logout) e {@code FiltroAutenticacaoJwt} (leitura).
 *
 * <p><b>Limite:</b> a lista vive em memoria. Serve para uma instancia (Azure Web App gratuito).
 * Em varias instancias, troque por Redis ou por uma colecao no Mongo, mantendo esta mesma
 * interface de metodos.
 */
public final class RegistroDeTokensRevogadosSingleton {

    private static RegistroDeTokensRevogadosSingleton instancia;

    private final Set<String> revogados = ConcurrentHashMap.newKeySet();

    private RegistroDeTokensRevogadosSingleton() {
    }

    public static synchronized RegistroDeTokensRevogadosSingleton getInstancia() {
        if (instancia == null) {
            instancia = new RegistroDeTokensRevogadosSingleton();
        }
        return instancia;
    }

    /** @param idDoToken claim "jti" do JWT (identificador unico do token). */
    public void revogar(String idDoToken) {
        revogados.add(idDoToken);
    }

    public boolean estaRevogado(String idDoToken) {
        return revogados.contains(idDoToken);
    }

    public int getQuantidade() {
        return revogados.size();
    }
}
