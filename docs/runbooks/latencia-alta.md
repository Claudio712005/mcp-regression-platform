# Runbook: latência alta

Sintoma: `fintech-srv-account` responde HTTP 200, porém com latência acima do limite de alerta
(high latency), classificando a dependência como DEGRADED.

Causas prováveis: base de dados do serviço sem aquecimento, ambiente compartilhado com carga concorrente,
consulta sem índice em massa de dados de teste, contenção de pool de conexões.

Ações: repetir a medição para descartar ruído, verificar carga concorrente no ambiente,
avaliar se o limite configurado corresponde ao SLA do ambiente de teste.

Impacto na regressão: WARNING. A regressão pode ser executada, mas testes sensíveis a tempo podem
apresentar instabilidade. Registre a latência observada no relatório da execução.
