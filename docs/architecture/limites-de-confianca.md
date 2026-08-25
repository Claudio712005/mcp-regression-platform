# Limites de confiança

O LLM e o MCP Host são componentes não confiáveis. Tudo que chega através do MCP é tratado como entrada
hostil até passar pela fronteira de segurança da plataforma.

A fronteira é composta por três etapas em sequência: autenticação do token JWT, autorização por capability
e validação de política de entrada. Somente depois dessas etapas o caso de uso da camada de aplicação é
executado.

O conteúdo recuperado pelo RAG também é não confiável. Ele é rotulado como `UNTRUSTED`, envolvido em
marcadores `<untrusted-data>` e submetido ao filtro de prompt injection antes de chegar ao modelo.

Regra fundamental: o LLM nunca é autoridade de segurança. Ele pode solicitar uma ação; quem decide se a
ação é permitida é a plataforma.
