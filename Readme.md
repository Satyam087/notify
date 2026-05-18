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

## Delivery Workers

Notify creates delivery jobs from tenant notification rules, then a scheduled delivery worker processes due jobs asynchronously.

- In-app notifications are delivered into the Notify database.
- Email delivery is implemented through SMTP, but it is disabled by default until credentials are configured.
- Delivery attempts are logged for every job, including provider name, attempt number, success/failure, and error message.
- Retryable failures are retried with a configurable backoff. Non-retryable failures are marked failed immediately.

Useful delivery settings:

```yaml
notify:
  delivery:
    enabled: true
    fixed-delay-ms: 5000
    batch-size: 50
    max-attempts: 3
    retry-backoff-seconds: 60
```

## Email Configuration

For now, CampusCritique uses the single company mailbox for both sender and replies:

```txt
connect@campuscritique.in
```

Email sending remains off unless `NOTIFY_EMAIL_ENABLED=true` is set.

Recommended provider for the first production setup is Resend SMTP. Use a Resend API key as the SMTP password.

Required production environment variables for Resend:

```bash
NOTIFY_EMAIL_ENABLED=true
NOTIFY_EMAIL_FROM=connect@campuscritique.in
NOTIFY_EMAIL_FROM_NAME=CampusCritique
NOTIFY_EMAIL_REPLY_TO=connect@campuscritique.in

SPRING_MAIL_HOST=smtp.resend.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=resend
SPRING_MAIL_PASSWORD=your-resend-api-key
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
```

Until these SMTP variables are configured, keep `NOTIFY_EMAIL_ENABLED=false` so only non-email channels such as in-app delivery are processed. If email is enabled without the required SMTP settings, Notify fails startup with a clear configuration error.

## Template Management

Templates are stored in Postgres per tenant. Delivery jobs render the matching enabled template for the event type and channel before workers send the notification.

List templates:

```bash
curl "http://localhost:8080/api/v1/templates?tenantId=campuscritique" \
  -H "X-Notify-Api-Key: notify_live_..."
```

Create or update a template:

```bash
curl -X PUT "http://localhost:8080/api/v1/templates/campuscritique_connect_requested_email_v1" \
  -H "Content-Type: application/json" \
  -H "X-Notify-Api-Key: notify_live_..." \
  -d '{
    "tenantId": "campuscritique",
    "eventType": "connect.requested",
    "channel": "EMAIL",
    "subjectTemplate": "New connect request for {{collegeName}}",
    "bodyTemplate": "A student requested a connect for {{collegeName}}.",
    "enabled": true
  }'
```

Preview rendering:

```bash
curl -X POST "http://localhost:8080/api/v1/templates/campuscritique_connect_requested_email_v1/render-test" \
  -H "Content-Type: application/json" \
  -H "X-Notify-Api-Key: notify_live_..." \
  -d '{
    "tenantId": "campuscritique",
    "payload": {
      "collegeName": "Newton ADYPU"
    }
  }'
```

## Event Status Lookup

Clients can check event delivery status with the same tenant API key used for ingestion:

```bash
curl "http://localhost:8080/api/v1/events/status?tenantId=campuscritique&idempotencyKey=example-001" \
  -H "X-Notify-Api-Key: notify_live_..."
```

The response includes the event status, generated delivery jobs, and delivery attempts for each channel.
