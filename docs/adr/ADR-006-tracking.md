# ADR-006: Motor de rastreamento da Fase 4

## Contexto

A Fase 4 exige histórico cronológico de eventos por remessa, registro idempotente de eventos externos e atualização do ciclo de vida da remessa a partir desses eventos, sem quebrar o isolamento multi-tenant já existente.

## Decisão

- Persistir eventos em `tb_tracking_events`, com `shipment_id`, tipo, local opcional, timestamp do evento, origem, identificador externo opcional e `created_at`.
- Garantir idempotência por meio do índice único parcial `(shipment_id, external_event_id)`. Quando o `external_event_id` já existe para a remessa, o evento existente é retornado sem nova gravação e o endpoint responde `200`; uma criação nova responde `201`.
- Ordenar o histórico por `event_timestamp` e, como desempate, por `created_at`. O `event_timestamp` enviado pelo cliente é preservado exatamente como recebido.
- Resolver o status alvo a partir do tipo do evento em `TrackingStatusResolver`. Eventos operacionais levam a `IN_TRANSIT`, `DELIVERED` leva a `DELIVERED` e `CANCELLED` leva a `CANCELLED`; `OTHER` não altera o status.
- Reutilizar `ShipmentStateMachine` para aplicar a transição. Se o status alvo for igual ao atual, nada é alterado. Transições inválidas lançam `ConflictException` com código `INVALID_TRACKING_STATUS_TRANSITION` e o evento não é persistido.
- Derivar `organization_id` exclusivamente do token JWT. Remessa de outro tenant resulta em `404` para registro, listagem e consulta pública de rastreio.

## Consequências

- Integrações podem reenviar eventos com segurança usando `external_event_id`.
- O histórico reflete a ordem cronológica informada, independentemente da ordem de chegada das requisições.
- O status da remessa é consistente com as regras já existentes de ciclo de vida.
- A consulta pública de rastreio permanece restrita à organização autenticada.
