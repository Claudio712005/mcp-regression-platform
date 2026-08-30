# MCP

## Papel do MCP na arquitetura

O MCP Server é **uma interface de integração**, não o núcleo da plataforma. Toda Tool segue o mesmo
caminho:

```text
MCP Tool -> McpSecurityGate -> Application Use Case -> Domain -> Port -> Adapter -> Sistema externo
```

As classes de MCP não contêm regra de negócio. Elas validam entrada, autorizam, delegam e mapeiam a
resposta para uma view.

## Versões

| Item | Versão |
|------|--------|
| Spring AI | 2.0.0 |
| MCP Java SDK | 2.0.0 |
| Starters | `spring-ai-starter-mcp-server`, `spring-ai-starter-mcp-server-webmvc` |
| Modelo de programação | anotações `org.springframework.ai.mcp.annotation` |

## Transports

| Transport | Como habilitar | Uso |
|-----------|----------------|-----|
| Streamable HTTP | default (`spring.ai.mcp.server.protocol=STREAMABLE`), endpoint `POST /mcp` | Claude Code, Copilot, qualquer host remoto |
| STDIO | `--spring.profiles.active=stdio` (`spring.ai.mcp.server.stdio=true`) | host local que executa o processo |

SSE não é usado como transport principal: na versão atual do Spring AI, Streamable HTTP é o transport
HTTP corrente e SSE permanece por compatibilidade.

Os dois transports são mutuamente exclusivos em runtime, porque a autoconfiguração desativa os
transports web quando STDIO está ligado. São dois modos de execução do mesmo binário.

### Identidade no transport

A plataforma declara o próprio `WebMvcStreamableServerTransportProvider` para instalar um
`contextExtractor` que copia o header `Authorization` para o `McpTransportContext`:

```kotlin
.contextExtractor { request ->
    McpTransportContext.create(
        buildMap {
            request.headers().firstHeader("Authorization")?.let {
                put(McpSecurityGate.AUTHORIZATION_KEY, it)
            }
        }
    )
}
```

As Tools recebem `McpSyncRequestContext` e os Resources recebem `McpTransportContext`; ambos chegam ao
`McpSecurityGate`. No modo STDIO, onde não existe header HTTP, o token vem da variável de ambiente
`PLATFORM_MCP_STDIO_TOKEN`.

## Tools

Sete Tools, pequenas e orientadas a capacidade. Não existe Tool genérica: nada de `execute_sql`,
`execute_http` ou `execute_shell`.

| Tool | Classificação | Capability | Parâmetros |
|------|---------------|-----------|------------|
| `get_bff_dependencies` | READ | `READ_DEPENDENCIES` | `bff` |
| `check_dependency_health` | READ | `CHECK_HEALTH` | `bff`, `dependency?` |
| `validate_service_contract` | VALIDATION | `VALIDATE_CONTRACT` | `service` |
| `run_smoke_test` | EXECUTION | `RUN_SMOKE_TEST` | `bff` |
| `search_regression_knowledge` | READ | `SEARCH_KNOWLEDGE` | `question`, `topK?` |
| `get_regression_status` | EXECUTION | `RUN_REGRESSION` | `bff` |
| `run_regression_analysis` | EXECUTION | `RUN_REGRESSION` | `bff` |

`get_regression_status` devolve o veredito determinístico com as evidências.
`run_regression_analysis` devolve o mesmo veredito acrescido da narrativa, com o campo
`narrativeSource` indicando se veio do LLM ou do template determinístico.

## Resources

Resources expõem o **ambiente declarado**, não uma duplicação das Tools. Tools respondem "como está
agora"; Resources respondem "o que existe e como foi declarado".

| URI | Capability | Conteúdo |
|-----|-----------|----------|
| `regression://bffs` | `READ_DEPENDENCIES` | catálogo de BFFs registrados |
| `regression://bff/{name}` | `READ_DEPENDENCIES` | definição de um BFF e suas dependências |
| `regression://dependencies/{service}` | `READ_DEPENDENCIES` | metadados da dependência e consumidores |
| `regression://contracts/{service}` | `VALIDATE_CONTRACT` | contrato esperado versionado |
| `regression://runbooks/{name}` | `SEARCH_KNOWLEDGE` | runbook em Markdown |
| `regression://architecture/{name}` | `READ_ARCHITECTURE` | nota de arquitetura em Markdown |

`regression://bffs` é um Resource estático; os demais são resource templates, descobertos via
`resources/templates/list`.

## Prompts

Quatro Prompts reutilizáveis, carregados do sistema interno de prompts (`prompts/workflows/*.md`),
sem nenhum segredo.

| Prompt | Argumentos |
|--------|-----------|
| `regression-readiness-analysis` | `bff` |
| `dependency-diagnosis` | `bff`, `dependency` |
| `contract-risk-analysis` | `service` |
| `incident-analysis` | `bff`, `symptom` |

Os argumentos passam pelo mesmo validador de entrada das Tools: `symptom` é texto livre e é submetido
ao detector de prompt injection antes de ser inserido no prompt.

## Sistema interno de prompts

Além dos Prompts MCP, existe um catálogo interno versionado em Git:

```text
prompts/
├── system/
│   ├── agent-identity.md
│   ├── security-policy.md
│   ├── regression-policy.md
│   └── tool-usage-policy.md
├── workflows/
│   ├── regression-analysis.md
│   ├── dependency-analysis.md
│   ├── contract-analysis.md
│   └── incident-analysis.md
└── rag/
    └── knowledge-analysis.md
```

`ClasspathPromptCatalog` resolve por referência (`system/security-policy`). Não há versionamento
complexo: o objetivo é demonstrar que prompts são artefato de arquitetura, revisável em pull request,
e não string espalhada pelo código.

## Cobertura de autorização garantida na inicialização

`McpCapabilityCoverageValidator` varre os beans à procura de métodos `@McpTool` e `@McpResource` e
falha a inicialização se algum deles não estiver declarado no `McpCapabilityRegistry` — ou se uma Tool
não tiver a anotação `@GuardedTool`. É impossível subir a aplicação com uma capacidade MCP sem
requisito de autorização.

## Testando o protocolo

`McpProtocolIntegrationTest` usa o cliente MCP oficial sobre Streamable HTTP e cobre discovery e
invocação de Tools, listagem e leitura de Resources e resource templates, e listagem e renderização de
Prompts. `McpSecurityIntegrationTest` cobre a superfície de segurança do mesmo endpoint.

Sem um host MCP, o `demo.sh mcp` faz `initialize` e `tools/list` via `curl`.
