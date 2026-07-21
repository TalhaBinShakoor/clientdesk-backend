# Production Security Baseline

Review date: 2026-07-20

Target: OWASP Application Security Verification Standard (ASVS) 5.0.0 Level 2 priorities.

This is a targeted engineering review of the ClientDesk MVP, not an independent audit or formal ASVS certification. Controls that require deployed infrastructure remain release gates for Day 16.

## Implemented Baseline

| Area | Status | ClientDesk evidence |
| --- | --- | --- |
| Validation and business logic | Implemented | Bean validation and allowlists, bounded text and numeric inputs, quote line-item limits, JSON and multipart limits, and server-enforced pagination. |
| Authentication | Implemented for MVP | BCrypt passwords, generic login failure responses, CSRF-protected session login, session ID rotation, logout invalidation, secure cookie production settings, and bounded login abuse protection. |
| Session management | Implemented for MVP | Server-side sessions, `HttpOnly`, `Secure`, `SameSite=Lax`, cookie-only tracking, maximum 30-minute production timeout, and no credentials in browser storage. |
| Authorization | Implemented | Deny-by-default routes, role checks, organization-scoped queries, object-level client access checks, server-derived actors, and cross-organization integration tests. |
| Browser and API security | Implemented | Exact-origin credentialed CORS, CSRF token validation, HSTS, frame denial, MIME sniffing prevention, restrictive referrer and permissions policies, safe API errors, and no stack traces in responses. |
| Data protection | Partially infrastructure-dependent | Secrets are environment-driven and excluded from logs; production database TLS, encryption at rest, private networking, backups, and credential rotation must be confirmed with the hosting provider. |
| Uploaded files | Implemented with follow-up | Authorization, generated storage names, path containment, extension/content allowlists, signature checks, size/count/storage quotas, forced download disposition, and rollback cleanup. Malware scanning and content disarm are not implemented. |
| Logging and monitoring | Implemented with deployment work | Structured security events pseudonymize raw email/IP references and exclude secrets and content. Retention, alerting, access controls, and centralized collection require provider configuration. |
| External services and AI | Implemented | AI disabled by default, role/object authorization, HTTPS production provider validation, no redirects, connection/read/response/output limits, bounded context, `store:false`, fallback behavior, and rate limits. |
| Configuration | Implemented with deployment work | Fail-closed `prod` startup guard, explicit demo profile, production logging restrictions, safe error settings, constrained trusted-proxy requirement, and environment-driven secrets. Provider values still require Day 16 verification. |
| Dependency and source security | Configured | GitHub Actions tests/builds, Dependabot, pull-request dependency review, Gitleaks full-history scans, npm production audit, and CodeQL security-extended analysis. GitHub results require commit and push. |

## Release Blockers

These items must be resolved or verified before public production traffic:

1. Replace every frontend `http://localhost:8080` API URL with the Day 16 production API configuration and build against HTTPS.
2. Select the deployment model for portfolio demo access. The hardened `prod` profile intentionally rejects known demo users and seed data; do not bypass this guard by combining `prod` and `demo`.
3. Configure a private TLS PostgreSQL service with separate migration and least-privilege application roles.
4. Enable encrypted database backups and complete a documented restore test.
5. Configure private persistent attachment storage and a backup synchronized with database metadata.
6. Set the exact frontend origin and hosting-provider proxy range; verify forwarded client addresses cannot be spoofed.
7. Configure frontend-host security headers, including a tested Content Security Policy for the Angular document.
8. Run GitHub CI/security jobs and resolve high-severity dependency, secret, or CodeQL findings.
9. Run staging authentication/authorization/upload smoke tests and a dynamic web security scan.

## Accepted MVP Limitations

These do not block a controlled portfolio deployment when documented and monitored, but they prevent a claim of complete ASVS Level 2 conformance:

- No MFA, password reset, password change, account recovery, or breached-password screening flow.
- Demo account passwords remain development-only and must never be deployed under the production profile.
- Login and API rate-limit state is in-memory and is not shared across multiple backend instances.
- Sessions are local to an application instance unless the platform supplies sticky sessions; distributed session storage is not configured.
- Uploaded files are not scanned by antivirus, sandbox, or content-disarm tooling.
- Security events are application logs rather than an immutable audit datastore.
- No automated end-to-end browser security suite or external penetration test has been completed.
- Database and attachment encryption, retention, deletion, and disaster recovery depend on the selected provider.

## Follow-Up Controls

- Add MFA and secure password lifecycle flows before handling real customer accounts.
- Move sessions and abuse counters to a shared store before horizontal scaling.
- Move attachments to private object storage with malware scanning and lifecycle policies.
- Add a restrictive frontend Content Security Policy and verify it in staging.
- Add centralized alerting and an access-controlled audit-log destination.
- Repeat the ASVS review after deployment architecture is known and before processing sensitive customer data.

## References

- [OWASP ASVS 5.0.0](https://github.com/OWASP/ASVS/tree/v5.0.0_release)
- [OWASP Database Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Database_Security_Cheat_Sheet.html)
- [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)
- [OWASP CI/CD Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/CI_CD_Security_Cheat_Sheet.html)
