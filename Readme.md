# Notify

Event-driven notification service for multiple products and tenants.

## Local API Key Creation

Use the internal CLI command to create tenant API keys. The raw key is printed once and only its SHA-256 hash is stored in Postgres.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="admin:create-api-key --tenant=campuscritique --name=campuscritique-production --spring.main.web-application-type=none"
```

Use the generated key from a client with:

```http
X-Notify-Api-Key: notify_live_...
```

Do not commit raw API keys. If a key is exposed, revoke it in `tenant_api_keys` and create a new one.

## Security Baseline

- Client APIs require `X-Notify-Api-Key`.
- Tenant keys are scoped to one tenant.
- API keys are stored as hashes, not plaintext.
- Request validation rejects malformed tenant and event identifiers.
- Event ingestion has a configurable body-size guard.
- Default error responses avoid stack traces and internal exception details.
