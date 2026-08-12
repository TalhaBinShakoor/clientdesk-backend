# ClientDesk Backend

**Related repository:** [ClientDesk Frontend](https://github.com/TalhaBinShakoor/clientdesk-frontend)

ClientDesk is a production-deployed client portal for freelancers, agencies, and small service businesses. It centralizes client records, incoming work requests, delivery tasks, comments, activity history, attachments, quotes, dashboard data, and AI-assisted communication in one role-aware workspace.

This repository contains the Java and Spring Boot API responsible for authentication, authorization, business rules, PostgreSQL persistence, attachment storage, AI integration, abuse controls, and production safety checks.

## Live Application

- Frontend: [https://clientdesk-omega.vercel.app](https://clientdesk-omega.vercel.app)
- Backend health: [https://clientdesk-backend.onrender.com/actuator/health](https://clientdesk-backend.onrender.com/actuator/health)

The backend runs on Render's free service tier. The first request after inactivity may take longer while the service wakes up.

### Demo Access

ClientDesk uses separate portfolio accounts for the ADMIN, TEAM_MEMBER, and CLIENT roles. Demo credentials are supplied privately on request and are never published in the repository.

Do not enter real client, personal, or confidential information into the public portfolio environment.

## Business Workflow

1. A client submits a work request with a requested priority and due date.
2. The server enforces an initial `NEW` status for CLIENT-created requests.
3. Admins and team members triage the request and organize delivery tasks.
4. Authorized users collaborate through comments, activity events, and attachments.
5. The team prepares quotes and can summarize request context or draft a reply.
6. Organization and client scoping restrict every user to authorized records.

## Backend Technology

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Security
- Spring Data JPA and Hibernate
- PostgreSQL 16
- Flyway
- Maven Wrapper
- Cloudinary Java SDK
- Docker and Docker Compose
- JUnit and Spring Security Test
- Render, Neon, and Cloudinary

## API Modules

| Module | Base path | Responsibility |
| --- | --- | --- |
| Authentication | `/api/auth` | Login, logout, current user, and CSRF token |
| Clients | `/api/clients` | Client CRM records |
| Work requests | `/api/work-requests` | Intake, filtering, status, priority, and due dates |
| Project tasks | `/api/project-tasks` | Delivery tasks, assignees, and four-state workflow |
| Comments | `/api/comments` | Request and task collaboration |
| Activity | `/api/activity-events` | Request history and actor attribution |
| Attachments | `/api/request-attachments` | Authorized upload, listing, and download |
| Quotes | `/api/quotes` | Commercial records and calculated line items |
| AI assistant | `/api/ai-assistant` | Request summaries and drafted client replies |
| Status | `/api/status` | Frontend backend-availability indicator |
| Health | `/actuator/health` | Hosting health check |

Collection endpoints use server-enforced pagination with a maximum page size of 100. Supported modules also expose status, priority, client, request, or assignee filters.

## Roles and Authorization

| Capability | ADMIN | TEAM_MEMBER | CLIENT |
| --- | --- | --- | --- |
| Read authorized business records | Yes | Yes | Assigned client only |
| Create and update clients | Yes | Yes | No |
| Submit work requests | Yes | Yes | Yes |
| Manage request status | Yes | Yes | No |
| Create and update tasks | Yes | Yes | No |
| Create and update quotes | Yes | Yes | No |
| Add comments and request attachments | Yes | Yes | Authorized records |
| Use the AI assistant | Yes | Yes | No |
| Delete supported business records | Yes | No | No |

Authorization is enforced in two layers:

- Spring Security applies deny-by-default route and HTTP-method rules.
- Service-layer access checks scope records by organization and, for CLIENT users, the assigned client account.

Object-level authorization failures return a not-found response where appropriate so records outside the caller's scope are not disclosed.

## Authentication and Browser Security

- BCrypt password hashing
- Server-side session authentication
- Cookie-only session tracking
- `HttpOnly`, `Secure`, and `SameSite=Lax` production session cookies
- CSRF cookie and request-header validation
- Session rotation after login and invalidation on logout
- Exact-origin credentialed CORS
- Deny-by-default authorization
- HSTS, frame denial, MIME-sniffing prevention, and restrictive referrer and permissions policies
- Safe API errors without stack traces or internal exception details

The frontend uses same-origin `/api/*` requests in production, so browser traffic follows the Vercel rewrite before reaching Render.

## AI Assistant

The assistant supports:

- Request-thread summaries
- Professional and friendly reply drafts
- Bounded request context and output
- Per-user and per-organization rate limits
- Connection, read, response-size, and output limits
- OpenAI requests with `store: false`
- Deterministic local fallback responses
- Explicit `OPENAI` or `LOCAL_FALLBACK` source metadata

Production currently runs with paid AI disabled, so the frontend intentionally displays **Local fallback**. The core request, authorization, and demonstration flows remain available without an OpenAI API key.

## Attachments

ClientDesk supports two storage implementations:

- Local filesystem storage for development and tests
- Cloudinary authenticated raw assets for production

Uploads are protected by:

- Authentication and record-level authorization
- Generated storage names and local path containment
- Extension, MIME type, and file-signature validation
- Per-file, per-request, and per-organization limits
- Forced download disposition and no-store responses
- Rollback and request-deletion cleanup behavior

Uploaded files are not currently processed by antivirus or content-disarm tooling; this remains a documented future improvement.

## Production Architecture

```text
Browser
  → Angular frontend on Vercel
  → same-origin /api/* rewrite
  → Spring Boot backend on Render
  → Neon PostgreSQL 16
  → Cloudinary authenticated raw assets
```

Production uses separate PostgreSQL migration and least-privilege application credentials. Flyway manages schema changes, while Render checks `/actuator/health` for service availability.

## Local Development

### Prerequisites

- Java 21
- Docker Desktop with Docker Compose
- A compatible local PostgreSQL client, optional

### Start PostgreSQL

```bash
docker compose up -d
```

The provided Compose service starts PostgreSQL 16 on `localhost:5432` with development-only defaults.

### Start the Backend

The default profile is `local`:

```bash
./mvnw spring-boot:run
```

The API starts at:

```text
http://localhost:8080
```

To load the fictional local portfolio dataset, activate the `demo` profile intentionally:

```bash
SPRING_PROFILES_ACTIVE=local,demo ./mvnw spring-boot:run
```

Demo credentials are not documented in this public README. Never combine the `demo` profile with a real production environment or database.

## Application Profiles

| Profile | Purpose |
| --- | --- |
| `local` | Local PostgreSQL, local attachment storage, and non-secure localhost cookie settings |
| `demo` | Adds repeatable fictional portfolio seed data |
| `test` | Test-specific settings with paid AI and API rate limiting disabled |
| `prod` | Secure cookies, trusted proxy processing, Cloudinary storage, and fail-closed production validation |

The production application refuses unsafe configuration, including blank database credentials, a non-HTTPS frontend origin, unconstrained proxy trust, paid AI without a key, and known development demo identities in a normal `prod` database.

## Configuration

Configuration is environment-driven. Set values in the local shell or hosting secret manager; never commit real values.

### Core Production Variables

```text
SPRING_PROFILES_ACTIVE
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
SPRING_FLYWAY_USER
SPRING_FLYWAY_PASSWORD
FRONTEND_ORIGIN
TRUSTED_PROXY_IP_PATTERN
```

### Attachment Variables

```text
ATTACHMENT_STORAGE_PROVIDER
ATTACHMENT_STORAGE_ROOT
ATTACHMENT_MAX_FILE_BYTES
ATTACHMENT_MAX_FILES_PER_WORK_REQUEST
ATTACHMENT_MAX_BYTES_PER_ORGANIZATION
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
CLOUDINARY_FOLDER_PREFIX
```

### AI Variables

```text
AI_ENABLED
OPENAI_API_KEY
OPENAI_BASE_URL
OPENAI_MODEL
OPENAI_CONNECT_TIMEOUT
OPENAI_READ_TIMEOUT
AI_MAX_CONTEXT_COMMENTS
AI_MAX_CONTEXT_CHARACTERS
AI_MAX_OUTPUT_CHARACTERS
AI_MAX_OUTPUT_TOKENS
```

### Abuse-Control Variables

```text
API_RATE_LIMIT_ENABLED
API_RATE_LIMIT_WINDOW_SECONDS
WRITE_RATE_LIMIT_PER_USER
WRITE_RATE_LIMIT_PER_ORGANIZATION
UPLOAD_RATE_LIMIT_PER_USER
UPLOAD_RATE_LIMIT_PER_ORGANIZATION
DOWNLOAD_RATE_LIMIT_PER_USER
DOWNLOAD_RATE_LIMIT_PER_ORGANIZATION
AI_RATE_LIMIT_PER_USER
AI_RATE_LIMIT_PER_ORGANIZATION
AUTH_RATE_LIMIT_ENABLED
AUTH_RATE_LIMIT_WINDOW_SECONDS
AUTH_RATE_LIMIT_MAX_FAILURES_PER_ACCOUNT_IP
AUTH_RATE_LIMIT_MAX_FAILURES_PER_IP
```

See `application.properties` and the operational documents for the complete configuration surface and reviewed defaults.

## Verification

Run the full backend test suite:

```bash
./mvnw test
```

The test suite covers business rules, validation, authentication, CSRF protection, role permissions, organization isolation, rate limiting, attachment validation and cleanup, AI behavior, production guards, and safe API errors.

GitHub Actions also runs:

- Backend tests against PostgreSQL
- Dependency review
- Gitleaks full-history secret scanning
- CodeQL security analysis
- Dependabot updates for Maven and GitHub Actions

## Deployment

`render.yaml` and `Dockerfile` define the Render deployment:

- Java 21 multi-stage container build
- Non-root runtime user
- Render free web-service plan
- Frankfurt region
- Health check at `/actuator/health`
- Automatic deployment after checks pass
- Environment-driven production secrets
- Cloudinary attachment provider
- Paid AI disabled by default

Production data lives in Neon PostgreSQL 16, and attachment bytes live as authenticated Cloudinary raw assets. Database-only backups are therefore incomplete without a matching Cloudinary asset inventory or recovery copy.

## Security and Operations Documentation

- [Deployment safety](DEPLOYMENT_SAFETY.md)
- [Production operations](PRODUCTION_OPERATIONS.md)
- [Production security baseline](SECURITY_BASELINE.md)
- [Security logging](SECURITY_LOGGING.md)

The security baseline is a targeted engineering review against OWASP ASVS Level 2 priorities. It is not an independent audit or formal certification.

## Known Limitations

- Render free-tier cold starts can delay the first request.
- Production currently uses the deterministic local AI fallback.
- Login and API rate-limit state is in memory and applies per backend instance.
- Sessions are local to one backend instance unless the hosting platform supplies sticky sessions.
- Task assignees are text values rather than validated organization-user references.
- Attachments do not yet have antivirus scanning or content disarm.
- Security events are structured application logs rather than an immutable audit datastore.
- MFA, password reset, account recovery, and breached-password screening are not implemented.

## Future Improvements

- Organization-user task assignment
- Shared session and abuse-control storage
- MFA and password lifecycle flows
- Private object storage with malware scanning and lifecycle policies
- Centralized audit-event storage and alerting
- Email notifications and team invitations
- Client approval workflows
- PDF quote export
- Advanced reporting, calendar, and payment integrations
