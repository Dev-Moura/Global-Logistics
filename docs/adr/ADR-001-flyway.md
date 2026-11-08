# ADR-001: Uso do Flyway para versionamento do schema

## Contexto

O projeto usa PostgreSQL e Hibernate. O MVP precisa de um schema reproduzível entre ambientes, com histórico de mudanças confiável e sem alterações automáticas de tabelas em produção.

## Decisão

Adotar Flyway com migrações SQL versionadas em `classpath:db/migration` e usar `spring.jpa.hibernate.ddl-auto=validate`. O schema inicial é definido em `V1__baseline_schema.sql`.

## Consequências

- O schema passa a ser controlado por migrações versionadas e revisáveis.
- A aplicação falha rápido se a entidade divergir do schema validado pelo Hibernate.
- Toda mudança de schema exige uma nova migração, incluindo ambientes de desenvolvimento.
- O baseline precisa ser mantido em sincronia com as entidades a partir da Fase 1.
