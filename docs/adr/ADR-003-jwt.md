# ADR-003: Autenticação JWT com access e refresh tokens

## Contexto

A API precisa de autenticação stateless e compatível com múltiplos clientes, sem sessão de servidor. O MVP anterior usava HTTP Basic com um usuário estático, o que não atende RBAC nem expiração controlada de credenciais.

## Decisão

- Emitir access tokens JWT assinados com HMAC-SHA256 (HS256) via `NimbusJwtEncoder`, com claims `sub`, `org_id`, `role`, `email`, `jti`, `iss`, `aud`, `iat` e `exp`.
- Validar tokens com `NimbusJwtDecoder` usando a mesma secret e validação de issuer e audience.
- Emitir refresh tokens opacos de 32 bytes (`SecureRandom`, base64url) e persistir apenas o hash SHA-256 em `tb_refresh_tokens`.
- Rotacionar o refresh token a cada uso. A detecção de reuso de token revogado revoga toda a família de tokens do usuário.
- Configurar TTLs por `app.jwt.access-ttl` (padrão 15m) e `app.jwt.refresh-ttl` (padrão 7d).
- Exigir secret com pelo menos 32 bytes e nunca registrar tokens ou senhas em logs.

## Consequências

- A API deixa de manter estado de sessão e passa a depender apenas do JWT para autorização.
- A rotação de refresh reduz a janela de uso de credenciais vazadas.
- O logout revoga o refresh token informado; access tokens continuam válidos até expirar.
- A secret precisa ser provisionada com segurança em cada ambiente.
