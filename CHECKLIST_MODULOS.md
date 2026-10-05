# Checklist de módulos e padrões de projeto

Mapeamento do código (`src/main/java/org/morah/morah`) contra o contrato `Documentos/morah-api.yaml`.
Legenda: ✅ implementado · 🟡 parcial · ⬜ não iniciado

---

## 1. Módulos do contrato

| Status | Módulo | Pacote | Endpoints | Observações |
|:-:|--------|--------|-----------|-------------|
| ✅ | Login / Autenticação | `login`, `seguranca` | `/auth/login`, `/auth/me`, `/auth/contexto`, `/auth/refresh`, `/auth/logout` | JWT próprio, contexto (condomínio + perfil) no token, logout revoga token |
| ✅ | Usuários | `usuario` | `/usuarios`, `/usuarios/{id}`, `/usuarios/{id}/desativar` | Apoio ao login (fora do contrato) |
| ✅ | Avisos | `aviso` | `/avisos` (CRUD), `/avisos/exportar` | Público alvo (condomínio/bloco/unidade), vigência (410), "lido" por usuário, notificação do público |
| ✅ | Dashboard | `dashboard` | `/home/dashboard` | Morador: unidade, boletos, avisos, encomendas, autorizações, reservas. Síndico: inadimplência, avisos, solicitações, ocorrências. Portaria: fila de autorizações, reservas do dia, encomendas de hoje |
| ✅ | Notificações | `notificacao` | `/notificacoes`, `/notificacoes/{id}/lida` | 14 tipos de notificação; canais e-mail/SMS/push simulados por log |
| ✅ | Financeiro | `financeiro` | `/financeiro/cobrancas`, `/pix`, `/baixa-manual`, `/webhooks/pagamentos`, `/documentos`, `/taxas`, `/gerar-cobrancas`, `/inadimplencia` | Atraso, multa e juros calculados na leitura; webhook com HMAC; PIX simulado |
| ✅ | Portaria | `portaria` | `/portaria/visitantes`, `/autorizacoes`, `/decisao`, `/reenviar`, `/acessos` | Autorização expira em 30 min; entrada exige autorização |
| ✅ | Áreas comuns e Reservas | `reserva` | `/areas-comuns`, `/disponibilidade`, `/reservas`, `/vistoria` | Conflito de horário → 409; vistoria com dano abre ocorrência |
| ✅ | Minha Unidade | `unidade` | `/unidades/minha`, `/vinculos`, `/solicitacoes`, `/inquilinos` | Inquilino novo ganha usuário e convite; encerrar retira o acesso |
| ✅ | Encomendas | `encomenda` | `/encomendas`, `/autorizacoes-retirada`, `/retirada`, `/ocorrencias` | Código de retirada oculto para portaria/síndico |
| ✅ | Ocorrências (apoio) | `ocorrencia` | — (usado por Encomendas, Reservas e Dashboard) | Notifica os síndicos |
| ✅ | Comum / Infra | `comum`, `config` | `/monitor/metricas` | Erros RFC 9457, paginação, ids sequenciais, datas em UTC, enums na URL, CORS, Swagger, carga inicial |

### Checklist transversal

- [x] Arquitetura em pacotes por módulo (`modelo / repositorio / dto / servico / controle`)
- [x] Segurança JWT stateless + `@PreAuthorize` por perfil
- [x] Isolamento por condomínio (outro condomínio → 404) e por unidade (outra unidade → 403)
- [x] Tratamento global de erros (`application/problem+json`), inclusive JSON mal formado e parâmetro inválido (400)
- [x] Paginação padronizada (`PaginaResponse`)
- [x] Ids numéricos sequenciais (contrato exige `int64`)
- [x] Datas gravadas em UTC e valores em Decimal128 (`ConfiguracaoMongo`)
- [x] Documentação Swagger / OpenAPI
- [x] Carga inicial de dados de teste (base + um runner por módulo)
- [x] Pipeline de deploy (`.github/workflows/azure-webapp.yml`)
- [x] Testes de todos os módulos (309 testes, sem precisar de MongoDB), incluindo conferência automática das 50 operações do contrato
- [x] Controle de "aviso lido" por usuário
- [ ] Revogação de tokens compartilhada entre instâncias (hoje em memória → Redis/Mongo)
- [ ] Integrações reais: gateway de pagamento/PIX, e-mail, push e SMS
- [ ] Rotinas agendadas (reserva "não compareceu", fim de contrato de inquilino)
- [ ] Link de definição de senha no convite do inquilino (hoje a senha temporária vai no e-mail)
- [ ] Teste de ponta a ponta contra um MongoDB real

---

## 2. Padrões de projeto aplicados

### Singleton — 2 aplicações

| Classe | Variação | Função |
|--------|----------|--------|
| `comum/singleton/ContadorDeRequisicoesSingleton` | Eager (`static final`) | Contagem de requisições por rota; alimentada por `InterceptadorDeMetricas`, lida em `/monitor/metricas` |
| `seguranca/singleton/RegistroDeTokensRevogadosSingleton` | Lazy (`getInstancia()` sincronizado) | Tokens cancelados no logout; consultado por `FiltroAutenticacaoJwt` |

### Template Method — 4 aplicações

| Classe abstrata (esqueleto) | Filhas | Passos fixos |
|-----------------------------|--------|--------------|
| `comum/servico/ServicoCrudTemplate` | `UsuarioService`, `AvisoService`, `EncomendaService`, `ReservaService`, `SolicitacaoVinculoService` | validar → converter → antesDeSalvar → salvar → depoisDeSalvar → responder |
| `login/template/AutenticacaoTemplate` | `AutenticacaoPorSenha`, `AutenticacaoPorRefreshToken` | conferir usuário ativo → escolher contexto → emitir tokens |
| `notificacao/template/NotificacaoTemplate` | 14 notificações (uma por evento dos módulos) | montar → gravar → despachar |
| `unidade/template/ConcessaoDeAcessoTemplate` | `ConcessaoPorSolicitacaoAprovada`, `ConcessaoPorCadastroDeInquilino` | montar vínculo → conferir duplicidade → localizar pessoa → gravar → liberar perfil → notificar |

### Strategy — 6 aplicações

| Interface | Implementações | Seletor (escolhe por) |
|-----------|----------------|-----------------------|
| `dashboard/strategy/DashboardStrategy` | Morador, Síndico, Portaria | `SeletorDeDashboard` (perfil do token) |
| `notificacao/strategy/CanalDeEnvioStrategy` | E-mail, Push, SMS | `SeletorDeCanal` (canal da notificação) |
| `aviso/strategy/ExportadorDeAvisosStrategy` | CSV, JSON | `SeletorDeExportador` (`?formato=`) |
| `financeiro/strategy/RateioStrategy` | Igualitário, Fração ideal | `SeletorDeRateio` (`tipoRateio` da taxa) |
| `reserva/strategy/RegraDeReservaStrategy` | Área disponível, Período válido, Início no futuro, Horário de funcionamento, Conflito | Todas aplicadas em ordem (`@Order`) |
| `encomenda/strategy/ValidacaoDeRetiradaStrategy` | Código do morador, Autorização de terceiro | A primeira que aceitar o código |

### State — 1 aplicação

| Interface | Estados | Seletor |
|-----------|---------|---------|
| `portaria/state/EstadoDaAutorizacao` | Pendente, Autorizada, Recusada, Expirada | `SeletorDeEstadoDaAutorizacao` (status gravado) |

### Outros padrões presentes

| Padrão | Onde | Observação |
|--------|------|------------|
| Observer (listener de evento) | `comum/sequencia/OuvinteDeIdSequencial` | Escuta o evento de persistência do Mongo para atribuir o id sequencial |
| Chain of Responsibility (filtro) | `seguranca/jwt/FiltroAutenticacaoJwt`, `comum/web/InterceptadorDeMetricas` | Cadeia de filtros/interceptadores do Spring |
| DTO | pacotes `dto` de cada módulo | `record`s; nunca expõem senha/hash |
| Repository | pacotes `repositorio` | Spring Data (`MongoRepository`) |
| Dependency Injection | `@RequiredArgsConstructor` + seletores | Spring injeta a lista de strategies/estados |
| Front Controller / REST Controller | pacotes `controle` | Controllers sem regra de negócio |
| Global Exception Handler | `comum/erro/TratadorGlobalDeErros` | `@RestControllerAdvice` com `ProblemDetail` |

### Cobertura de testes por padrão

| Padrão | Teste |
|--------|-------|
| Singleton 1 e 2 | `ContadorDeRequisicoesSingletonTest`, `RegistroDeTokensRevogadosSingletonTest` ✅ |
| Strategy 1 (dashboard) | `SeletorDeDashboardTest`, `DashboardStrategiesTest` ✅ |
| Strategy 2 (canal de notificação) | `SeletorDeCanalTest` ✅ |
| Strategy 3 (exportador) | `SeletorDeExportadorTest` ✅ |
| Strategy 4 (rateio) | `RateioStrategyTest` ✅ |
| Strategy 5 (regras de reserva) | `RegrasDeReservaTest` ✅ |
| Strategy 6 (retirada) | `ValidacaoDeRetiradaStrategyTest` ✅ |
| State 1 (autorização) | `EstadosDaAutorizacaoTest` ✅ |
| Template Method 1 (CRUD) | `AvisoServiceTest`, `EncomendaServiceTest`, `ReservaServiceTest`, `SolicitacaoVinculoServiceTest` ✅ |
| Template Method 2 (autenticação) | `LoginFluxoTest` ✅ |
| Template Method 3 (notificação) | `NotificacoesDaPortariaTest` e asserts nos testes de serviço ✅ |
| Template Method 4 (concessão de acesso) | `ConcessaoPorSolicitacaoAprovadaTest`, `ConcessaoPorCadastroDeInquilinoTest` ✅ |
