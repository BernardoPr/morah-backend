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
│   ├── servico/               → ServicoCrudTemplate  ................. TEMPLATE METHOD 1
│   ├── sequencia/             → ids numéricos automáticos (1, 2, 3...)
│   ├── singleton/             → ContadorDeRequisicoesSingleton  ...... SINGLETON 1
│   ├── tempo/                 → Datas ("hoje" no fuso de São Paulo)
│   └── web/                   → métricas, /monitor, conversor de enums da URL
├── seguranca/
│   ├── jwt/                   → geração/leitura do token e filtro de autenticação
│   └── singleton/             → RegistroDeTokensRevogadosSingleton  .. SINGLETON 2
├── usuario/                   → cadastro de usuários (coleção "usuarios")
├── login/                     → /auth/* e os templates de autenticação  TEMPLATE METHOD 2
├── dashboard/                 → /home/dashboard  ......................... STRATEGY 1
├── notificacao/               → notificações  ......... TEMPLATE METHOD 3 + STRATEGY 2
├── aviso/                     → mural de avisos  ......................... STRATEGY 3
├── financeiro/                → cobranças, PIX, taxas, inadimplência  .... STRATEGY 4
├── reserva/                   → áreas comuns, reservas e vistorias  ...... STRATEGY 5
├── encomenda/                 → recebimento, retirada e extravio  ........ STRATEGY 6
├── portaria/                  → visitantes, autorizações e acessos  ...... STATE 1
├── unidade/                   → minha unidade, vínculos e inquilinos  .... TEMPLATE METHOD 4
└── ocorrencia/                → ocorrências abertas por encomendas e reservas
```

Dentro de cada módulo a divisão é sempre a mesma:

| Pasta | Responsabilidade | Regra de ouro |
|-------|------------------|---------------|
| `modelo` | Como o dado é gravado no MongoDB | Não conhece HTTP |
| `repositorio` | Consultas ao banco (Spring Data) | Só interfaces |
| `dto` | O que entra e o que sai da API | `record`, nunca expõe senha/hash |
| `servico` | Regra de negócio | Não conhece HTTP |
| `controle` | Endpoints REST | Sem regra de negócio: recebe, delega, devolve |
| `strategy` / `state` / `template` | Classes de um padrão de projeto | Uma variação = uma classe |
| `config` | Carga inicial de dados do módulo | Só roda com `morah.carga-inicial=true` |

---

## 2. Os padrões de projeto aplicados

Resumo: **2 Singleton, 4 Template Method, 6 Strategy e 1 State**. Cada classe principal de
padrão tem um Javadoc "PADRAO DE PROJETO: ... (exemplo X de N)" explicando o problema, a solução
e quem usa.

### Singleton (2 exemplos)

| # | Classe | Variação | Para que serve |
|---|--------|----------|----------------|
| 1 | `comum/singleton/ContadorDeRequisicoesSingleton` | adiantada (*eager*): campo `static final` | Placar único de requisições por rota, alimentado pelo `InterceptadorDeMetricas` e lido em `GET /monitor/metricas` |
| 2 | `seguranca/singleton/RegistroDeTokensRevogadosSingleton` | sob demanda (*lazy*): `getInstancia()` `synchronized` | Lista de tokens cancelados no logout, consultada pelo filtro de segurança |

Os dois têm construtor privado — ninguém consegue dar `new` — e entregam sempre o mesmo objeto.

> No Spring, um `@Component` já é singleton dentro do contexto. Aqui usamos a forma clássica do
> GoF de propósito, para o padrão ficar visível no código.

### Template Method (4 exemplos)

| # | Classe abstrata | Filhas | O que o esqueleto garante |
|---|-----------------|--------|---------------------------|
| 1 | `comum/servico/ServicoCrudTemplate` | `UsuarioService`, `AvisoService`, `EncomendaService`, `ReservaService`, `SolicitacaoVinculoService` | Toda criação segue validar → converter → antesDeSalvar → salvar → depoisDeSalvar |
| 2 | `login/template/AutenticacaoTemplate` | `AutenticacaoPorSenha`, `AutenticacaoPorRefreshToken` | Toda autenticação confere o usuário ativo, escolhe o contexto e devolve os tokens |
| 3 | `notificacao/template/NotificacaoTemplate` | 14 notificações (boas-vindas, aviso, encomenda recebida/retirada, visitante aguardando, decisão de visita, reserva confirmada, dano em reserva, cobrança gerada, pagamento confirmado, vínculo, convite de inquilino, ocorrência...) | Toda notificação é montada, gravada e despachada na mesma ordem |
| 4 | `unidade/template/ConcessaoDeAcessoTemplate` | `ConcessaoPorSolicitacaoAprovada`, `ConcessaoPorCadastroDeInquilino` | Quem ganha acesso a uma unidade sempre recebe o vínculo da unidade **e** o vínculo de perfil do login, e é notificado — nenhum dos fluxos esquece um passo |

Em todos, o método principal é `final` (a ordem dos passos não muda), os passos obrigatórios são
`abstract` e os opcionais são *hooks* com corpo vazio.

### Strategy (6 exemplos)

| # | Interface | Implementações | Quem escolhe |
|---|-----------|----------------|--------------|
| 1 | `dashboard/strategy/DashboardStrategy` | morador (+proprietário), síndico, portaria | `SeletorDeDashboard`, pelo perfil do token |
| 2 | `notificacao/strategy/CanalDeEnvioStrategy` | e-mail, push, SMS | `SeletorDeCanal`, pelo canal escolhido na notificação |
| 3 | `aviso/strategy/ExportadorDeAvisosStrategy` | CSV, JSON | `SeletorDeExportador`, pelo `?formato=` da requisição |
| 4 | `financeiro/strategy/RateioStrategy` | igualitário, por fração ideal | `SeletorDeRateio`, pelo `tipoRateio` da taxa |
| 5 | `reserva/strategy/RegraDeReservaStrategy` | área disponível, período válido, início no futuro, horário de funcionamento, conflito de horário | Todas são aplicadas, na ordem do `@Order` (regra nova = classe nova) |
| 6 | `encomenda/strategy/ValidacaoDeRetiradaStrategy` | código do morador, autorização de terceiro | A primeira que aceitar o código digitado na portaria |

Os seletores usam o mesmo truque: o Spring injeta a **lista** com todas as implementações da
interface e o seletor escolhe a certa. Criar uma variação nova = criar uma classe nova; nenhum
código existente precisa mudar.

### State (1 exemplo)

| # | Interface | Estados | Quem escolhe |
|---|-----------|---------|--------------|
| 1 | `portaria/state/EstadoDaAutorizacao` | pendente, autorizada, recusada, expirada | `SeletorDeEstadoDaAutorizacao`, pelo status gravado na autorização |

Cada estado sabe quais operações aceita (decidir, reenviar, expirar, liberar a entrada) e faz a
transição para o próximo status; as demais respondem 409. O serviço nunca testa o status com `if`.

> **Template Method x Strategy:** o módulo de notificação usa os dois e é o melhor lugar para ver
> a diferença. O Template Method (herança) define *a ordem dos passos*; o Strategy (composição)
> troca *como* um passo é executado.
>
> **Strategy x State:** a estrutura é parecida (interface + implementações + seletor). Na Strategy
> quem escolhe o algoritmo é o cliente (perfil do token, `?formato=`); no State é o *estado interno
> do objeto*, e o próprio estado provoca a troca para o próximo.

---

## 3. Como criar um módulo novo (ex.: assembleias)

Copie um módulo pequeno (`aviso` ou `encomenda`) e siga esta ordem:

1. **Modelo** — `assembleia/modelo/Assembleia.java`, estendendo `EntidadeBase`, anotado com
   `@Document(collection = "assembleias")` e com `condominioId` (`@Indexed`). O id numérico é
   gerado sozinho.
2. **Repositório** — `assembleia/repositorio/AssembleiaRepository.java` estendendo
   `MongoRepository<Assembleia, Long>`. Consultas simples saem do nome do método
   (`findByCondominioIdAndStatus`...); filtros opcionais combinados ficam melhor com `Criteria`
   (veja `AvisoService.listarVisiveis`).
3. **DTOs** — um `record` de entrada (com `@NotBlank`/`@NotNull`) e um de saída, copiando os
   nomes dos campos do `Documentos/morah-api.yaml`.
4. **Service** — estenda `ServicoCrudTemplate<Assembleia, AssembleiaCreateRequest, AssembleiaResponse>`
   e escreva só o que é específico. Regras extras vão nos hooks (`validarCriacao`,
   `antesDeSalvar`, `depoisDeSalvar`). Toda consulta filtra pelo `condominioId` do token.
5. **Controller** — `@RestController` com `@RequestMapping("/assembleias")`, `@Valid` no corpo e
   `@PreAuthorize("hasRole('SINDICO')")` quando o contrato restringir o perfil.

O que você **não** precisa escrever de novo: tratamento de erro, paginação, autenticação,
geração de id, conversão de enums na URL e documentação no Swagger — tudo isso vem da base.

### Regras que todos os módulos seguem

- **Multi-condomínio:** registro de outro condomínio → 404; morador/proprietário mexendo em
  registro de outra unidade do mesmo condomínio → 403.
- **Enums:** a constante Java é o valor do contrato em maiúsculo (`"nao_compareceu"` →
  `NAO_COMPARECEU`). No JSON sai o valor minúsculo (`@JsonValue`); na URL qualquer caixa é aceita.
- **"Hoje"** é sempre calculado com `comum/tempo/Datas` (fuso de São Paulo), nunca com o relógio
  do servidor.
- **Notificar uma unidade:** `usuarioRepository.findByVinculosUnidadeIdAndAtivoTrue(unidadeId)`;
  notificar os síndicos ou a portaria: `usuarioRepository.listarPorPerfilNoCondominio(...)`.

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
| Datas (`LocalDate`) gravadas em UTC pelos codecs do driver | O padrão do Spring Data usa o fuso da máquina: um vencimento gravado pelo Azure (UTC) apareceria um dia antes para quem roda a API no Brasil contra o mesmo Atlas (`ConfiguracaoMongo`) |
| `BigDecimal` gravado como Decimal128 | Valor decimal exato no banco, sem erros de arredondamento com dinheiro |
| Status "atrasado" da cobrança e "expirada" da autorização calculados na leitura | Não há rotina agendada no plano gratuito; o status gravado continua "pendente" e a resposta mostra o status efetivo |
| Código de retirada da encomenda oculto para portaria e síndico | Se o porteiro visse o código, conferir o código não provaria nada; o morador recebe pela notificação |
| Ocorrências em um módulo próprio (`ocorrencia`) | Encomendas (extravio) e Reservas (dano na vistoria) abrem ocorrências, e o dashboard do síndico conta as abertas |
| Unidades da carga inicial com id fixo (101, 102, 201, 202) | Facilita os testes: a unidade 101 é o "Apto 101" |
| Webhook de pagamento público, autenticado por HMAC-SHA256 | É chamado pelo gateway, não pelo app; a assinatura no cabeçalho `X-Webhook-Signature` prova a origem |

---

## 5. Limitações conhecidas (próximos passos)

- **Integrações simuladas:** PIX (código copia-e-cola gerado localmente), gateway de pagamento,
  e-mail, push e SMS (os canais apenas registram no log).
- **Convite de inquilino:** a senha temporária vai no texto do e-mail simulado (aparece no log e
  na coleção `notificacoes`). Em produção, troque por um link de definição de senha.
- **Sem transações no MongoDB:** em operações com mais de uma gravação, uma falha no meio pode
  deixar um registro parcial; duas requisições simultâneas podem, em casos raros, passar pelas
  mesmas checagens (as reservas têm uma proteção extra "quem gravou primeiro vence").
- **Sem rotinas agendadas:** nada marca reserva como "não compareceu" nem encerra o acesso do
  inquilino quando o contrato vence.
- **Tokens já emitidos:** depois de encerrar um inquilino, o access token dele vale até expirar
  (no máximo 1 hora); o refresh já é recusado.
