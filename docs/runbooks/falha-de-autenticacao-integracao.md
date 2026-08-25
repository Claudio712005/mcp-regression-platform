# Runbook: falha de autenticação na integração

Sintoma: `fintech-srv-account` responde HTTP 401 ou HTTP 403 para as chamadas da plataforma
(authentication failure).

Causas prováveis: chave de serviço expirada ou rotacionada, variável de ambiente não propagada para o
container da plataforma, ambiente de destino diferente do ambiente que emitiu a credencial.

Ações: verificar se a variável `PLATFORM_ACCOUNT_SERVICE_API_KEY` está preenchida no ambiente da
plataforma, confirmar a rotação da chave com o time provedor, reemitir a credencial se necessário.

Impacto na regressão: bloqueio. Sem credencial aceita não é possível distinguir uma falha funcional
de uma falha de autorização.
