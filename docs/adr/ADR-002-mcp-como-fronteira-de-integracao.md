# ADR-002: MCP como fronteira de integração, não como núcleo

## Status

Aceito.

## Contexto

A forma mais rápida de construir um MCP Server é colocar a lógica dentro das classes de Tool. Isso
funciona em um exemplo e falha em um sistema: a regra fica presa ao protocolo, não é reutilizável por
REST ou CLI, e não é testável sem o servidor MCP.

## Decisão

Tratar o MCP como um adaptador de entrada. Toda Tool segue:

```text
MCP Tool -> McpSecurityGate -> Application Use Case -> Domain -> Port -> Adapter
```

As classes MCP fazem quatro coisas: validar entrada, autorizar, delegar e mapear a resposta.

## Consequências

**Positivas**

- A mesma capacidade é servida por MCP, REST e CLI sem duplicação.
- O motor de regressão é testado sem levantar o protocolo.
- Trocar a versão do MCP afeta apenas a camada de infraestrutura.

**Negativas**

- Existe uma camada de mapeamento (`ToolViewMapper`) entre domínio e resposta MCP.

**Justificativa da camada extra**

O mapeamento é onde a fronteira de confiança fica visível: é ele que suprime o texto de passagens em
quarentena e que anexa o aviso de conteúdo não confiável na resposta.
