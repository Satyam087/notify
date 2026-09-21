# Notify

Multi-tenant notification service: a product posts an event, Notify turns it into one job per channel from the tenant's rules, renders the tenant's template, and delivers in-app, email and push with retries, recovery and a full attempt log. Java 21, Spring Boot 3.5, RabbitMQ, PostgreSQL, Flyway. In production since May 2026.

Case study and numbers: [satyamkumarsingh.com/work/notify](https://satyamkumarsingh.com/work/notify) · design write-up: [What 1,346 notification jobs taught me about async delivery](https://satyamkumarsingh.com/writing/1346-notification-jobs) · live reliability ledger: [satyamkumarsingh.com/contact#ledger](https://satyamkumarsingh.com/contact#ledger)

## Measured in production

| Metric | Value | Source |
|---|---|---|
| Delivery success | 98.1% (1,321 of 1,346 jobs) | `notification_jobs`, production Postgres, 14 Sep 2026 |
| In-app success | 100% (712 of 712) | same query, `channel = IN_APP` |
| Retries needed | 32 across 1,378 attempts | `notification_delivery_attempts`, 14 Sep 2026 |
| Events ingested | 712 since 18 May 2026 | `notification_events`, 14 Sep 2026 |

The three tenants are CampusCritique (Connect bookings, reminders, refunds, payouts), the contact form on satyamkumarsingh.com, and the contact form on Nimit Jain's portfolio.

## How it works

```
POST /api/v1/events  --X-Notify-Api-Key-->  notification_events (RECEIVED -> QUEUED, commit)
        |                                            |
        |                                   afterCommit: publish to RabbitMQ (notify.events)
        |                                            |   publish failed? recovery sweep republishes
        |                                            v   QUEUED events older than 120 s, every 60 s
        |                                   consumer: tenant rules -> one job per channel,
        |                                   template rendered at creation (idempotent per event+channel)
        |                                            |
        v                                            v
   202 { status: QUEUED }               delivery worker every 5 s, batch 50:
                                        claim (FOR UPDATE) -> handler with 20 s timeout -> record attempt
                                        retryable? -> next attempt in 60 s, max 3   else FAILED
                                        channels: in-app (stored here), email (Resend), push (Firebase, VAPID)
```

Design decisions, in one line each:

- **Commit, then publish, then sweep.** The event row is the outbox; a stale `QUEUED` row is the signal to republish. No distributed transaction.
- **Idempotent at every hop.** Idempotency key at ingest; `existsByEventIdAndChannel` plus a unique constraint at fan-out; row lock at claim.
- **Claim and finalise one job at a time**, each in its own transaction, so one failure cannot roll back deliveries that already happened.
- **Failures are classified by the handler** (`DeliveryException(message, retryable)`); the worker retries exactly what can change on a retry.
- **Off by default.** Email and push stay disabled until credentials exist; misconfiguration fails startup, not delivery at 2am.
- **Templates in Postgres per tenant**, versioned by Flyway migration, with a `render-test` endpoint.

Endpoints under `/api/v1`: `events` (ingest, status), `jobs/failed`, `templates` (CRUD, render-test), `in-app-notifications` (list, unread count, read), `push-subscriptions`, `metrics` (totals, per channel, per day, median ingest-to-delivered). Health at `/actuator/health`.

## Deployment

See `docs/deployment.md` for Docker deployment, production environment variables, health checks, and rollback notes.

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

## Email Sender Overrides

Optional payload fields read by the email channel, alongside `emailHtml`:

- `fromName`: display name for the sender. The address stays `NOTIFY_EMAIL_FROM`; `< > "` and line breaks are stripped, 80 characters max.
- `replyTo`: address replies go to, used only when it parses as an email; otherwise `NOTIFY_EMAIL_REPLY_TO` applies.

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

## Delivery Metrics

Tenant-scoped delivery counts for dashboards and status pages. Volumes only, never content or recipients.

```bash
curl "http://localhost:8080/api/v1/metrics?tenantId=portfolio&days=30" \
  -H "X-Notify-Api-Key: notify_live_..."
```

The response carries `tenant` totals (events, jobs, sent, failed, pending, attempts, successRate, medianDeliveryMs), `byChannel`, and `byDay` for the requested window. Tenants listed in `NOTIFY_METRICS_PLATFORM_TENANTS` (default `portfolio`) also receive a `platform` block with totals and a per-channel breakdown across all tenants.

## Failed Job Visibility

Clients can inspect recent failed jobs for their tenant with the same API key:

```bash
curl "http://localhost:8080/api/v1/jobs/failed?tenantId=campuscritique&limit=25" \
  -H "X-Notify-Api-Key: notify_live_..."
```

The response includes the failed job id, event id, channel, rendered subject, latest provider, and latest error message.
