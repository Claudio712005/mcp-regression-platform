# Modelo de autorização

A plataforma usa OAuth2/JWT. O token carrega apenas o subject e as roles. As capabilities nunca vêm do
token: elas são derivadas das roles pelo servidor, o que impede que um token forjado ou manipulado
conceda capacidades adicionais.

Roles e capacidades:

- `DEV`: leitura de dependências, health check, validação de contrato, smoke test e busca no conhecimento.
- `QA`: tudo de DEV mais execução de regressão.
- `ARCHITECT`: tudo de DEV mais recursos de arquitetura e análise avançada.

Cada Tool e cada Resource MCP possui uma classificação (READ, VALIDATION, EXECUTION) e exige uma
capability específica. Um validador de inicialização recusa subir a aplicação se alguma Tool ou Resource
MCP não estiver declarada no registro de autorização.
