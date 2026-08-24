# mcp-regression-platform

Plataforma de **regression readiness** para BFFs de uma fintech fictícia, construída como demonstração
técnica de uma arquitetura moderna de **AI Application + MCP + RAG + Agent + Security**.

A pergunta que a plataforma responde é operacional:

> O ambiente está pronto para executar uma regressão do `fintech-bff-account`?

O MCP Server é **uma** das interfaces da plataforma, não o núcleo dela. A lógica vive na camada de
aplicação e de domínio; MCP, REST e CLI são adaptadores de entrada.

---

## Sumário

- [O que a demo mostra](#o-que-a-demo-mostra)
- [Arquitetura em uma tela](#arquitetura-em-uma-tela)
- [Stack](#stack)
- [Executando com Docker](#executando-com-docker)
- [Executando localmente](#executando-localmente)
- [Roteiro da demonstração](#roteiro-da-demonstração)
- [Conectando o Claude Code ao MCP Server](#conectando-o-claude-code-ao-mcp-server)
- [Capacidades MCP](#capacidades-mcp)
- [Segurança](#segurança)
- [Testes](#testes)
- [Documentação](#documentação)
- [Limitações da demo](#limitações-da-demo)

---

## O que a demo mostra

| Eixo | O que está implementado |
|------|-------------------------|
| MCP | 7 Tools, 6 Resources, 4 Prompts, transports Streamable HTTP e STDIO |
| AI | Agent com planner + workflow determinístico, LLM apenas para interpretação, RAG com pgvector |
| Segurança | OAuth2/JWT, RBAC por capability, autorização por Tool e por Resource, mitigação de prompt injection |
| Regressão | Descoberta de dependências, health check, validação de contrato, smoke tests, análise de prontidão |
| Engenharia | Monólito modular hexagonal, Testcontainers, WireMock, Actuator/Micrometer, Docker Compose |

---

## Arquitetura em uma tela

```text
                     UNTRUSTED
                         |
                  LLM / MCP Host
                         |
                  MCP Boundary  (Tools / Resources / Prompts)
                         |
                  Authentication  (JWT)
                         |
                  Authorization   (role -> capability)
                         |
                  Policy Validation (input + prompt injection)
                         |
                     TRUSTED
                         |
                  Application Use Cases
                         |
                       Domain
                         |
                       Ports
                         |
          +--------------+--------------+
          |                             |
   HTTP Adapter                  JDBC / pgvector
 (fintech-srv-account)           (fintech-db + RAG)
```

O fluxo de regressão é uma máquina de estados determinística:

```text
DISCOVER_DEPENDENCIES -> CHECK_HEALTH -> VALIDATE_SECURITY -> VALIDATE_CONTRACT
   -> RUN_SMOKE_TEST -> RETRIEVE_KNOWLEDGE -> ANALYZE
```

O planner pode **pular** estágios (contrato e smoke test) quando uma dependência crítica já está
bloqueada. O LLM entra somente em `ANALYZE`, e apenas para explicar evidências já classificadas.

---

## Stack

| Componente | Versão |
|-----------|--------|
| Kotlin | 2.3.21 |
| JDK | 21 |
| Spring Boot | 4.1.1 |
| Spring AI | 2.0.0 |
| MCP Java SDK | 2.0.0 |
| PostgreSQL + pgvector | pg17 |
| Redis | 7 |
| WireMock | 3.13 |
| Testcontainers | 2.0.5 |

---

## Executando com Docker

```bash
cp .env.example .env
docker compose up --build
```

Sobem quatro serviços: `postgres` (com pgvector), `redis`, `wiremock` (simulando o
`fintech-srv-account`) e a própria plataforma em `http://localhost:8080`.

Observabilidade opcional (Prometheus + Grafana):

```bash
docker compose --profile observability up -d
```

---

## Executando localmente

Suba apenas a infraestrutura e rode a aplicação pela JVM:

```bash
docker compose up -d postgres redis wiremock

export PLATFORM_JWT_SIGNING_KEY=local-development-signing-key-32-chars-min
export PLATFORM_ACCOUNT_SERVICE_API_KEY=demo-service-api-key
export PLATFORM_ACCOUNT_SERVICE_URL=http://localhost:8081
export PLATFORM_DEV_PASSWORD=dev-password
export PLATFORM_QA_PASSWORD=qa-password
export PLATFORM_ARCHITECT_PASSWORD=architect-password

./gradlew bootRun
```

Para rodar o MCP Server em **STDIO** (sem servidor HTTP):

```bash
./gradlew bootJar
PLATFORM_MCP_STDIO_TOKEN="$(./demo.sh token qa)" \
  java -jar build/libs/mcp-regression-platform-0.0.1-SNAPSHOT.jar --spring.profiles.active=stdio
```

---

## Roteiro da demonstração

```bash
./demo.sh regression fintech-bff-account     # cenário saudável
./demo.sh scenario service-down
./demo.sh regression fintech-bff-account     # cenário bloqueado
./demo.sh scenario contract-mismatch
./demo.sh regression fintech-bff-account     # bloqueio por contrato
./demo.sh scenario high-latency
./demo.sh regression fintech-bff-account     # warning por latência
./demo.sh scenario healthy
./demo.sh mcp                                 # lista as Tools via Streamable HTTP
```

Cenários disponíveis: `healthy`, `service-down`, `contract-mismatch`, `authentication-failure`,
`high-latency`, `timeout`. A troca de cenário é feita por um endpoint técnico restrito à role
`ARCHITECT` e **não** é exposta como Tool MCP: o LLM não controla o ambiente.

---

## Conectando o Claude Code ao MCP Server

Gere um token e registre o servidor:

```bash
TOKEN="$(./demo.sh token qa)"

claude mcp add --transport http regression http://localhost:8080/mcp \
  --header "Authorization: Bearer $TOKEN"
```

Depois, no Claude Code:

> Prepare o ambiente para uma regressão do fintech-bff-account.

O host vai descobrir as Tools, os Resources `regression://...` e os Prompts registrados.

Para STDIO, registre o comando `java -jar ... --spring.profiles.active=stdio` passando
`PLATFORM_MCP_STDIO_TOKEN` no ambiente do processo.

---

## Capacidades MCP

**Tools**

| Tool | Classificação | Capability exigida |
|------|---------------|--------------------|
| `get_bff_dependencies` | READ | `READ_DEPENDENCIES` |
| `check_dependency_health` | READ | `CHECK_HEALTH` |
| `validate_service_contract` | VALIDATION | `VALIDATE_CONTRACT` |
| `run_smoke_test` | EXECUTION | `RUN_SMOKE_TEST` |
| `search_regression_knowledge` | READ | `SEARCH_KNOWLEDGE` |
| `get_regression_status` | EXECUTION | `RUN_REGRESSION` |
| `run_regression_analysis` | EXECUTION | `RUN_REGRESSION` |

**Resources**

```text
regression://bffs
regression://bff/{name}
regression://dependencies/{service}
regression://contracts/{service}
regression://runbooks/{name}
regression://architecture/{name}
```

**Prompts**

```text
regression-readiness-analysis
dependency-diagnosis
contract-risk-analysis
incident-analysis
```

Detalhes em [MCP.md](MCP.md).

---

## Segurança

Roles e capacidades:

| Role | Capacidades |
|------|-------------|
| `DEV` | ler dependências, health check, validar contrato, smoke test, buscar conhecimento |
| `QA` | tudo de DEV + executar regressão |
| `ARCHITECT` | tudo de DEV + recursos de arquitetura + análise avançada |

Regra central: **o LLM nunca é autoridade de segurança**. As capacidades são derivadas das roles pelo
servidor, nunca lidas do token. Um validador de inicialização derruba a aplicação se alguma Tool ou
Resource MCP não estiver declarada no registro de autorização.

Detalhes em [SECURITY.md](SECURITY.md) e [THREAT-MODEL.md](THREAT-MODEL.md).

---

## Testes

```bash
./gradlew test
```

62 testes: unitários de domínio (classificação de health, comparação de contrato, política de
capacidades, detector de injeção, guarda de saída do modelo) e testes de integração com Testcontainers
(PostgreSQL/pgvector e Redis) e WireMock, cobrindo os cinco cenários de regressão, o protocolo MCP
(discovery e invocação de Tools, Resources e Prompts) e a superfície de segurança.

Os testes de integração exigem Docker. Sem Docker, eles são ignorados e os testes unitários rodam.

---

## Documentação

- [ARCHITECTURE.md](ARCHITECTURE.md) — decisões arquiteturais e trade-offs
- [MCP.md](MCP.md) — superfície MCP e transports
- [SECURITY.md](SECURITY.md) — autenticação, autorização e fronteiras
- [RAG.md](RAG.md) — ingestão, embeddings e recuperação
- [THREAT-MODEL.md](THREAT-MODEL.md) — modelo de ameaças e mitigações
- [docs/adr/](docs/adr/) — ADRs

---

## Limitações da demo

- Não há IdP real: o endpoint `/auth/token` é um emissor JWT local com usuários de demonstração.
- Sem secret manager: os segredos vêm de variáveis de ambiente.
- O `fintech-srv-account` é simulado com WireMock, não é um microsserviço real.
- Sem LLM configurado, a narrativa é gerada por um template determinístico; com um modelo configurado,
  o LLM explica as mesmas evidências.
- A validação de contrato é uma comparação dirigida, não um diff completo de OpenAPI.
- A defesa contra prompt injection é uma camada de mitigação, não uma garantia.
