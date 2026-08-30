# RAG

## Escopo

RAG moderado e proposital: busca semântica sobre a base de conhecimento da própria plataforma, usada
para trazer o runbook certo para dentro da análise de regressão. Não há hybrid search, reranking,
graph RAG nem multi-vector retrieval — nenhum deles agregaria à demonstração arquitetural.

## Base de conhecimento

O corpus é o diretório `docs/`, versionado em Git e copiado para o classpath como `knowledge/` durante
o build:

```text
docs/
├── architecture/     visão geral e limites de confiança
├── regression/       política de regressão e critérios de bloqueio
├── runbooks/         runbooks por sintoma
├── security/         modelo de autorização e prompt injection
├── api-contracts/    descrição do contrato account-api
└── adr/              decisões arquiteturais
```

A documentação lida por humanos e o corpus consultado pelo agente são o mesmo artefato. Documentação
desatualizada degrada a resposta do agente, o que é exatamente o incentivo desejado.

## Ingestão

```text
documents -> parser -> chunking -> embedding -> pgvector
```

- `MarkdownKnowledgeSource` carrega os arquivos e extrai título (primeiro `# `), categoria (diretório)
  e origem.
- `DocumentChunker` agrupa parágrafos até ~700 caracteres com ~120 de sobreposição, preservando limites
  de parágrafo em vez de cortar no meio de uma frase.
- `PgVectorKnowledgeAdapter` indexa cada chunk com um id UUID determinístico
  (`UUID.nameUUIDFromBytes(documentId + "#" + ordinal)`), o que torna a reingestão idempotente.
- O texto embutido recebe um cabeçalho com título, categoria e origem. Isso melhora a recuperação de
  consultas que citam o nome do runbook ou do serviço.
- `KnowledgeIngestionRunner` roda na inicialização e não faz nada se a tabela já estiver populada.

## Recuperação

```text
question -> embedding -> semantic search -> top-k -> filtro de injeção -> LLM
```

`SearchRequest` com `topK` (limitado a 10) e `similarityThreshold` configurável
(`platform.knowledge.minimum-score`, default `0.10`). O resultado é mapeado para `RetrievedPassage`
com título, categoria, origem e score.

## Embeddings

Por padrão a plataforma registra um `EmbeddingModel` lexical determinístico de 384 dimensões
(hashing de tokens com normalização L2), condicionado a `@ConditionalOnMissingBean`. Isso mantém a demo
executável sem chave de API e torna os testes reprodutíveis.

Com `PLATFORM_EMBEDDING_MODEL=openai` e `SPRING_AI_OPENAI_API_KEY` definidos, o modelo real assume o
lugar. Ao trocar de modelo é preciso ajustar `spring.ai.vectorstore.pgvector.dimensions` e reindexar,
porque a dimensionalidade do vetor muda.

## Fronteira de confiança

Passagem recuperada é dado, nunca instrução:

1. `TrustLevel.UNTRUSTED` em toda passagem;
2. `PromptInjectionDetector` inspeciona o texto recuperado; passagem com sinal de risco alto é marcada
   como `quarantined`, com o motivo, e o texto **não** é devolvido nem enviado ao modelo;
3. o que sobra é envolto em `<untrusted-data>` antes de ir para o prompt, com marcadores aninhados
   neutralizados;
4. `prompts/rag/knowledge-analysis.md` instrui o modelo a ignorar sentenças imperativas dentro das
   passagens e a preferir a evidência da plataforma quando houver conflito;
5. quarentena vira evidência de `WARNING` no relatório de prontidão — o operador fica sabendo que a base
   de conhecimento contém conteúdo suspeito.

`KnowledgeQuarantineIntegrationTest` indexa um documento envenenado e prova os pontos 2 e 5.

## Configuração

```yaml
platform:
  knowledge:
    location: classpath:knowledge/
    chunk-size: 700
    chunk-overlap: 120
    ingest-on-startup: true
    minimum-score: 0.10
```

## O que falta para produção

- Reindexação incremental por hash de documento em vez de "pular se já tem dados".
- Avaliação de recuperação com um conjunto de perguntas de referência.
- Hybrid search (BM25 + vetorial), que só se justifica quando o corpus cresce.
- Particionamento por domínio e filtro de metadados por permissão do solicitante.
