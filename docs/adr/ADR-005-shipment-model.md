# ADR-005: Modelo de remessa da Fase 3

## Contexto

A remessa original guardava apenas países de origem/destino, peso, distância, seguro e custo em colunas soltas na própria entidade. A documentação v0.2.0 exige locais estruturados, transportadoras, janelas de tempo, versionamento e isolamento por organização.

## Decisão

- Modelar `Location` e `Carrier` como entidades próprias (`tb_locations` e `tb_carriers`) e referenciá-las em `Shipment` por `origin_id`, `destination_id` e `carrier_id`.
- Agrupar os valores de frete em `FreightDetails` (`@Embeddable`) mapeado para as colunas `weight`, `distance`, `insurance_value`, `shipping_cost` e `currency`.
- Manter a fórmula de frete existente em `FreightCalculator`: peso × 5 + distância × 0,5 + seguro × 0,01, com `HALF_UP` e escala 2 em BRL.
- Representar o ciclo de vida em `ShipmentStateMachine`: `PENDING` pode ir para `IN_TRANSIT` ou `CANCELLED`; `IN_TRANSIT` pode ir para `DELIVERED` ou `CANCELLED`; os demais estados são terminais. Cancelar uma remessa em trânsito exige `SYSTEM_ADMIN` ou `LOGISTICS_MANAGER`.
- Derivar `organization_id` exclusivamente do JWT. Toda leitura e escrita de remessa usa `organizationId` do token, e recurso de outro tenant retorna 404.
- Expor DTOs de API e paginação (`PageResponse`) mantendo a entidade fora do contrato HTTP.
- Versionar o agregado com `@Version` para detectar atualizações concorrentes.

## Consequências

- A API passa a ter contratos explícitos de criação, atualização, consulta e mudança de status.
- A unicidade de `tracking_number` é por organização, e não global.
- As colunas legadas `origin_country` e `destination_country` permanecem no banco apenas por compatibilidade, sem mapeamento na entidade.
- O isolamento multi-tenant depende de todas as consultas de remessa filtrarem pelo `organizationId` do token.
