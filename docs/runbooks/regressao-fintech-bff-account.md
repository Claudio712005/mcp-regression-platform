# Runbook: regressão do fintech-bff-account

Pré-condições para iniciar a regressão do `fintech-bff-account`:

1. `fintech-srv-account` respondendo HTTP 200 no health check, com latência abaixo de 800ms.
2. `fintech-db` acessível, com a query de sonda `select 1` respondendo.
3. Contrato `account-api` compatível com o contrato esperado versionado na plataforma.
4. Suíte de smoke tests aprovada: authentication, account lookup, account status e account statement.

Sequência recomendada de verificação: `get_bff_dependencies`, `check_dependency_health`,
`validate_service_contract`, `run_smoke_test` e por fim `get_regression_status`.

Se o status final for BLOCKED, não inicie a regressão. Corrija a dependência apontada e repita o ciclo.
