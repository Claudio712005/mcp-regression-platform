# ADR-003: RAG com PostgreSQL e pgvector

## Status

Aceito.

## Contexto

A plataforma precisa de busca semântica sobre uma base pequena de documentação (dezenas de chunks) e já
usa PostgreSQL para o registro de BFFs.

## Decisão

Usar PostgreSQL com a extensão pgvector através do `spring-ai-starter-vector-store-pgvector`, com busca
puramente semântica. Sem banco vetorial dedicado, sem hybrid search, sem reranking.

## Consequências

**Positivas**

- Um banco a menos para operar; o registro e os vetores compartilham backup e transação.
- `PgVectorStore` cuida do schema; a aplicação cuida do chunking e da fronteira de confiança.
- Volume da demo (dezenas de chunks) está muito abaixo de qualquer limite do pgvector.

**Negativas**

- Em escala de milhões de vetores, um banco especializado renderia melhor.
- Busca puramente semântica erra em consultas de correspondência exata.

**Decisão complementar**

Sem chave de modelo configurada, um `EmbeddingModel` lexical determinístico de 384 dimensões é
registrado para manter a demo executável offline e os testes reprodutíveis. Trocar de modelo exige
ajustar `dimensions` e reindexar.
