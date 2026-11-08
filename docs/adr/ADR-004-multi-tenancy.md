# ADR-004: Isolamento multi-tenant por organização

## Contexto

Usuários pertencem a organizações e os recursos de negócio devem ser isolados entre tenants. A identidade do tenant não pode depender de parâmetros enviados pelo cliente.

## Decisão

- Toda requisição autenticada carrega `org_id` no JWT.
- O `CurrentUserProvider` expõe `userId`, `organizationId`, `role` e `email` a partir do `SecurityContext`.
- Consultas de recursos do tenant usam obrigatoriamente o `organizationId` do token, nunca um identificador fornecido pelo cliente.
- Acesso a recursos de outra organização retorna 404 (`ResourceNotFoundException`) para não revelar a existência do recurso. O 403 fica reservado a violações de RBAC.
- O RBAC usa authorities `ROLE_<role>` e `@PreAuthorize` nos casos de uso expostos.

## Consequências

- O tenant é derivado exclusivamente de um token assinado.
- Endpoints futuros de shipments e pagamentos devem filtrar por `organizationId`.
- Testes de isolamento precisam cobrir múltiplas organizações para evitar regressões.
- Não há suporte a um usuário pertencente a várias organizações neste momento.
