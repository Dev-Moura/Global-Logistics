# ADR-002: Organização em camadas por feature

## Contexto

A estrutura inicial concentrava controller, service, repository e modelo em um único pacote de domínio, além de configurações globais dispersas. Isso dificultava a evolução do MVP e a separação de responsabilidades.

## Decisão

Organizar o código por feature e por camada:

- `shipment.domain`: entidades e enums.
- `shipment.application`: regras de negócio e casos de uso.
- `shipment.infrastructure`: persistência.
- `shipment.api`: endpoints HTTP.
- `common.*`: contratos transversais de API, domínio, observabilidade e segurança.

Controllers dependem apenas de serviços de aplicação, nunca diretamente de repositórios.

## Consequências

- Fronteiras explícitas entre transporte HTTP, regra de negócio e persistência.
- Maior facilidade para testar serviços e controllers isoladamente.
- Novas features seguem o mesmo formato de pacotes.
- A separação adiciona algum boilerplate de mapeamento entre camadas quando os contratos divergirem das entidades.
