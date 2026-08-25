# Política de regressão

A classificação de prontidão é determinística e possui três estados.

`READY_FOR_REGRESSION` significa que nenhuma evidência de bloqueio ou de degradação foi encontrada.

`WARNING` significa que não existe bloqueio, mas há degradação. O caso mais comum é latência alta
(high latency) acima do limite de alerta e abaixo do limite de falha.

`BLOCKED` significa que existe pelo menos uma evidência bloqueante. Uma dependência crítica indisponível,
uma falha de autenticação na integração, uma incompatibilidade de contrato ou um smoke test reprovado
produzem bloqueio.

O LLM não pode alterar a classificação. Ele explica a causa e recomenda a investigação.
