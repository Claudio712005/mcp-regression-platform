# Runbook: fintech-srv-account indisponível

Sintoma: `fintech-srv-account` responde HTTP 503 ou HTTP 500, ou não responde (UNAVAILABLE, service down).

Causas prováveis, em ordem de frequência:

1. Deploy em andamento ou pods sem réplicas prontas no ambiente de teste.
2. Dependência interna do próprio serviço fora do ar, propagando 503.
3. Circuit breaker aberto por acúmulo de erros anteriores.
4. Configuração de rota apontando para um ambiente errado.

Ações: confirmar o status do deploy do serviço, checar o health do serviço diretamente,
verificar se o endpoint configurado na plataforma aponta para o ambiente correto.

Impacto na regressão: bloqueio total. A suíte de regressão do `fintech-bff-account` exercita
o serviço de contas em praticamente todos os cenários.
