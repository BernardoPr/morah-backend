# Morah API

Base (template) da API do sistema Morah: **Java 21 + Spring Boot 4 + MongoDB Atlas**.

O objetivo deste repositório é servir de **esqueleto pronto** para o time: a arquitetura,
a segurança, o tratamento de erros, a paginação e os padrões de projeto já estão no lugar.
As regras de negócio de cada módulo (financeiro, portaria, reservas, encomendas...) ainda
serão implementadas — a base mostra exatamente onde cada coisa deve entrar.

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

### Usuários de teste

Na primeira execução com o banco vazio, a aplicação cria dois usuários (senha `morah1234`):

| CPF           | Nome        | Perfis                                   |
|---------------|-------------|------------------------------------------|
| `11111111111` | Ana Souza   | morador (Apto 101)                       |
| `22222222222` | Carlos Lima | síndico **e** morador (Apto 202)         |

Use o Carlos para testar a troca de contexto (`POST /auth/contexto`).

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

### Endpoints que já funcionam

| Método | Rota | Quem acessa | O que faz |
|--------|------|-------------|-----------|
| POST | `/auth/login` | público | Entra com CPF e senha |
| POST | `/auth/refresh` | público | Renova a sessão |
| GET | `/auth/me` | autenticado | Dados da pessoa + perfis disponíveis |
| POST | `/auth/contexto` | autenticado | Troca o condomínio/perfil ativo |
| POST | `/auth/logout` | autenticado | Encerra a sessão |
| GET | `/home/dashboard` | autenticado | Tela inicial (muda conforme o perfil) |
| GET/POST/PATCH/DELETE | `/avisos` | leitura: todos / escrita: síndico | CRUD do mural |
| GET | `/avisos/exportar?formato=csv` | síndico | Baixa a lista em csv ou json |
| GET/PATCH | `/notificacoes` | autenticado | Notificações do usuário |
| POST/GET | `/usuarios` | síndico | Cadastro de usuários |
| GET | `/monitor/metricas` | público | Mostra os singletons funcionando |

> **Diferença em relação ao contrato:** o `morah-api.yaml` previa o Keycloak como servidor de
> autenticação e, por isso, não tinha rota de usuário/senha. Como a hospedagem será no plano
> gratuito do Azure (sem espaço para outro servidor), a autenticação foi feita dentro da própria
> API com JWT e ganhamos o `POST /auth/login`. As demais rotas de sessão seguem o contrato.

---

## 3. Rodando os testes

```bash
./gradlew test
```

Os testes não precisam de banco: os singletons e as strategies são testados isoladamente e o
fluxo de login usa um repositório falso (`@MockitoBean`).

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

Cada módulo novo segue o mesmo desenho do módulo `aviso` (modelo → repositório → service →
controller). O passo a passo está em [`ARQUITETURA.md`](ARQUITETURA.md).

---

## 6. Problemas comuns

| Sintoma | O que fazer |
|---------|-------------|
| A aplicação sobe e cai com erro do driver `mongodb` (`MongoTimeoutException`) | Não há MongoDB acessível. Suba um Mongo local ou aponte a `MONGODB_URI` para o Atlas |
| `401` em todas as chamadas | O token expirou (1 hora). Faça login de novo ou use `POST /auth/refresh` |
| `403` mesmo logado | O perfil ativo não tem permissão para a rota; troque o contexto em `POST /auth/contexto` |
| O front reclama de CORS | Inclua a URL do front na variável `CORS_ORIGENS` |
| Quero recriar os usuários de exemplo | Apague a coleção `usuarios` no banco e reinicie com `morah.carga-inicial=true` |

> Pequena diferença em relação ao contrato: `GET /notificacoes` devolve a lista no mesmo
> envelope paginado dos outros endpoints (`{ "content": [...], "page": {...} }`) em vez de um
> array puro — o contrato já previa os parâmetros `page` e `size` nessa rota.
