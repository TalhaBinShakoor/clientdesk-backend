# Production Operations

This runbook is the release gate for a production ClientDesk environment. Provider-specific values belong in the hosting platform's secret manager, never in Git, workflow files, build output, or frontend code.

## Environment Baseline

Set and verify these values before starting the backend:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=<private PostgreSQL JDBC URL with TLS enabled>
SPRING_DATASOURCE_USERNAME=<least-privilege application role>
SPRING_DATASOURCE_PASSWORD=<application role password>
SPRING_FLYWAY_USER=<migration role>
SPRING_FLYWAY_PASSWORD=<migration role password>
FRONTEND_ORIGIN=https://<exact frontend host>
TRUSTED_PROXY_IP_PATTERN=<constrained platform proxy IP regex>
ATTACHMENT_STORAGE_ROOT=<private persistent storage path>
AI_ENABLED=false
OPENAI_API_KEY=
```

Keep the default secure session, request-size, upload-quota, authentication-rate-limit, API-rate-limit, and safe-error settings unless an intentional reviewed override is required. Never activate the `demo` profile together with `prod`; production startup rejects known demo identities and data.

Before release:

- Confirm the frontend uses the deployed HTTPS API URL rather than `localhost`.
- Confirm the backend is reachable only through the HTTPS proxy.
- Confirm `FRONTEND_ORIGIN` is one exact HTTPS origin without a path or wildcard.
- Copy the proxy provider's documented internal address range into `TRUSTED_PROXY_IP_PATTERN`; do not use `.*`.
- Keep the database and attachment storage private and inaccessible from the public internet.
- Start with `AI_ENABLED=false`. Add a dedicated production OpenAI key only when paid AI is intentionally enabled.
- Confirm production does not contain the known demo users, organization, or default password.

## Database Roles

Use separate credentials for schema migration and normal application traffic:

- **Deployment administrator:** provider-controlled account used only to create roles, grants, and recovery databases.
- **Migration role:** owns ClientDesk schema objects and may run Flyway DDL. Configure it with `SPRING_FLYWAY_USER` and `SPRING_FLYWAY_PASSWORD`.
- **Application role:** used by the datasource. Grant only `CONNECT`, schema `USAGE`, table `SELECT`, `INSERT`, `UPDATE`, and `DELETE`, plus required sequence privileges.
- **Backup role:** optional provider-supported role with read-only backup access and no application write privileges.

Do not use a PostgreSQL superuser, database owner, or migration role as `SPRING_DATASOURCE_USERNAME`. Restrict every role to the production database and application host where the provider supports network rules.

The deployment administrator should adapt and review this grant pattern for the selected provider:

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

The migration role must own, or be authorized to alter, objects managed by Flyway. Test migrations with the same role split in a disposable environment before production.

## Backup Policy

Back up both PostgreSQL and the attachment storage. Database-only backups are incomplete because attachment metadata is stored in PostgreSQL while file bytes are stored under `ATTACHMENT_STORAGE_ROOT`.

- Enable encrypted automatic database backups with the selected provider.
- Set a documented recovery point objective and recovery time objective before launch.
- Retain at least one backup outside the live database failure domain when the provider supports it.
- Encrypt backups at rest and in transit, and restrict restore/download access.
- Back up private attachment storage on the same schedule.
- Coordinate database and attachment snapshots by pausing attachment writes or using a provider-supported consistent snapshot process.
- Record backup time, retention expiry, encryption status, and restore-test result without recording credentials.
- Never place production dumps in either Git repository or a public storage bucket.

For a provider-independent logical backup, use `pg_dump --format=custom --no-owner --no-acl` with credentials supplied through a protected password file or secret manager. Do not put passwords directly in command history or process arguments.

## Restore Test

Test restoration into an isolated, non-production database before launch and at a regular interval:

1. Create an empty recovery database with no public access.
2. Restore the latest database backup with `pg_restore --clean --if-exists --no-owner --no-acl`.
3. Restore the matching attachment snapshot to a private temporary storage root.
4. Start the tested backend revision with the restored resources and `AI_ENABLED=false`.
5. Confirm Flyway validation succeeds and no unexpected migration runs.
6. Verify login, organization isolation, request access, attachment download, and quote totals.
7. Compare database attachment metadata with stored files and investigate missing or orphaned files.
8. Destroy the temporary environment and securely remove restored secrets and data.
9. Record duration and outcome. A backup is not considered usable until this test passes.

## Monitoring And Alerts

- Monitor `/actuator/health` through the private or platform health-check path.
- Alert on availability failures, database connection exhaustion, storage capacity, repeated restarts, and elevated 5xx rates.
- Alert on repeated `auth_login_rate_limited`, `authorization_denied`, `api_rate_limited`, and `api_failure` events.
- Keep logs encrypted, access-controlled, and retained only as long as operationally required.
- Verify rate limiting at the deployed instance count. The current limiter is in-memory and applies per backend instance.
- Monitor attachment storage against both platform capacity and application quotas.
- Monitor OpenAI usage and budget whenever real AI is enabled.

## Secret Rotation

Rotate database, hosting, and OpenAI credentials after suspected exposure and on the provider's normal rotation schedule. Update the secret manager first, restart or redeploy the affected service, verify health, then revoke the old credential. Invalidate active application sessions after an authentication-related incident or session-secret exposure.

## Incident Checklist

1. Disable affected integrations or public traffic when containment is required.
2. Preserve access-controlled logs and record the deployment revision and event time range.
3. Rotate exposed credentials and invalidate affected sessions.
4. Check organization-scoped access, database changes, file downloads, and AI usage.
5. Restore from a verified backup if integrity is uncertain.
6. Patch and test the cause before reopening traffic.
7. Document impact, remediation, and follow-up controls without copying sensitive customer data into tickets.

## Release Sign-Off

- [ ] Production frontend API URL is configured and uses HTTPS.
- [ ] Production profile starts successfully with constrained proxy settings.
- [ ] Database is private, TLS-protected, backed up, and uses separate migration and application roles.
- [ ] A database and attachment restore test has passed.
- [ ] Attachment storage is private, persistent, capacity-monitored, and backed up.
- [ ] Hosting secrets contain no demo or development credentials.
- [ ] CI tests, dependency review, secret scanning, and CodeQL are green on GitHub.
- [ ] Security logs and alerts are configured and access-controlled.
- [ ] Staging smoke tests cover authentication, CSRF, roles, organization isolation, uploads, and AI modes.
- [ ] A staging dynamic security scan has no unresolved release-blocking finding.
