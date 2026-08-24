# ADR-001: Monólito modular em vez de microsserviços

## Status

Aceito.

## Contexto

A plataforma precisa de várias capacidades — registro de dependências, sondagem de saúde, validação de
contrato, smoke tests, RAG, agente e MCP Server. É tentador quebrar isso em serviços, ainda mais em uma
demonstração cujo domínio é justamente uma arquitetura distribuída.

## Decisão

Implementar a plataforma como um monólito modular, com módulos expressos por pacotes e dependências
dirigidas (`domain <- application <- infrastructure`), aplicando princípios hexagonais sem purismo.

## Consequências

**Positivas**

- Um processo, um deploy, uma transação: a demo sobe com `docker compose up`.
- Refatorar a fronteira entre módulos custa um rename, não um contrato entre serviços.
- Os testes de integração exercitam o sistema inteiro com Testcontainers.

**Negativas**

- Escalar times independentes exigiria extrair módulos.
- Uma falha no processo derruba todas as interfaces (MCP, REST, CLI) ao mesmo tempo.

**Mitigação**

As fronteiras de módulo são explícitas e os ports isolam a infraestrutura, então extrair um módulo para
um serviço próprio é um caminho aberto, não uma reescrita.
