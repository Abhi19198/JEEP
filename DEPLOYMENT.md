# Production Deployment

## Required secrets

Copy `.env.example` to a secret-managed environment file. Replace every placeholder before deployment. Do not commit the real file.

`JEEP_SECURITY_JWT_SECRET` must contain at least 32 random characters. `JEEP_CORS_ALLOWED_ORIGINS` must be the exact HTTPS frontend origin, without a trailing slash.

## Build and release

CI runs backend tests, frontend lint/build, container builds, and Trivy filesystem scanning. Publish immutable images tagged with the commit SHA, then set `BACKEND_IMAGE` and `FRONTEND_IMAGE` to those immutable references in the deployment environment.

## Deploy

1. Point DNS for `APP_DOMAIN` at the host.
2. Ensure ports 80 and 443 are reachable; Caddy obtains and renews the TLS certificate automatically.
3. Run `docker compose -f docker-compose.prod.yml up -d`.
4. Confirm `https://APP_DOMAIN/` and the backend readiness status through the frontend proxy.

Flyway runs migrations on backend startup. Review migrations in a release before deployment and never use Hibernate schema auto-update in production.

## Backups and restore tests

Create a backup with `ops/backup-postgres.sh`. Store the resulting dump outside the host and encrypt it at rest. On a disposable PostgreSQL instance, restore with `ops/restore-postgres.sh path/to/backup.dump`, then run application smoke tests. Perform this restore test at least monthly.

## Rollback

Set `BACKEND_IMAGE` and `FRONTEND_IMAGE` back to the previous successful commit-SHA image references and run `docker compose -f docker-compose.prod.yml up -d`. Database migrations must be backward-compatible with the previous application for an image rollback to be safe.

## Monitoring

The backend exposes health, metrics, and Prometheus endpoints through Actuator. Keep Actuator behind the private backend network or an authenticated monitoring gateway; ship container logs to the platform log collector and alert on readiness failures, restart loops, 5xx rates, latency, database disk usage, and backup age.