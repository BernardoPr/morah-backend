# Morah API

API do sistema Morah (gestão de condomínio): **Java 21 + Spring Boot 4 + MongoDB Atlas**.

Todos os módulos do contrato estão implementados — login, tela inicial, financeiro, portaria,
avisos, reservas de áreas comuns, minha unidade, encomendas e notificações — com segurança por
perfil, erros no padrão RFC 9457, paginação e os padrões de projeto da disciplina
(Singleton, Template Method, Strategy e State).

- Contrato com o front: [`Documentos/morah-api.yaml`](Documentos/morah-api.yaml)
- Estrutura de pastas e padrões de projeto: [`ARQUITETURA.md`](ARQUITETURA.md)

---

## 1. Como rodar na sua máquina

Pré-requisitos: **Git** e **Java 21** (se não tiver o 21, o Gradle baixa sozinho na primeira
execução) e um **MongoDB** — pode ser o local ou um cluster gratuito do Atlas.

```bash
./gradlew bootRun
```

No Windows (PowerShell):

```bash
.\gradlew.bat bootRun
```

A API sobe em `http://localhost:8080/v1` e a documentação interativa fica em
`http://localhost:8080/v1/swagger-ui.html`.

### Apontando para o MongoDB

Por padrão a aplicação usa `mongodb://localhost:27017/morah`. Para usar o Atlas, defina a
variável de ambiente antes de subir:

```bash
export MONGODB_URI="mongodb+srv://usuario:senha@cluster0.xxxxx.mongodb.net/morah"
```

### Dados de teste

Na primeira execução, a aplicação cria um condomínio de exemplo ("Residencial Morah") com as
unidades **101, 102** (Bloco A) e **201, 202** (Bloco B) — o id da unidade é o número do
apartamento — e estes usuários (senha `morah1234`):

| CPF           | Nome        | Perfis                                         |
|---------------|-------------|------------------------------------------------|
| `11111111111` | Ana Souza   | morador (Apto 101, inquilina)                  |
| `22222222222` | Carlos Lima | síndico **e** morador (Apto 202)               |
| `33333333333` | Joana Reis  | portaria                                       |
| `44444444444` | Bruno Alves | proprietário (Apto 101, alugado para a Ana)    |

Use o Carlos para testar a troca de contexto (`POST /auth/contexto`).

Cada módulo também cria dados de demonstração, para as telas não ficarem vazias: vínculos das
unidades e uma solicitação de dependente pendente, áreas comuns (salão, churrasqueira, quadra,
academia) e uma reserva da Ana para amanhã, taxas condominiais com um boleto atrasado no 101,
uma prestação de contas e uma encomenda aguardando retirada no 101 (o código de retirada sai no
log da aplicação).

---

## 2. Testando o login

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"cpf":"11111111111","senha":"morah1234"}'
```

Resposta:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600,
  "contexto": {
    "perfil": "morador",
    "condominioId": 1,
    "condominioNome": "Residencial Morah",
    "unidadeId": 101
  }
}
```

Depois é só mandar o `accessToken` em todas as chamadas:

```bash
curl http://localhost:8080/v1/auth/me -H "Authorization: Bearer SEU_TOKEN"
```

No Swagger UI, clique em **Authorize** e cole o token.

### Endpoints

A lista completa, com os corpos de requisição, está no Swagger. Resumo por módulo
(M = morador, P = proprietário, S = síndico, R = portaria):

| Módulo | Rotas | Quem acessa |
|--------|-------|-------------|
| Login | `POST /auth/login`, `POST /auth/refresh` | público |
| | `GET /auth/me`, `POST /auth/contexto`, `POST /auth/logout` | autenticado |
| Tela inicial | `GET /home/dashboard` (muda conforme o perfil) | todos |
| Avisos | `GET /avisos`, `GET /avisos/{id}` (marca como lido; 410 se vencido) | todos |
| | `POST`, `PATCH`, `DELETE /avisos...`, `GET /avisos/exportar?formato=csv` | S |
| Financeiro | `GET /financeiro/cobrancas`, `GET /financeiro/cobrancas/{id}`, `GET /financeiro/documentos` | M, P, S |
| | `GET /financeiro/cobrancas/{id}/pix` | M, P |
| | `POST .../baixa-manual`, `POST /financeiro/documentos`, `POST /financeiro/taxas`, `POST /financeiro/taxas/{id}/gerar-cobrancas`, `GET /financeiro/inadimplencia` | S |
| | `POST /financeiro/webhooks/pagamentos` | gateway (assinatura HMAC) |
| Portaria | `POST /portaria/visitantes`, `POST .../reenviar`, `POST /portaria/acessos` | R |
| | `GET /portaria/autorizacoes`, `GET /portaria/autorizacoes/{id}` | R, M, P |
| | `PATCH /portaria/autorizacoes/{id}/decisao` | M, P |
| | `GET /portaria/acessos` | R, S |
| Reservas | `GET /areas-comuns`, `GET /reservas`, `GET /reservas/{id}` | todos |
| | `GET /areas-comuns/{id}/disponibilidade`, `POST /reservas`, `DELETE /reservas/{id}` | M, P |
| | `POST /reservas/{id}/vistoria` | R |
| Minha Unidade | `GET /unidades/minha`, `GET /unidades/minha/vinculos`, `POST /unidades/minha/vinculos/solicitacoes` | M, P |
| | `POST /unidades/minha/inquilinos`, `DELETE /unidades/minha/inquilinos/{id}` | P |
| | `GET /unidades/{id}`, `GET`/`PATCH /unidades/{id}/vinculos/solicitacoes...` | S |
| Encomendas | `GET /encomendas`, `GET /encomendas/{id}` | todos |
| | `POST /encomendas`, `POST /encomendas/{id}/retirada` | R |
| | `POST /encomendas/{id}/autorizacoes-retirada`, `POST /encomendas/{id}/ocorrencias` | M, P |
| Notificações | `GET /notificacoes`, `PATCH /notificacoes/{id}/lida` | todos |
| Apoio (fora do contrato) | `POST`/`GET /usuarios`, `POST /usuarios/{id}/desativar` | S |
| | `GET /monitor/metricas` (mostra os singletons) | público |

### Testando o webhook de pagamento

O gateway assina o corpo com HMAC-SHA256 usando o segredo `WEBHOOK_SEGREDO`. Para simular:

```bash
CORPO='{"transacaoId":"tx-1","cobrancaId":1,"status":"confirmado"}'
ASSINATURA=$(printf '%s' "$CORPO" | openssl dgst -sha256 -hmac "morah-webhook-de-desenvolvimento" | sed 's/^.* //')
curl -X POST http://localhost:8080/v1/financeiro/webhooks/pagamentos \
  -H "Content-Type: application/json" -H "X-Webhook-Signature: $ASSINATURA" -d "$CORPO"
```

> **Diferença em relação ao contrato:** o `morah-api.yaml` previa o Keycloak como servidor de
> autenticação e, por isso, não tinha rota de usuário/senha. Como a hospedagem será no plano
> gratuito do Azure (sem espaço para outro servidor), a autenticação foi feita dentro da própria
> API com JWT e ganhamos o `POST /auth/login`. As demais rotas de sessão seguem o contrato.

---

## 3. Rodando os testes

```bash
./gradlew test
```

Os testes não precisam de banco: as regras de cada módulo e as classes de padrão de projeto são
testadas com repositórios falsos (Mockito), e cada módulo tem um teste HTTP de segurança e
formato do JSON. O `MapeamentoMongoTest` confere, sem banco, que todas as entidades vão para o
formato do MongoDB (BSON) e voltam iguais.

---

## 4. Publicando no Azure Web App (plano gratuito)

1. **MongoDB Atlas** — crie um cluster M0 (gratuito), um usuário de banco e libere o acesso de
   rede (`0.0.0.0/0` para testes). Copie a *connection string*.
2. **Gere o .jar**:

```bash
./gradlew bootJar
```

O arquivo sai em `build/libs/Morah-0.0.1-SNAPSHOT.jar`.

3. **Crie o Web App** (Linux, runtime **Java 21**, plano F1 gratuito).
4. **Configure as variáveis de ambiente** no Azure (*Configuration → Application settings*):

| Variável | Valor |
|----------|-------|
| `MONGODB_URI` | string de conexão do Atlas |
| `JWT_SEGREDO` | um texto secreto com 32+ caracteres |
| `CORS_ORIGENS` | URL do front, ex.: `https://morah.vercel.app` |
| `WEBHOOK_SEGREDO` | segredo combinado com o gateway de pagamento |
| `CHAVE_PIX` | chave PIX do condomínio (usada no código copia-e-cola) |
| `SPRING_PROFILES_ACTIVE` | `prod` |

5. **Faça o deploy** do jar (pelo plugin do Azure no VS Code/IntelliJ, pelo `az webapp deploy`
   ou pelo workflow do GitHub Actions em `.github/workflows/azure-webapp.yml`).

A aplicação usa a porta indicada pela variável `PORT` (o Azure define isso sozinho) e expõe
`/v1/actuator/health` para o health check.

> Dicas para o plano gratuito: o F1 hiberna quando fica sem uso (a primeira chamada depois
> disso demora alguns segundos) e tem pouca memória — por isso as *virtual threads* do Java 21
> já vêm ligadas no `application.yaml`.

---

## 5. Onde continuar o trabalho

Cada módulo novo segue o mesmo desenho dos existentes (modelo → repositório → service →
controller). O passo a passo e as limitações conhecidas (integrações simuladas, ausência de
transações e de rotinas agendadas) estão em [`ARQUITETURA.md`](ARQUITETURA.md).

---

## 6. Problemas comuns

| Sintoma | O que fazer |
|---------|-------------|
| A aplicação sobe e cai com erro do driver `mongodb` (`MongoTimeoutException`) | Não há MongoDB acessível. Suba um Mongo local ou aponte a `MONGODB_URI` para o Atlas |
| `401` em todas as chamadas | O token expirou (1 hora). Faça login de novo ou use `POST /auth/refresh` |
| `403` mesmo logado | O perfil ativo não tem permissão para a rota; troque o contexto em `POST /auth/contexto` |
| O front reclama de CORS | Inclua a URL do front na variável `CORS_ORIGENS` |
| Quero recriar os dados de exemplo | Apague o banco (ou as coleções do módulo) e reinicie com `morah.carga-inicial=true`; cada carga só cria o que estiver faltando |
| Datas de banco antigo aparecem um dia antes/depois | Bancos criados antes da mudança para datas em UTC (`ConfiguracaoMongo`) devem ser recriados |

> Pequena diferença em relação ao contrato: `GET /notificacoes` devolve a lista no mesmo
> envelope paginado dos outros endpoints (`{ "content": [...], "page": {...} }`) em vez de um
> array puro — o contrato já previa os parâmetros `page` e `size` nessa rota.
