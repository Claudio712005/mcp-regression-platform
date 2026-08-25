# Runbook: contrato incompatível

Sintoma: a validação de contrato aponta MISSING_ENDPOINT, METHOD_NOT_SUPPORTED,
MISSING_REQUIRED_PARAMETER, INCOMPATIBLE_SUCCESS_STATUS ou INCOMPATIBLE_RESPONSE (contract mismatch).

Causas prováveis: deploy de uma versão do serviço mais nova ou mais antiga que a esperada pelo BFF,
remoção de campo de resposta sem período de convivência, mudança de parâmetro obrigatório.

Ações: comparar a versão publicada em `/v3/api-docs` com o contrato esperado versionado na plataforma,
identificar o campo ou endpoint divergente, alinhar com o time provedor do `fintech-srv-account`.

Impacto na regressão: bloqueio. A suíte passaria a validar um comportamento que não corresponde ao
contrato acordado, gerando falsos positivos e falsos negativos.
