# Arquitetura

## Problema

Antes de rodar uma suíte de regressão contra um BFF, alguém precisa responder se o ambiente está
pronto. Na prática essa resposta é montada manualmente: abrir o health de cada dependência, conferir
se o contrato do serviço mudou, rodar alguns testes de fumaça, lembrar de qual runbook trata daquele
sintoma. É trabalho repetitivo, propenso a erro e mal documentado.

A `mcp-regression-platform` transforma essa investigação em uma capacidade de plataforma: determinística
onde precisa ser determinística, assistida por IA onde a IA agrega valor real (correlação e explicação),
e exposta para agentes através do Model Context Protocol.

## Solução: monólito modular hexagonal

A plataforma é um **monólito modular**. Criar microsserviços para a própria plataforma adicionaria
operação sem adicionar capacidade. Os módulos são pacotes com dependências dirigidas:

```text
domain          <- sem dependência de framework
application     <- depende de domain e de ports
infrastructure  <- depende de application e domain, implementa os ports
```

### Camadas

```text
src/main/kotlin/br/com/claus/mcpregressionplatform
├── domain
│   ├── dependency   ServiceDependency, HealthState, HealthClassifier
│   ├── contract     ApiContract, ContractComparator, ContractViolation
│   ├── smoke        SmokeTestOutcome, SmokeTestSuiteResult
│   ├── regression   RegressionStage, ReadinessStatus, ReadinessPolicy, Evidence
│   ├── security     Role, Capability, CapabilityPolicy, AuthenticatedPrincipal
│   │   └── injection  PromptInjectionDetector
│   └── knowledge    KnowledgeDocument, RetrievedPassage, TrustLevel
│
├── application
│   ├── port         BffRegistry, DependencyHealthProbe, PublishedContractSource,
│   │                SmokeTestRunner, KnowledgeSearchPort, ReasoningModel, PromptCatalog
│   ├── dependency   DiscoverDependenciesUseCase, CheckDependencyHealthUseCase,
│   │                ValidateIntegrationSecurityUseCase
│   ├── contract     ValidateServiceContractUseCase
│   ├── smoke        RunSmokeTestsUseCase
│   ├── knowledge    SearchKnowledgeUseCase
│   ├── regression   GetRegressionStatusUseCase, RunRegressionAnalysisUseCase
│   ├── agent        RegressionWorkflow, RegressionPlanner, EvidenceNarrator, EvidenceRenderer
│   └── security     ModelOutputGuard, UntrustedContentEnvelope
│
└── infrastructure
    ├── mcp          tools, resources, prompts, transport, security gate
    ├── security     JWT, emissão de token, filtros, principal
    ├── persistence  JdbcBffRegistry, DatabaseHealthProbeAdapter
    ├── http         AccountServiceClient, health probe, contract source, smoke runner
    ├── contract     OpenApiContractParser, ClasspathExpectedContractCatalog
    ├── rag          chunking, ingestão, PgVectorKnowledgeAdapter
    ├── llm          ReasoningModel adapters, embedding fallback
    ├── prompt       ClasspathPromptCatalog
    ├── observability PlatformMetrics
    ├── api          controllers REST e renderizador de console
    ├── demo         DemoScenarioHolder
    └── configuration PlatformProperties e wiring de domínio
```

### Hexagonal sem purismo

Foram criados ports somente onde existe uma fronteira real de tecnologia ou de teste:
registro de BFFs, sonda de saúde, fonte de contrato publicado, runner de smoke test, busca semântica,
modelo de linguagem e catálogo de prompts. Não existe interface para cada caso de uso, nem DTO
duplicado por camada, nem mapper cerimonial. Os casos de uso são classes concretas: eles já são o port
de entrada.

Serviços de domínio (`HealthClassifier`, `ContractComparator`, `ReadinessPolicy`, `CapabilityPolicy`,
`PromptInjectionDetector`) são classes puras, sem anotação de framework, instanciadas em
`DomainConfiguration`. Isso mantém a regra de negócio testável sem contexto Spring — e os testes
unitários provam isso.

## Fronteira determinística versus IA

A separação é explícita e é o ponto central do projeto.

**Determinístico (código):**

- classificação de estado de saúde a partir de status HTTP, latência e timeout;
- autenticação e autorização;
- comparação de contrato;
- execução e avaliação dos smoke tests;
- classificação de prontidão (`READY_FOR_REGRESSION`, `WARNING`, `BLOCKED`);
- decisão de quais estágios executar ou pular.

**IA (LLM):**

- explicação causal das evidências;
- correlação entre achados de estágios diferentes;
- interpretação do conhecimento recuperado;
- recomendação de ordem de investigação.

O LLM recebe o status já calculado e é instruído a explicá-lo, não a decidi-lo. A saída do modelo passa
por `ModelOutputGuard`, que descarta respostas que vazam o system prompt, carregam payload de injeção
ou contradizem o status determinístico. Quando não há modelo configurado — ou quando a saída é
descartada — a narrativa vem de um template determinístico, e a resposta declara a origem em
`narrativeSource`.

## O agente

`RegressionWorkflow` é uma máquina de estados. `RegressionPlanner` decide, com regras determinísticas,
quais estágios fazem sentido: se uma dependência crítica está `UNAVAILABLE`, validar contrato e rodar
smoke test só produziria ruído, então esses estágios são pulados e registrados como `stagesSkipped`
com a justificativa. Essa é a parte de "planning" do agente — e ela não é delegada ao LLM.

## Decisões e divergências em relação à especificação inicial

1. **Transport SSE não é usado.** A versão atual do Spring AI trata Streamable HTTP como o transport
   HTTP corrente (`spring.ai.mcp.server.protocol=STREAMABLE`, que é o default) e mantém SSE por
   compatibilidade. A plataforma usa Streamable HTTP e STDIO.
2. **Streamable HTTP e STDIO são mutuamente exclusivos em runtime.** A autoconfiguração do Spring AI
   desabilita os transports web quando `spring.ai.mcp.server.stdio=true`. Por isso STDIO é um profile
   (`--spring.profiles.active=stdio`), não um segundo listener no mesmo processo.
3. **Programação por anotações.** Tools, Resources e Prompts usam `@McpTool`, `@McpResource` e
   `@McpPrompt` do pacote `org.springframework.ai.mcp.annotation`, com o scanner de anotações do
   starter. Não são construídas `SyncToolSpecification` manualmente.
4. **`WebMvcStreamableServerTransportProvider` é declarado pela aplicação.** A autoconfiguração cria o
   bean com `@ConditionalOnMissingBean`; a plataforma o substitui para instalar um `contextExtractor`
   que propaga o header `Authorization` para o `McpTransportContext`. É assim que a identidade chega às
   Tools sem depender de `ThreadLocal` do servlet.
5. **Embeddings determinísticos por padrão.** Sem chave de modelo configurada, a plataforma registra um
   `EmbeddingModel` lexical (hashing de tokens, 384 dimensões) para que o RAG funcione offline. Com
   `PLATFORM_EMBEDDING_MODEL=openai`, o modelo real assume o lugar.
6. **Sem auditoria.** Fora de escopo por decisão explícita: logs técnicos e métricas cobrem a demo.

## Trade-offs assumidos

| Decisão | Ganho | Custo |
|---------|-------|-------|
| Monólito modular | Simplicidade operacional, refatoração barata | Escala de time limitada |
| Registro de BFFs em PostgreSQL | Demonstra o adapter de persistência e permite evolução | Poderia ser YAML numa demo |
| Redis como cache de sonda | Evita martelar a dependência durante um ciclo do agente | Mais um serviço na stack |
| Comparação de contrato dirigida | Código pequeno e legível, suficiente para decidir | Não cobre todo o OpenAPI |
| WireMock em vez de um SRV real | Cenários de falha reproduzíveis sem código extra | Não exercita um serviço real |
| Embeddings lexicais no fallback | Demo roda sem chave de API | Qualidade semântica inferior |

## O que mudaria em produção

- IdP real (Keycloak, Entra ID, Auth0) emitindo os tokens; a plataforma continuaria apenas validando.
- Secret manager (Vault, AWS Secrets Manager) no lugar de variáveis de ambiente.
- Registro de serviços alimentado por descoberta real (service mesh, catálogo interno) em vez de seed.
- Embeddings e LLM gerenciados, com orçamento, rate limit e fallback explícito.
- Exportador OTLP apontando para um coletor, com alertas em cima de `dependency_health_status`.
- Política de retenção e reindexação incremental da base de conhecimento.
- Auditoria de chamadas de capacidade, deliberadamente fora do escopo desta demo.
