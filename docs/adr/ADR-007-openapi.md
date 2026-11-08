# ADR-007: Documentação OpenAPI com springdoc

## Contexto

A API já possui autenticação JWT, múltiplos módulos e endpoints versionados em `/api/v1`. Consumidores precisam de um contrato legível e sempre atualizado, sem manter documentação paralela ao código.

## Decisão

- Adotar `springdoc-openapi-starter-webmvc-ui` na versão `2.6.0`, compatível com Spring Boot 3.3.x e declarada explicitamente por não ser gerenciada pelo BOM do Boot.
- Definir o documento em `OpenApiConfig`, com título, versão, descrição, contato e o security scheme `bearerAuth` do tipo HTTP/Bearer/JWT.
- Aplicar `bearerAuth` como requisito global de segurança, mantendo os endpoints públicos documentados e liberados no `SecurityConfig`.
- Expor o JSON em `/v3/api-docs` e a interface em `/swagger-ui.html`.
- Anotar os controllers com `@Tag` e `@Operation` para agrupar e resumir as operações principais, sem duplicar a modelagem dos DTOs.

## Consequências

- A documentação é gerada a partir do código e acompanha a evolução dos controllers.
- O Swagger UI e o JSON ficam acessíveis sem token, enquanto as operações protegidas continuam exigindo JWT.
- A versão do springdoc precisa ser acompanhada manualmente em atualizações do Spring Boot.
- Detalhes de contrato além dos resumos das operações permanecem responsabilidade dos DTOs e dos testes de integração.
