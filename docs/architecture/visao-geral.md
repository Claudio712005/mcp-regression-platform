# Visão geral da plataforma

A `mcp-regression-platform` é um monólito modular que responde a uma pergunta operacional:
o ambiente está pronto para executar uma regressão de um BFF?

A plataforma expõe três interfaces: o MCP Server (Tools, Resources e Prompts), uma API técnica REST
e a CLI `demo.sh`. Todas as interfaces chamam os mesmos casos de uso da camada de aplicação.

O BFF observado na demonstração é o `fintech-bff-account`, que depende do serviço HTTP
`fintech-srv-account` e do banco relacional `fintech-db`.

O motor de regressão executa estágios determinísticos: descoberta de dependências, health check,
validação de segurança das integrações, validação de contrato, smoke tests e recuperação de conhecimento.
Somente depois disso o LLM é acionado, e apenas para explicar as evidências já classificadas.
