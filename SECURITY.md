# Segurança

## Princípio

> O LLM nunca é autoridade de segurança. Ele pode solicitar uma ação; quem decide se a ação é
> permitida é a plataforma.

Tudo que atravessa a fronteira MCP é tratado como entrada não confiável, inclusive quando vem de um
host legítimo como o Claude Code.

## Fronteira

```text
                 UNTRUSTED
                     |
              LLM / MCP Host
                     |
              MCP Boundary
                     |
              Authentication      JWT validado (assinatura, expiração, emissor)
                     |
              Authorization       role -> capability, derivada no servidor
                     |
              Policy Validation   validação de entrada + filtro de injeção
                     |
                 TRUSTED
                     |
              Application Core
                     |
          +----------+----------+
          |                     |
       Serviços              Banco
```

## Autenticação

OAuth2/JWT com Spring Security como resource server. Na demo, o endpoint `POST /auth/token` emite
tokens HS256 assinados com `PLATFORM_JWT_SIGNING_KEY` (mínimo 32 caracteres; a aplicação recusa subir
com chave curta ou ausente). Em produção esse endpoint sairia e um IdP real assumiria a emissão — a
plataforma continuaria fazendo apenas validação.

O endpoint `/mcp` exige autenticação já na cadeia de filtros HTTP. Isso é defesa em profundidade: mesmo
que uma Tool esquecesse de checar, uma requisição sem token não chega ao servidor MCP.

## Autorização

O token carrega **apenas** `sub` e `roles`. As capacidades são derivadas das roles pelo servidor:

```kotlin
enum class Role(val capabilities: Set<Capability>) {
    DEV(setOf(READ_DEPENDENCIES, CHECK_HEALTH, VALIDATE_CONTRACT, RUN_SMOKE_TEST, SEARCH_KNOWLEDGE)),
    QA(DEV.capabilities + setOf(RUN_REGRESSION)),
    ARCHITECT(DEV.capabilities + setOf(READ_ARCHITECTURE, ADVANCED_ANALYSIS))
}
```

Consequência prática: um token que traga uma claim `capabilities` forjada não obtém nada — a claim é
ignorada. Existe um teste de integração exatamente para isso.

Não existe role `ADMIN`. Nenhuma role acumula tudo: `QA` executa regressão mas não lê recursos de
arquitetura; `ARCHITECT` lê arquitetura mas não executa regressão. Isso torna a demonstração de negação
verificável em qualquer direção.

## Autorização por capacidade MCP

Cada Tool e cada Resource declara classificação e capacidade exigida no `McpCapabilityRegistry`:

| Classificação | Significado |
|---------------|-------------|
| `READ` | leitura de estado declarado ou observado |
| `VALIDATION` | comparação que não altera o ambiente |
| `EXECUTION` | dispara chamadas ao ambiente alvo |

O `McpSecurityGate` é o único ponto de execução: resolve o token do `McpTransportContext`, monta o
`AuthenticatedPrincipal`, consulta a `CapabilityPolicy` e só então executa o caso de uso. Uma negação
vira `AuthorizationDeniedException`, contabilizada em `mcp_authorization_denied_total`.

`McpCapabilityCoverageValidator` impede que uma Tool ou Resource sem requisito declarado exista:
a aplicação não sobe.

## Segurança das integrações

A chamada ao `fintech-srv-account` envia uma chave de serviço no header configurado
(`X-Service-Api-Key`), vinda de variável de ambiente. `HttpIntegrationSecurityInspector` transforma a
resposta em evidência de domínio: `VALID`, `REJECTED` (HTTP 401/403) ou `NOT_APPLICABLE` (inalcançável).
Credencial rejeitada bloqueia a regressão e faz o planner pular contrato e smoke test.

Nenhum segredo está no código. Na demo eles vêm de `.env` e do Docker Compose; em produção viriam de um
secret manager.

## Validação de entrada

`ToolInputValidator` aplica, antes de qualquer caso de uso:

- identificadores restritos a `[A-Za-z0-9._-]+`, com limite de tamanho;
- texto livre limitado em tamanho e submetido ao detector de prompt injection;
- inteiros com faixa fechada (`topK` entre 1 e 10).

É por isso que `bff=fintech-bff-account'; drop table bff_service; --` é rejeitado antes de chegar ao
repositório — que, ainda assim, usa apenas queries parametrizadas.

## Prompt injection

Tratado como classe de risco, não como problema resolvido. Detalhes e cenários em
[THREAT-MODEL.md](THREAT-MODEL.md). Camadas implementadas:

1. **Separação instrução/dado** — políticas em `prompts/system/`, dados sempre no bloco do usuário.
2. **Rotulagem de confiança** — passagens recuperadas chegam como `UNTRUSTED`, envoltas em
   `<untrusted-data>`, com marcadores aninhados neutralizados.
3. **Validação de entrada** — payloads de injeção em parâmetros de Tool são recusados.
4. **Quarentena no RAG** — passagem recuperada com sinal de injeção não vai para o modelo; ela aparece
   na resposta como `quarantined` com o motivo, e vira evidência de `WARNING`.
5. **Autorização independente do LLM** — nenhuma decisão de acesso passa pelo modelo.
6. **Proteção do system prompt** — saída que contém marcadores do prompt de sistema é descartada.
7. **Restrição de saída** — saída que contradiz o status determinístico é descartada e a plataforma cai
   no template determinístico.
8. **Sem capacidade genérica** — não existe execução de SQL, shell ou HTTP arbitrário para ser
   sequestrada.

## Observabilidade da segurança

- `mcp_authorization_denied_total{capability,reason}` — negações por capacidade e motivo.
- `mcp_tool_calls_total`, `mcp_tool_errors_total`, `mcp_tool_duration` — uso e falha por Tool.
- Logs em nível `WARN` para token inválido e para negação de capacidade, sem eco do token.

## O que falta para produção

- IdP externo, rotação de chave e tokens de curta duração com refresh.
- Secret manager e chaves por ambiente.
- mTLS entre a plataforma e as integrações.
- Rate limiting por principal no endpoint MCP.
- Auditoria de chamadas de capacidade — deliberadamente fora do escopo desta demo.
