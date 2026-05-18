# Notify Deployment Checklist

This service is designed to run as one Docker web service connected to managed Postgres, CloudAMQP RabbitMQ, and Resend SMTP.

Do not commit real secrets. Configure these values only in the hosting provider's environment variable panel.

## Required Runtime

- Java 21 runtime, supplied by the Docker image.
- One Postgres database.
- One RabbitMQ broker.
- One Resend SMTP API key if email delivery is enabled.

## Required Environment Variables

```bash
SPRING_PROFILES_ACTIVE=prod
PORT=8080

SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME=database_user
SPRING_DATASOURCE_PASSWORD=database_password

CLOUDAMQP_URL=amqps://user:password@host/vhost

NOTIFY_ALLOWED_ORIGINS=https://your-client-domain.com
```

## Reliability Environment Variables

The defaults are safe for the first deployment. Override them only if the hosted
environment needs different polling windows.

```bash
NOTIFY_DELIVERY_ENABLED=true
NOTIFY_DELIVERY_FIXED_DELAY_MS=5000
NOTIFY_DELIVERY_BATCH_SIZE=50
NOTIFY_DELIVERY_MAX_ATTEMPTS=3
NOTIFY_DELIVERY_RETRY_BACKOFF_SECONDS=60

NOTIFY_EVENT_RECOVERY_ENABLED=true
NOTIFY_EVENT_RECOVERY_FIXED_DELAY_MS=60000
NOTIFY_EVENT_RECOVERY_STALE_AFTER_SECONDS=120
NOTIFY_EVENT_RECOVERY_BATCH_SIZE=50
```

The event recovery scheduler republishes old `QUEUED` events if the database
commit succeeded but RabbitMQ publishing failed. Delivery is claimed and
finalized per job, so one failed job cannot roll back a whole batch of already
sent emails.

## Email Environment Variables

Keep email disabled until all SMTP values are configured.

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

## Build Locally

```bash
docker build -t notify:local .
```

## Run Locally With Docker Image

Use local Docker Compose for Postgres and RabbitMQ first:

```bash
docker compose up -d
```

Then run the image with local service URLs:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:55432/notify \
  -e SPRING_DATASOURCE_USERNAME=notify \
  -e SPRING_DATASOURCE_PASSWORD=notify \
  -e CLOUDAMQP_URL=amqp://notify:notify@host.docker.internal:5672 \
  -e NOTIFY_EMAIL_ENABLED=false \
  notify:local
```

## Health Checks

Use these endpoints for hosting health checks:

```txt
/actuator/health
/actuator/health/liveness
/actuator/health/readiness
```

## First Production Boot

1. Deploy the Docker image with `SPRING_PROFILES_ACTIVE=prod`.
2. Confirm `/actuator/health` returns `UP`.
3. Run the internal API-key CLI against the production database to create the first CampusCritique key.
4. Store the raw API key in the CampusCritique hosting provider as a secret.
5. Send one test `connect.requested` event.
6. Confirm `/api/v1/events/status` shows `SENT` for expected channels.

Start with one running app instance for the first client. The current worker
uses per-job database locks and safe status checks, but horizontal worker
scaling should still be validated with load tests before adding multiple
instances.

## Rollback

If deployment fails:

1. Revert to the previously deployed image or commit.
2. Keep Postgres and CloudAMQP untouched.
3. Check Flyway logs. Do not manually edit schema history unless the migration failure is fully understood.
4. Keep `NOTIFY_EMAIL_ENABLED=false` while debugging SMTP issues.
