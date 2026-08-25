# Contrato account-api

O contrato esperado do `fintech-srv-account` é versionado em `src/main/resources/contracts/`.

Operações validadas:

- `GET /accounts/{accountId}` com resposta contendo `id`, `status`, `balance` e `currency`.
- `GET /accounts/{accountId}/status` com resposta contendo `status`.
- `GET /accounts/{accountId}/statement` com parâmetro obrigatório `from` e resposta contendo `entries`.

A comparação verifica existência do endpoint, método suportado, parâmetros obrigatórios,
status de sucesso e campos de resposta. Não é um diff completo de OpenAPI: é uma verificação
suficiente para decidir se a regressão pode começar.
