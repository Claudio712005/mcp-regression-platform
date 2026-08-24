# ADR-004: O LLM não controla segurança nem classificação

## Status

Aceito.

## Contexto

Um agente com acesso a ferramentas parece um bom lugar para colocar julgamento: "decida se o ambiente
está pronto", "verifique se este usuário pode executar isso". É um erro. O LLM é influenciável por
qualquer texto que entre no contexto, inclusive documentos recuperados e respostas de serviços.

## Decisão

Manter no código, deterministicamente:

- autenticação e autorização;
- classificação de estado de saúde (HTTP 5xx, 401/403, timeout, latência);
- comparação de contrato;
- avaliação de smoke tests;
- classificação de prontidão;
- decisão de quais estágios executar.

Reservar ao LLM: explicação, correlação, interpretação do conhecimento recuperado e recomendação de
investigação.

## Consequências

**Positivas**

- O mesmo ambiente sempre produz o mesmo veredito, com ou sem LLM configurado.
- Prompt injection não altera decisão de acesso nem status de prontidão.
- Os cenários de regressão são testáveis sem chamar um modelo.

**Negativas**

- Regras determinísticas precisam ser mantidas quando o domínio muda.
- O agente é menos "autônomo" do que uma demo que delega tudo ao modelo.

**Reforço em runtime**

`ModelOutputGuard` descarta saída que contradiz o status calculado, que vaza o prompt de sistema ou que
carrega payload de injeção. Descartada a saída, a plataforma responde com o template determinístico e
sinaliza a origem em `narrativeSource`.
