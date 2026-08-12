# ClientDesk Production Operations

Review date: 2026-08-12

This runbook covers the deployed ClientDesk portfolio environment. It separates current architecture and routine checks from operational assurance work that cannot be proven by source code alone.

## Service Inventory

| Component | Provider | Production responsibility |
| --- | --- | --- |
| Frontend | Vercel | Angular application, browser-security headers, SPA fallback, and same-origin `/api/*` rewrite |
| Backend | Render free web service | Spring Boot API and health endpoint |
| Database | Neon PostgreSQL 16 | Application data and Flyway schema history |
| Attachments | Cloudinary | Authenticated raw attachment assets |
| CI and security | GitHub Actions | Tests, builds, dependency checks, secret scanning, and CodeQL |

Stable public frontend:

```text
https://clientdesk-omega.vercel.app
```

Health endpoint:

```text
https://clientdesk-backend.onrender.com/actuator/health
```

Render cold starts are expected after inactivity. Treat the first slow request as an infrastructure characteristic unless health checks or subsequent requests fail.

## Environment Baseline

The backend runs with the `prod` profile and environment-managed configuration:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=<Neon PostgreSQL JDBC URL with TLS>
SPRING_DATASOURCE_USERNAME=<least-privilege application role>
SPRING_DATASOURCE_PASSWORD=<secret>
SPRING_FLYWAY_USER=<migration role>
SPRING_FLYWAY_PASSWORD=<secret>
FRONTEND_ORIGIN=https://clientdesk-omega.vercel.app
TRUSTED_PROXY_IP_PATTERN=<constrained Render proxy pattern>
ATTACHMENT_STORAGE_PROVIDER=cloudinary
CLOUDINARY_CLOUD_NAME=<secret-managed Cloudinary cloud name>
CLOUDINARY_API_KEY=<secret>
CLOUDINARY_API_SECRET=<secret>
CLOUDINARY_FOLDER_PREFIX=clientdesk/production/attachments
AI_ENABLED=false
OPENAI_API_KEY=<unset or blank>
```

The production startup guard fails closed when database credentials are blank, the frontend origin is not an exact HTTPS origin, secure cookies are disabled, session timeout exceeds 30 minutes, proxy trust is unconstrained, or paid AI is enabled without safe provider configuration.

Never combine the repeatable development `demo` seed with the normal production environment.

## Routine Health Checks

### Before a Portfolio Demonstration

1. Open the stable Vercel frontend.
2. If the backend is waking, wait for the health endpoint to become healthy.
3. Verify login and logout for the three portfolio roles.
4. Verify the dashboard and one primary workflow load without 5xx errors.
5. Confirm the assistant reports **Local fallback** while `AI_ENABLED=false`.
6. Confirm attachment downloads require an authorized session.
7. Review recent Render logs for repeated startup, database, authorization, throttling, or API-failure events.

### After a Deployment

1. Confirm Render deployed the intended backend revision after checks passed.
2. Confirm Flyway validation and startup complete without unexpected migrations.
3. Confirm the Vercel stable domain serves the intended frontend revision.
4. Verify the same-origin `/api/*` rewrite reaches Render.
5. Smoke-test authentication, CSRF-protected writes, role boundaries, requests, tasks, quotes, attachments, and the configured AI mode.
6. Confirm production headers remain present on the frontend and API responses.

## Database Roles and Migrations

Use separate credentials for schema migration and application traffic:

- **Provider administrator:** Neon-controlled account used only for role and recovery administration.
- **Migration role:** owns or may alter Flyway-managed schema objects.
- **Application role:** receives only the connection, schema, table, and sequence privileges needed at runtime.
- **Backup role:** optional read-only recovery role when supported by the provider.

Do not use a PostgreSQL superuser, database owner, or migration role as the normal application datasource user.

A provider administrator should adapt and review this least-privilege pattern before applying it:

```sql
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT CONNECT ON DATABASE clientdesk TO clientdesk_app;
GRANT USAGE ON SCHEMA public TO clientdesk_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO clientdesk_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO clientdesk_app;

ALTER DEFAULT PRIVILEGES FOR ROLE clientdesk_migrator IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO clientdesk_app;
ALTER DEFAULT PRIVILEGES FOR ROLE clientdesk_migrator IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO clientdesk_app;
```

Test grant changes and migrations against an isolated database before changing production.

## Backup and Recovery Policy

Database rows and attachment bytes are stored by different providers. A usable recovery point therefore requires both:

- A Neon database backup or logical dump
- A matching Cloudinary asset inventory or recovery copy for the configured production folder

Operational requirements:

- Enable the strongest encrypted Neon backup or point-in-time recovery available for the selected plan.
- Define and record recovery point and recovery time objectives.
- Restrict backup and restore access.
- Never place production dumps in Git or public storage.
- Record Cloudinary public IDs, byte sizes, and creation times without recording API secrets or signed delivery URLs.
- Coordinate database and attachment inventories so metadata and asset bytes describe the same recovery window.
- Keep controlled source copies of fictional portfolio assets when provider-plan recovery features are limited.

Source control cannot verify provider backup retention or restore success. Treat those as operator-owned assurance evidence, not implemented application features.

## Restore Exercise

Perform recovery in isolated, non-production resources:

1. Create an empty private PostgreSQL recovery database.
2. Restore the selected Neon backup or logical dump without overwriting production.
3. Configure a separate Cloudinary recovery folder or verified recovery copy.
4. Start the deployed backend revision with `AI_ENABLED=false`.
5. Confirm Flyway validation succeeds without an unexpected migration.
6. Verify login, organization isolation, request access, quote totals, and attachment downloads.
7. Compare attachment metadata with the Cloudinary recovery inventory.
8. Record timing, missing assets, orphaned assets, and the final result without recording credentials.
9. Destroy temporary recovery resources and remove their secrets securely.

A backup is not considered operationally proven until a restore exercise succeeds. The repository does not claim that provider restore testing is automated.

## Monitoring and Alerts

Monitor:

- Render health-check failures and repeated restarts
- Backend 5xx responses and database connection exhaustion
- Neon storage, connection, and backup status
- Cloudinary storage, credit, and bandwidth usage
- Repeated authentication failures, authorization denials, throttling, and API failures
- OpenAI usage and budget whenever real AI is intentionally enabled

Important structured events include:

```text
auth_login_failed
auth_login_rate_limited
authentication_required
authorization_denied
api_rate_limited
api_failure
```

The current rate limiter and sessions are instance-local. Reassess monitoring and persistence before horizontal scaling.

## Secret Rotation

1. Create the replacement credential in the provider secret manager.
2. Update Render or the affected service without exposing the value in logs or chat.
3. Restart or redeploy the affected component.
4. Verify health and the relevant workflow.
5. Revoke the previous credential.
6. Invalidate application sessions after authentication-related exposure.

Rotate immediately after suspected exposure and follow provider schedules for routine rotation.

## Incident Response

1. Record the time range, affected provider, and deployed revision.
2. Disable the affected integration or public traffic when containment is required.
3. Preserve access-controlled logs without copying customer content into tickets.
4. Rotate exposed credentials and invalidate affected sessions.
5. Review organization access, database changes, attachment access, and AI usage.
6. Restore from a verified recovery point if integrity is uncertain.
7. Patch and test the cause before reopening traffic.
8. Document impact, remediation, and follow-up controls without sensitive data.

## Operational Assurance Status

The repository and deployed application provide evidence for application behavior, production configuration guards, health checks, provider integration, and CI controls. The following require recurring operator or provider evidence and must not be implied as permanently complete by the README:

- Backup retention and encryption status
- Successful database and Cloudinary recovery exercises
- Centralized alert delivery and log-retention settings
- Provider least-privilege grants remaining unchanged
- External dynamic security scanning
- Credential-rotation exercises

Use [SECURITY_LOGGING.md](./SECURITY_LOGGING.md) for event and data-exclusion rules and [SECURITY_BASELINE.md](./SECURITY_BASELINE.md) for the reviewed application security posture.
