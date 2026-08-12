# ClientDesk Deployment Safety

Review date: 2026-08-12

This document describes the safe deployment modes for the public ClientDesk portfolio environment. It records configuration names and expected behavior only; real values belong in provider secret managers and must never be committed to Git.

## Current Production Topology

```text
Browser
  → stable Angular frontend on Vercel
  → same-origin /api/* rewrite
  → Spring Boot backend on Render
  → Neon PostgreSQL 16
  → Cloudinary authenticated raw assets
```

- Frontend: `https://clientdesk-omega.vercel.app`
- Backend health: `https://clientdesk-backend.onrender.com/actuator/health`
- Backend hosting: Render free web service
- Database: Neon PostgreSQL 16
- Attachment storage: Cloudinary authenticated raw assets
- Production profile: `prod`

Only the stable frontend URL should be published or used for portfolio browser testing. Render cold starts remain an expected infrastructure limitation.

## Production AI Modes

### Public Portfolio Mode — Current

Use this mode when the application should remain demonstrable without paid AI traffic.

```text
AI_ENABLED=false
OPENAI_API_KEY=<unset or blank>
```

Expected behavior:

- Summary and drafted-reply endpoints remain available to authorized roles.
- Responses use the deterministic local generator.
- Responses identify their source as `LOCAL_FALLBACK`.
- No OpenAI API call is attempted.
- The frontend displays **Local fallback**.

This is the current production configuration and is an expected state, not a defect.

### Real AI Enabled

Use this mode only for an intentional, budgeted demonstration.

```text
AI_ENABLED=true
OPENAI_API_KEY=<secret supplied by the hosting platform>
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=<reviewed supported model>
```

Expected behavior:

- Authorized assistant requests call OpenAI over HTTPS.
- Requests use bounded context and output limits with storage disabled.
- Per-user and per-organization rate limits remain active.
- Provider failures or unusable responses fall back to the local generator.
- The response source identifies whether OpenAI or the local fallback produced the result.

The production guard refuses `AI_ENABLED=true` when the API key is blank or the provider URL is unsafe.

### Non-Production Fallback Exercise

Local or test environments may exercise fallback behavior with AI enabled and no key. Do not document that combination as a production mode: the `prod` safety guard intentionally rejects it.

## Portfolio Account Safety

- Production portfolio accounts are provisioned separately from the repository's development seed identities.
- Each role uses a unique password stored outside Git and never pasted into documentation or chat.
- The repeatable `demo` Flyway seed must not be activated with the normal production profile.
- The production guard rejects known development demo identities and data.
- Public documentation may name the available roles, but it must not publish account passwords.
- The public portfolio environment must contain fictional data only.

## Required Production Configuration

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

Optional reviewed tuning includes:

```text
OPENAI_MODEL
OPENAI_CONNECT_TIMEOUT
OPENAI_READ_TIMEOUT
AI_RATE_LIMIT_PER_USER
AI_RATE_LIMIT_PER_ORGANIZATION
API_RATE_LIMIT_WINDOW_SECONDS
AUTH_RATE_LIMIT_WINDOW_SECONDS
```

Do not weaken secure cookies, request limits, upload validation, safe errors, or rate limiting to simplify deployment.

## Secret Handling

- Store production credentials only in Render, Neon, Cloudinary, or another approved secret manager.
- Never place secrets in Git, workflow files, frontend code, screenshots, logs, command history, or support messages.
- Keep separate migration and application database credentials.
- Use a dedicated OpenAI project and key if paid AI is enabled.
- Rotate a credential after suspected exposure and revoke the previous value after the replacement is verified.
- Treat signed attachment URLs, session identifiers, CSRF tokens, and database dumps as sensitive.

## Recurring Public-Demo Checks

Before sharing or rehearsing the portfolio demo:

- Open only the stable frontend URL.
- Allow for a Render cold start, then confirm the health endpoint returns healthy status.
- Confirm login and logout for each portfolio role without exposing passwords.
- Confirm ADMIN, TEAM_MEMBER, and CLIENT authorization boundaries.
- Confirm CLIENT-created requests begin as `NEW`.
- Confirm attachment upload and download remain authenticated.
- Confirm the assistant displays **Local fallback** while paid AI is disabled.
- Confirm `/api/*` traffic uses the Vercel same-origin rewrite.
- Confirm no secret values or real customer data appear in the browser, logs, or screenshots.
- Confirm CI and security checks are green for the deployed revisions.

Database recovery, Cloudinary recovery, monitoring, and incident procedures are maintained in [PRODUCTION_OPERATIONS.md](./PRODUCTION_OPERATIONS.md). The current reviewed security posture is summarized in [SECURITY_BASELINE.md](./SECURITY_BASELINE.md).
