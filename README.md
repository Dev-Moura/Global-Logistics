# Global Logistics API

API REST para gestão de logística global: organizações multi-tenant, autenticação JWT, remessas, transportadoras, locais e rastreamento de eventos.

## Visão geral

O projeto é um backend Spring Boot organizado em módulos de negócio, com isolamento por organização, controle de acesso baseado em papéis (RBAC), migrações versionadas e observabilidade via Actuator.

### Stack

- Java 21 (compatível com JDK 25)
- Spring Boot 3.3.4
- Spring Web, Spring Validation e Spring Security (OAuth2 Resource Server com JWT)
- Spring Data JPA e Hibernate
- PostgreSQL 15
- Flyway
- Springdoc OpenAPI / Swagger UI
- Spring Boot Actuator
- JUnit 5, Mockito e Testcontainers
- Maven Wrapper

## Arquitetura

A aplicação segue uma arquitetura em camadas por módulo:

- `api`: controllers, DTOs de entrada/saída e contratos HTTP.
- `application`: serviços de orquestração e regras de aplicação.
- `domain`: entidades, enums, exceções e regras de negócio.
- `infrastructure`: repositórios e integrações com persistência.
- `common`: segurança, tratamento global de erros, configuração, observabilidade e utilitários compartilhados.

### Estrutura de pacotes

```text
src/main/java/com/michael/global_logistics_api/
├── common/
│   ├── api/              # ProblemDetail, paginação e caminhos da API
│   ├── config/           # Configuração OpenAPI
│   ├── domain/           # Exceções compartilhadas
│   ├── observability/    # Correlation ID
│   └── security/         # JWT, RBAC e SecurityConfig
├── identity/             # Registro, login, refresh tokens e usuários
├── organization/         # Organizações e usuários do tenant
├── shipment/             # Ciclo de vida e cálculo de frete
├── tracking/             # Eventos e consulta de rastreamento
├── location/             # Locais de origem e destino
└── carrier/              # Transportadoras
```

As migrações ficam em `src/main/resources/db/migration` e os testes em `src/test/java`.

## Pré-requisitos

- Java 21 ou superior
- Docker
- Docker Compose
- Bash (para os exemplos com `curl`)

Não é necessário instalar Maven: use o wrapper `./mvnw`.

## Configuração

Crie o arquivo de ambiente a partir do exemplo:

```bash
cp .env.example .env
```

Variáveis disponíveis:

| Variável | Descrição | Exemplo |
| --- | --- | --- |
| `DB_NAME` | Nome do banco PostgreSQL | `global_logistics` |
| `DB_USER` | Usuário do banco | `logistics` |
| `DB_PASSWORD` | Senha do banco | `change-me` |
| `APP_JWT_SECRET` | Segredo HMAC do JWT (mínimo de 32 bytes) | `replace-with-a-strong-secret-with-at-least-32-bytes` |
| `APP_JWT_ACCESS_TTL` | Validade do access token | `15m` |
| `APP_JWT_REFRESH_TTL` | Validade do refresh token | `7d` |
| `APP_JWT_ISSUER` | Emissor do token | `global-logistics-api` |
| `APP_JWT_AUDIENCE` | Audiência do token | `global-logistics-client` |

O arquivo `.env` é ignorado pelo Git. O Docker Compose o utiliza automaticamente. Para rodar a aplicação fora do Docker, exporte as variáveis no shell antes de iniciar:

```bash
set -a
source .env
set +a
```

Em produção, nunca use os valores de exemplo. O `APP_JWT_SECRET` deve ser forte e mantido em um gerenciador de secrets.

## Como executar

### 1. Subir apenas o PostgreSQL

```bash
docker compose up -d db
```

O banco ficará disponível em `localhost:5432`.

### 2. Subir a API localmente

Com as variáveis de ambiente exportadas:

```bash
./mvnw spring-boot:run
```

A API ficará disponível em `http://localhost:8080`.

### 3. Subir banco e API com Docker Compose

```bash
docker compose up --build
```

Esse comando constrói a imagem da aplicação, sobe o PostgreSQL, aguarda o banco ficar saudável e inicia a API.

## Migrações Flyway

As migrações ficam em `src/main/resources/db/migration` e são executadas automaticamente na inicialização da aplicação (`spring.flyway.enabled=true`).

- O schema é validado pelo Hibernate com `ddl-auto=validate`.
- Migrações já aplicadas não devem ser alteradas; qualquer mudança de schema exige uma nova migração versionada, por exemplo `V5__add_new_table.sql`.
- O Flyway mantém o histórico na tabela `flyway_schema_history`.

O projeto possui as migrações `V1` a `V4`, incluindo o seed inicial de locais e transportadoras.

## Documentação interativa

Com a aplicação em execução:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Os endpoints de documentação são públicos. O esquema de segurança `bearerAuth` é aplicado globalmente, e as operações protegidas exigem o header `Authorization: Bearer <token>`.

## Endpoints principais

Todos os endpoints de negócio usam o prefixo `/api/v1`.

### Autenticação

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Registra organização e usuário administrador |
| `POST` | `/api/v1/auth/login` | Autentica e emite access e refresh tokens |
| `POST` | `/api/v1/auth/refresh` | Renova os tokens |
| `POST` | `/api/v1/auth/logout` | Revoga o refresh token |
| `GET` | `/api/v1/auth/me` | Retorna o usuário autenticado |

### Organizações

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/organizations` | Cria organização (`SYSTEM_ADMIN`) |
| `GET` | `/api/v1/organizations/me` | Consulta a organização atual |
| `PATCH` | `/api/v1/organizations/me` | Atualiza a organização atual |
| `GET` | `/api/v1/organizations/users` | Lista usuários da organização |

### Remessas

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/shipments` | Cria remessa |
| `GET` | `/api/v1/shipments` | Lista remessas com filtros e paginação |
| `GET` | `/api/v1/shipments/{id}` | Consulta remessa por ID |
| `PATCH` | `/api/v1/shipments/{id}` | Atualiza dados da remessa |
| `PATCH` | `/api/v1/shipments/{id}/status` | Atualiza o status |
| `POST` | `/api/v1/shipments/{id}/cancel` | Cancela a remessa |

### Rastreamento

| Método | Caminho | Descrição |
| --- | --- | --- |
| `POST` | `/api/v1/shipments/{shipmentId}/events` | Registra evento de rastreamento |
| `GET` | `/api/v1/shipments/{shipmentId}/events` | Lista eventos da remessa |
| `GET` | `/api/v1/tracking/{trackingNumber}` | Consulta rastreamento pelo número |

### Locais e transportadoras

| Método | Caminho | Descrição |
| --- | --- | --- |
| `GET` | `/api/v1/locations` | Lista locais |
| `GET` | `/api/v1/locations/{id}` | Consulta local por ID |
| `GET` | `/api/v1/carriers` | Lista transportadoras |
| `GET` | `/api/v1/carriers/{id}` | Consulta transportadora por ID |

### Observabilidade

| Método | Caminho | Acesso |
| --- | --- | --- |
| `GET` | `/actuator/health` | Público |
| `GET` | `/actuator/health/liveness` | Público |
| `GET` | `/actuator/health/readiness` | Público |
| `GET` | `/actuator/info` | Público |
| `GET` | `/actuator/metrics` | Autenticado |

O health não expõe detalhes sensíveis (`show-details=never`). O `info` publica nome, versão e descrição da aplicação.

## Fluxo rápido de uso

O exemplo abaixo mostra o fluxo completo com `curl`. Substitua os valores de exemplo e nunca versionar tokens reais.

### 1. Registrar organização e usuário

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "organizationName": "Demo Logistics",
    "countryCode": "BR",
    "taxIdentifier": "DEMO-001",
    "fullName": "Demo Admin",
    "email": "admin@demo.local",
    "password": "StrongPass123!"
  }'
```

### 2. Autenticar

```bash
ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@demo.local","password":"StrongPass123!"}' \
  | jq -r '.accessToken')
```

### 3. Criar remessa

Os IDs abaixo pertencem ao seed inicial de locais e transportadoras.

```bash
SHIPMENT=$(curl -s -X POST http://localhost:8080/api/v1/shipments \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "originId": "11111111-1111-1111-1111-111111111111",
    "destinationId": "33333333-3333-3333-3333-333333333333",
    "carrierId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "weightKg": 1200.5,
    "distanceKm": 18500,
    "insuredValue": 50000
  }')

SHIPMENT_ID=$(echo "$SHIPMENT" | jq -r '.id')
TRACKING_NUMBER=$(echo "$SHIPMENT" | jq -r '.trackingNumber')
```

### 4. Registrar evento de rastreamento

```bash
curl -s -X POST "http://localhost:8080/api/v1/shipments/$SHIPMENT_ID/events" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "eventType": "PICKED_UP",
    "description": "Coleta realizada",
    "locationId": "11111111-1111-1111-1111-111111111111",
    "eventTimestamp": "2026-01-01T10:00:00Z",
    "source": "MANUAL"
  }'
```

### 5. Consultar rastreamento

```bash
curl -s "http://localhost:8080/api/v1/tracking/$TRACKING_NUMBER" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Os eventos são idempotentes quando informado `externalEventId`, e o status da remessa é atualizado conforme o tipo do evento.

## Testes

Execute a suíte completa:

```bash
./mvnw verify
```

- Testes unitários são executados pelo Surefire.
- Testes de integração (`*IT`) são executados pelo Failsafe.
- Os testes de integração usam Testcontainers para subir um PostgreSQL real, portanto Docker precisa estar disponível.
- Em Docker 29+, o `pom.xml` já define `api.version=1.43` para o Testcontainers.

Para executar apenas os testes unitários:

```bash
./mvnw test
```

O template de pipeline de CI está em `docs/ci/github-actions.yml` e executa `./mvnw -B verify` em Java 21, publicando os relatórios de teste como artefatos. Para ativá-lo, copie o arquivo para `.github/workflows/ci.yml` (requer permissão de workflow no repositório).

## Tratamento de erros

Os erros são retornados como `ProblemDetail` (RFC 9457), com os campos `title`, `status`, `detail`, `code` e `traceId`. O header `X-Correlation-Id` é propagado ou gerado automaticamente para facilitar o rastreamento de requisições.

## Migração de banco legado

Versões anteriores usavam `spring.jpa.hibernate.ddl-auto=update` e não possuíam histórico do Flyway. Ao subir esta versão sobre um volume PostgreSQL antigo, o Flyway recusa um schema não vazio sem `flyway_schema_history`.

Para ambientes de desenvolvimento, recrie o volume:

```bash
docker compose down -v
docker compose up -d db
```

Para dados que precisem ser preservados, faça um dump, recrie o schema vazio, aplique as migrações e reimporte os dados com o mapeamento para `organization_id`. Remessas legadas sem `organization_id` não são acessíveis pela API (todas as consultas são isoladas por tenant).

## Limitações conhecidas do MVP

- `APP_JWT_SECRET` é obrigatório fora do perfil `local`; a aplicação falha ao iniciar sem um segredo de pelo menos 32 bytes.
- O frete usa `weightKg`, `distanceKm` e `insuredValue` enviados pelo cliente; a distância não é derivada das coordenadas das localizações.
- Eventos de tracking concorrentes com o mesmo `externalEventId` podem retornar `409` em vez de `200`; a idempotência sequencial é garantida pelo índice único.
- Logout revoga o refresh token; access tokens continuam válidos até expirarem.
- Documentos, rotas, processamento assíncrono, analytics e integrações reais com transportadoras estão fora do escopo do MVP (fases 5-8 da especificação).

## Branches e roadmap por fases

- `feat/phase-1-foundation`: estrutura base, Flyway, camadas e observabilidade.
- `feat/phase-2-auth-organizations`: autenticação JWT, RBAC e multi-tenancy.
- `feat/phase-3-shipments`: ciclo de vida de remessas, DTOs e paginação.
- `feat/phase-4-tracking`: eventos de rastreamento idempotentes e transições de status.
- `feat/phase-5-quality`: OpenAPI, observabilidade, documentação e CI.
- `fix/mvp-hardening`: correções de segurança, concorrência e integridade pós-revisão.
- Próximas fases: documentos, rotas, processamento assíncrono, analytics e integrações externas.

Cada fase deve manter os testes existentes verdes, adicionar cobertura para o novo comportamento e preservar o isolamento entre organizações.
