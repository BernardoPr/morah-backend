package org.morah.morah.comum.singleton;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PADRAO DE PROJETO: SINGLETON (exemplo 1 de 2) - versao "inicializacao adiantada" (eager).
 *
 * <p><b>Problema:</b> queremos um unico placar de requisicoes para a aplicacao inteira.
 * Se cada classe criasse o seu proprio contador, cada uma teria um numero diferente.
 *
 * <p><b>Solucao:</b> construtor privado (ninguem consegue dar {@code new}) + uma unica
 * instancia guardada em um campo {@code static final}, entregue por {@link #getInstancia()}.
 * Como o campo e inicializado na carga da classe, a criacao ja e segura entre threads.
 *
 * <p><b>Quem usa:</b> {@code InterceptadorDeMetricas} (conta) e {@code MonitorController} (le).
 *
 * <p><b>Observacao importante:</b> no Spring, um {@code @Component}/{@code @Service} ja e
 * singleton dentro do contexto. Usamos a forma classica do GoF aqui para o exemplo ficar
 * explicito. O estado fica em memoria: reiniciou a aplicacao, zerou.
 */
public final class ContadorDeRequisicoesSingleton {

    private static final ContadorDeRequisicoesSingleton INSTANCIA = new ContadorDeRequisicoesSingleton();

    private final Map<String, AtomicLong> porRota = new ConcurrentHashMap<>();
    private final AtomicLong total = new AtomicLong();
    private final Instant iniciadoEm = Instant.now();

    /** Privado: impede {@code new ContadorDeRequisicoesSingleton()} fora da classe. */
    private ContadorDeRequisicoesSingleton() {
    }

    public static ContadorDeRequisicoesSingleton getInstancia() {
        return INSTANCIA;
    }

    public void registrar(String metodo, String rota) {
        porRota.computeIfAbsent(metodo + " " + rota, chave -> new AtomicLong()).incrementAndGet();
        total.incrementAndGet();
    }

    public long getTotal() {
        return total.get();
    }

    public Duration getTempoDeAtividade() {
        return Duration.between(iniciadoEm, Instant.now());
    }

    /** Copia ordenada do placar (o mapa interno nao e exposto). */
    public Map<String, Long> getTotaisPorRota() {
        Map<String, Long> copia = new TreeMap<>();
        porRota.forEach((rota, contador) -> copia.put(rota, contador.get()));
        return copia;
    }

    public void zerar() {
        porRota.clear();
        total.set(0);
    }
}
