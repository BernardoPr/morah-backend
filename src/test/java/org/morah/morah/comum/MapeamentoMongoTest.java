package org.morah.morah.comum;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.bson.BsonDocumentReader;
import org.bson.RawBsonDocument;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mongodb.MongoClientSettings;

/**
 * Nao temos MongoDB nos testes, mas da para conferir o MAPEAMENTO sem ele: cada classe
 * {@code @Document} do projeto e preenchida com valores de exemplo, convertida pelo mesmo
 * conversor que a aplicacao usa, codificada em BSON pelo driver (como se fosse para o banco),
 * lida de volta e comparada com a original.
 *
 * <p>Pega erros que so apareceriam rodando contra o banco: tipo sem codec, campo que nao volta,
 * data que muda de dia por causa de fuso, {@code BigDecimal} gravado como texto...
 */
@SpringBootTest(properties = {
        "morah.carga-inicial=false",
        "spring.data.mongodb.auto-index-creation=false"
})
class MapeamentoMongoTest {

    private static final CodecRegistry CODECS = MongoClientSettings.getDefaultCodecRegistry();
    private static final Codec<org.bson.Document> CODEC_DE_DOCUMENTO = CODECS.get(org.bson.Document.class);

    @Autowired
    private MongoConverter conversor;

    static Stream<Class<?>> entidades() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Document.class));
        return scanner.findCandidateComponents("org.morah.morah").stream()
                .map(definicao -> {
                    try {
                        return Class.forName(definicao.getBeanClassName());
                    } catch (ClassNotFoundException excecao) {
                        throw new IllegalStateException(excecao);
                    }
                });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entidades")
    @DisplayName("a entidade vai para o BSON e volta igual")
    void idaEVolta(Class<?> tipo) throws Exception {
        Object original = preencher(tipo, 0);

        org.bson.Document gravado = new org.bson.Document();
        conversor.write(original, gravado);

        RawBsonDocument bson = new RawBsonDocument(gravado, CODEC_DE_DOCUMENTO);
        org.bson.Document lido = CODEC_DE_DOCUMENTO.decode(
                new BsonDocumentReader(bson.toBsonDocument()), DecoderContext.builder().build());

        Object devolta = conversor.read(tipo, lido);

        assertThat(devolta).usingRecursiveComparison().isEqualTo(original);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entidades")
    @DisplayName("valores BigDecimal sao gravados como Decimal128, e nao como texto")
    void dinheiroEmDecimal128(Class<?> tipo) throws Exception {
        org.bson.Document gravado = new org.bson.Document();
        conversor.write(preencher(tipo, 0), gravado);

        List<Object> valores = new ArrayList<>();
        juntarValores(gravado, valores);

        assertThat(valores)
                .as("BigDecimal de %s gravado como texto", tipo.getSimpleName())
                .doesNotContain("1234.56");
        assertThat(valores)
                .as("BigDecimal de %s sem converter para Decimal128", tipo.getSimpleName())
                .noneMatch(BigDecimal.class::isInstance);
    }

    /** Percorre o documento inteiro, inclusive objetos embutidos e listas. */
    private void juntarValores(Object valor, List<Object> destino) {
        if (valor instanceof org.bson.Document documento) {
            documento.values().forEach(filho -> juntarValores(filho, destino));
        } else if (valor instanceof Collection<?> colecao) {
            colecao.forEach(filho -> juntarValores(filho, destino));
        } else {
            destino.add(valor);
        }
    }

    // ---------- valores de exemplo ----------

    private Object preencher(Class<?> tipo, int profundidade) throws Exception {
        if (tipo.isRecord()) {
            RecordComponent[] componentes = tipo.getRecordComponents();
            Object[] valores = new Object[componentes.length];
            Class<?>[] tipos = new Class<?>[componentes.length];
            for (int i = 0; i < componentes.length; i++) {
                tipos[i] = componentes[i].getType();
                valores[i] = amostra(tipos[i], componentes[i].getGenericType(), profundidade);
            }
            Constructor<?> construtor = tipo.getDeclaredConstructor(tipos);
            construtor.setAccessible(true);
            return construtor.newInstance(valores);
        }

        Constructor<?> construtor = tipo.getDeclaredConstructor();
        construtor.setAccessible(true);
        Object objeto = construtor.newInstance();

        for (Class<?> classe = tipo; classe != Object.class; classe = classe.getSuperclass()) {
            for (Field campo : classe.getDeclaredFields()) {
                if (Modifier.isStatic(campo.getModifiers()) || Modifier.isFinal(campo.getModifiers())
                        || campo.isSynthetic() || campo.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                Object valor = amostra(campo.getType(), campo.getGenericType(), profundidade);
                if (valor != null) {
                    campo.setAccessible(true);
                    campo.set(objeto, valor);
                }
            }
        }
        return objeto;
    }

    private Object amostra(Class<?> tipo, Type generico, int profundidade) throws Exception {
        if (tipo == Long.class || tipo == long.class) {
            return 7L;
        }
        if (tipo == Integer.class || tipo == int.class) {
            return 3;
        }
        if (tipo == Boolean.class || tipo == boolean.class) {
            return true;
        }
        if (tipo == Double.class || tipo == double.class) {
            return 1.5;
        }
        if (tipo == String.class) {
            return "texto";
        }
        if (tipo == BigDecimal.class) {
            return new BigDecimal("1234.56");
        }
        if (tipo == Instant.class) {
            return Instant.parse("2026-10-04T12:34:56.789Z"); // BSON guarda milissegundos
        }
        if (tipo == LocalDate.class) {
            return LocalDate.of(2026, 10, 10);
        }
        if (tipo == LocalTime.class) {
            return LocalTime.of(10, 30);
        }
        if (tipo == LocalDateTime.class) {
            return LocalDateTime.of(2026, 10, 10, 10, 30);
        }
        if (tipo.isEnum()) {
            return tipo.getEnumConstants()[0];
        }
        if (Collection.class.isAssignableFrom(tipo) && generico instanceof ParameterizedType parametrizado
                && parametrizado.getActualTypeArguments()[0] instanceof Class<?> tipoDoItem) {
            Object item = amostra(tipoDoItem, tipoDoItem, profundidade);
            Collection<Object> colecao = Set.class.isAssignableFrom(tipo) ? new HashSet<>() : new ArrayList<>();
            if (item != null) {
                colecao.add(item);
            }
            return colecao;
        }
        if (tipo.getPackageName().startsWith("org.morah.morah") && profundidade < 3) {
            return preencher(tipo, profundidade + 1);
        }
        return null;
    }
}
