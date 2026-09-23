# Arquitetura e padrões de projeto

Documento de apoio para quem vai programar neste repositório.

---

## 1. Organização das pastas

O código é separado **por módulo** (e não por tipo de classe). Tudo que fala de avisos está
dentro de `aviso`, tudo que fala de login está dentro de `login`, e assim por diante:

```
org.morah.morah
├── MorahApplication.java      → classe que inicia a aplicação
├── config/                    → segurança, CORS, Mongo, Swagger, carga inicial
├── comum/                     → o que é reaproveitado por todos os módulos
│   ├── modelo/                → EntidadeBase, Perfil
│   ├── dto/                   → PaginaResponse, PaginaMetadata, PessoaResumo
│   ├── erro/                  → exceções + tratador global (RFC 9457)
│   ├── servico/               → ServicoCrudTemplate  ......... TEMPLATE METHOD 1
│   ├── sequencia/             → ids numéricos automáticos (1, 2, 3...)
│   ├── singleton/             → ContadorDeRequisicoesSingleton  SINGLETON 1
│   └── web/                   → interceptador de métricas + /monitor
├── seguranca/
│   ├── jwt/                   → geração/leitura do token e filtro de autenticação
│   └── singleton/             → RegistroDeTokensRevogadosSingleton  SINGLETON 2
├── usuario/                   → modelo de banco de exemplo (colecao "usuarios")
├── login/                     → /auth/* e os templates de autenticação  TEMPLATE METHOD 2
├── dashboard/                 → /home/dashboard  ................... STRATEGY 1
├── notificacao/               → notificações  TEMPLATE METHOD 3 + STRATEGY 2
└── aviso/                     → módulo CRUD de exemplo  ............ STRATEGY 3
```

Dentro de cada módulo a divisão é sempre a mesma:

| Pasta | Responsabilidade | Regra de ouro |
|-------|------------------|---------------|
| `modelo` | Como o dado é gravado no MongoDB | Não conhece HTTP |
| `repositorio` | Consultas ao banco (Spring Data) | Só interfaces |
| `dto` | O que entra e o que sai da API | `record`, nunca expõe senha/hash |
| `servico` | Regra de negócio | Não conhece HTTP |
| `controle` | Endpoints REST | Sem regra de negócio: recebe, delega, devolve |

---

## 2. Os padrões de projeto aplicados

### Singleton (2 exemplos)

| # | Classe | Variação | Para que serve |
|---|--------|----------|----------------|
| 1 | `comum/singleton/ContadorDeRequisicoesSingleton.java` | adiantada (*eager*): campo `static final` | Placar único de requisições por rota, alimentado pelo `InterceptadorDeMetricas` e lido em `GET /monitor/metricas` |
| 2 | `seguranca/singleton/RegistroDeTokensRevogadosSingleton.java` | sob demanda (*lazy*): `getInstancia()` `synchronized` | Lista de tokens cancelados no logout, consultada pelo filtro de segurança |

Os dois têm construtor privado — ninguém consegue dar `new` — e entregam sempre o mesmo objeto.

> No Spring, um `@Component` já é singleton dentro do contexto. Aqui usamos a forma clássica do
> GoF de propósito, para o padrão ficar visível no código.

### Template Method (3 exemplos)

| # | Classe abstrata | Filhas | O que o esqueleto garante |
|---|-----------------|--------|---------------------------|
| 1 | `comum/servico/ServicoCrudTemplate.java` | `UsuarioService`, `AvisoService` | Todo CRUD segue validar → converter → salvar → responder |
| 2 | `login/template/AutenticacaoTemplate.java` | `AutenticacaoPorSenha`, `AutenticacaoPorRefreshToken` | Toda autenticação confere o usuário ativo, escolhe o contexto e devolve os tokens |
| 3 | `notificacao/template/NotificacaoTemplate.java` | `NotificacaoDeBoasVindas`, `NotificacaoDeAvisoPublicado` | Toda notificação é montada, gravada e despachada na mesma ordem |

Em todos, o método principal é `final` (a ordem dos passos não muda), os passos obrigatórios são
`abstract` e os opcionais são *hooks* com corpo vazio.

### Strategy (3 exemplos)

| # | Interface | Implementações | Quem escolhe |
|---|-----------|----------------|--------------|
| 1 | `dashboard/strategy/DashboardStrategy.java` | morador (+proprietário), síndico, portaria | `SeletorDeDashboard`, pelo perfil do token |
| 2 | `notificacao/strategy/CanalDeEnvioStrategy.java` | e-mail, push, SMS | `SeletorDeCanal`, pelo canal escolhido na notificação |
| 3 | `aviso/strategy/ExportadorDeAvisosStrategy.java` | CSV, JSON | `SeletorDeExportador`, pelo `?formato=` da requisição |

Os três seletores usam o mesmo truque: o Spring injeta a **lista** com todas as implementações da
interface e o seletor escolhe a certa. Criar uma variação nova = criar uma classe nova; nenhum
código existente precisa mudar.

> **Template Method x Strategy:** o módulo de notificação usa os dois e é o melhor lugar para ver
> a diferença. O Template Method (herança) define *a ordem dos passos*; o Strategy (composição)
> troca *como* um passo é executado.

---

## 3. Como criar um módulo novo (ex.: encomendas)

Copie o módulo `aviso` e siga esta ordem:

1. **Modelo** — `encomenda/modelo/Encomenda.java`, estendendo `EntidadeBase` e anotado com
   `@Document(collection = "encomendas")`. O id numérico é gerado sozinho.
2. **Repositório** — `encomenda/repositorio/EncomendaRepository.java` estendendo
   `MongoRepository<Encomenda, Long>`. Consultas simples saem do nome do método
   (`findByStatus`, `findByUnidadeId`...).
3. **DTOs** — um `record` de entrada (com `@NotBlank`/`@NotNull`) e um de saída, copiando os
   nomes dos campos do `Documentos/morah-api.yaml`.
4. **Service** — estenda `ServicoCrudTemplate<Encomenda, EncomendaCreateRequest, EncomendaResponse>`
   e escreva só o que é específico. Regras extras vão nos hooks (`validarCriacao`,
   `antesDeSalvar`, `depoisDeSalvar`).
5. **Controller** — `@RestController` com `@RequestMapping("/encomendas")`, `@Valid` no corpo e
   `@PreAuthorize("hasRole('PORTARIA')")` quando o contrato restringir o perfil.

O que você **não** precisa escrever de novo: tratamento de erro, paginação, autenticação,
geração de id e documentação no Swagger — tudo isso vem da base.

---

## 4. Decisões técnicas (e o porquê)

| Decisão | Motivo |
|---------|--------|
| Ids numéricos sequenciais em vez do `ObjectId` do Mongo | O contrato com o front declara todos os ids como `integer/int64` |
| Login com JWT próprio em vez de Keycloak | O plano gratuito do Azure não comporta um servidor de autenticação separado |
| API sem sessão (*stateless*) | Cada chamada carrega o token; facilita rodar em qualquer hospedagem |
| Contexto (condomínio + perfil) dentro do token | Nenhum endpoint precisa receber `condominioId` na URL, como decidido no contrato |
| Erros no formato RFC 9457 (`application/problem+json`) | Exigência do contrato; o Spring já tem suporte nativo (`ProblemDetail`) |
| `virtual threads` ligadas | Mais requisições simultâneas com a pouca memória do plano gratuito |
| Lista de tokens revogados em memória | Suficiente para uma instância; com várias instâncias, troque por Redis ou por uma coleção no Mongo |

---

## 5. O que ainda não foi implementado

Estes módulos do contrato têm apenas o formato de resposta previsto (listas vazias no dashboard),
e devem ser criados seguindo o passo a passo da seção 3:

- Financeiro (cobranças, PIX, documentos, inadimplência)
- Portaria (visitantes, autorizações, acessos)
- Reservas de áreas comuns e vistorias
- Minha Unidade (vínculos, solicitações, inquilinos)
- Encomendas (recebimento, autorização de retirada, ocorrências)
- Controle de "aviso lido" por usuário
