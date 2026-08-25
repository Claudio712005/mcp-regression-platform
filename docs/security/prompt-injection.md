# Prompt injection

Prompt injection é uma classe de risco, não um problema resolvido. A plataforma implementa camadas de
mitigação, não uma garantia de invulnerabilidade.

Camadas aplicadas: separação entre instrução e dado, rotulagem de conteúdo não confiável, validação e
sanitização de entradas das Tools, autorização independente do LLM, proteção do system prompt,
fronteira de confiança para Resources e restrição da saída do modelo.

Conteúdo recuperado pelo RAG que contenha sinais de injeção é colocado em quarentena e não é enviado ao
modelo. A saída do modelo é descartada quando tenta vazar o system prompt ou contradizer o status
determinístico calculado pela plataforma.
