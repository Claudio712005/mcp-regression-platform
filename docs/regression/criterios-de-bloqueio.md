# Critérios de bloqueio

HTTP 500 e HTTP 503 do `fintech-srv-account` são interpretados como indisponibilidade (UNAVAILABLE) da
dependência. Como o serviço é crítico para o `fintech-bff-account`, a regressão fica BLOCKED.

HTTP 401 e HTTP 403 são interpretados como falha de autenticação (AUTHENTICATION_FAILURE) entre a
plataforma e a integração. Isso também bloqueia a regressão, porque nenhum smoke test poderá ser confiável.

Timeout de leitura é interpretado como TIMEOUT e bloqueia a regressão.

Divergência de contrato (contract mismatch) bloqueia a regressão mesmo que o serviço esteja disponível,
porque a suíte de regressão passaria a exercitar um contrato diferente do esperado.

Quando uma dependência crítica está bloqueada, o planner pula as etapas de contrato e smoke test.
Executá-las produziria ruído, não informação.
