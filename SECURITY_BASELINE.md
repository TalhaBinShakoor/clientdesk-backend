# ClientDesk Production Security Baseline

Review date: 2026-08-12

Target: OWASP Application Security Verification Standard (ASVS) 5.0.0 Level 2 priorities.

This is a targeted engineering review of the ClientDesk portfolio application. It is not an independent audit, penetration test, or formal ASVS certification. Deployed-provider settings and recurring operational procedures require separate operator evidence.

## Deployed Security Architecture

```text
Browser
  → Vercel frontend and browser-security headers
  → same-origin /api/* rewrite
  → Render Spring Boot API
  → Neon PostgreSQL 16
  → Cloudinary authenticated raw assets
```

The production application uses an exact HTTPS frontend origin, secure session cookies, constrained proxy trust, a least-privilege application database role, a separate Flyway migration role, and environment-managed credentials.

## Implemented and Verified Application Controls

| Area | Status | ClientDesk evidence |
| --- | --- | --- |
| Input and business validation | Implemented | Allowlisted enums, bounded text and numeric inputs, quote line-item limits, JSON and multipart limits, attachment quotas, and server-enforced pagination. |
| Authentication | Implemented for portfolio MVP | BCrypt passwords, generic login failures, CSRF-protected session login, session rotation, logout invalidation, secure production cookies, and login abuse protection. |
| Session management | Implemented for portfolio MVP | Server-side sessions, cookie-only tracking, `HttpOnly`, `Secure`, `SameSite=Lax`, and a maximum 30-minute production timeout. |
| Route authorization | Implemented | Deny-by-default HTTP route rules with ADMIN, TEAM_MEMBER, and CLIENT permissions. |
| Object authorization | Implemented | Organization-scoped queries, assigned-client scoping, server-derived actors, not-found responses for inaccessible records, and cross-organization integration tests. |
| Browser and API security | Implemented | Same-origin production API rewrite, exact-origin credentialed CORS, CSRF validation, HSTS, frame denial, MIME-sniffing prevention, restrictive referrer and permissions policies, and safe API errors. |
| Uploaded files | Implemented with accepted limitations | Authorized access, generated storage names, extension/MIME/signature validation, size and storage quotas, authenticated Cloudinary assets, forced downloads, and cleanup behavior. |
| AI integration | Implemented | Role and record authorization, disabled-by-default paid AI, safe HTTPS provider validation, bounded context/output, timeouts, response limits, `store:false`, rate limits, fallback behavior, and visible source metadata. |
| Abuse controls | Implemented for single instance | Login, write, upload, download, and AI rate limits with bounded and expiring tracking state. |
| Error handling | Implemented | Centralized safe responses without stack traces, exception details, secrets, or request content. |
| Security logging | Implemented | Structured authentication, authorization, throttling, success, and failure events with sensitive-data exclusions and pseudonymized account/network references. |
| Production safety | Implemented | Fail-closed `prod` guard for database credentials, HTTPS origin, secure cookies, session timeout, proxy trust, AI provider settings, and known development demo identities. |
| CI and source security | Configured and exercised | Backend/frontend tests and builds, dependency review, npm production audit, Dependabot, Gitleaks full-history scans, and CodeQL security-extended analysis. |

## Resolved Deployment Gates

The earlier pre-deployment review identified gates that are now reflected in the deployed architecture:

- The frontend uses relative `/api` URLs with a Vercel same-origin production rewrite.
- Vercel supplies frontend security headers, including a restrictive Content Security Policy.
- Render hosts the containerized Spring Boot API and checks `/actuator/health`.
- Neon PostgreSQL 16 supplies the production database.
- Production uses separate migration and application credentials.
- Cloudinary stores production attachments as authenticated raw assets.
- Production uses separately provisioned portfolio accounts rather than the repository's development seed identities.
- CI and security workflows run for both repositories.
- Stable-domain production QA passed for ADMIN, TEAM_MEMBER, and CLIENT workflows.

These statements describe the reviewed application and deployment design. They do not replace recurring verification that provider settings remain unchanged.

## Outstanding Operational Assurance

The following depend on provider configuration or recurring operator evidence and are not proven solely by repository code:

- Neon backup retention, encryption, and successful isolated restore exercises
- Cloudinary inventory/recovery procedures synchronized with database backups
- Centralized alert delivery, log access controls, and retention settings
- Periodic verification of database grants and hosting secret access
- External dynamic scanning and independent penetration testing
- Credential-rotation and incident-response exercises

Track these as operational assurance work rather than unresolved application defects.

## Accepted Portfolio MVP Limitations

- No MFA, password reset, password change, account recovery, or breached-password screening flow
- Login and API rate-limit state is in memory and is not shared across backend instances
- Sessions are local to an application instance unless the platform supplies sticky sessions
- Attachments are not scanned by antivirus, sandbox, or content-disarm tooling
- Security events are application logs rather than an immutable audit datastore
- No automated end-to-end browser security suite or independent penetration test
- Task assignees are text values rather than validated organization-user references
- Render cold starts and provider quotas remain infrastructure limitations

These limitations are acceptable for a controlled fictional-data portfolio deployment. They must be revisited before processing real customer or sensitive business data.

## Follow-Up Controls

- Add MFA and secure password lifecycle flows
- Move sessions and abuse counters to a shared store before horizontal scaling
- Replace text assignees with organization-user references and server validation
- Add attachment malware scanning and lifecycle policies
- Add centralized alerting and an access-controlled audit destination
- Automate recovery evidence and scheduled restore exercises where provider plans allow
- Add browser-level security regression tests and periodic external assessment
- Repeat this review after material authentication, storage, AI, or deployment changes

## References

- [OWASP ASVS 5.0.0](https://github.com/OWASP/ASVS/tree/v5.0.0_release)
- [OWASP Database Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Database_Security_Cheat_Sheet.html)
- [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)
- [OWASP CI/CD Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/CI_CD_Security_Cheat_Sheet.html)
