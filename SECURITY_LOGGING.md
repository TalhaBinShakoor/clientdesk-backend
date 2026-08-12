# ClientDesk Security Logging

Review date: 2026-08-12

ClientDesk writes structured security and audit events to the Spring Boot application log. Stable `key=value` fields support Render log search, operational investigation, and provider-configured alerting without recording request content or reusable secrets.

## Event Catalog

- `auth_login_succeeded`
- `auth_login_failed`
- `auth_login_rate_limited`
- `auth_logout`
- `authentication_required`
- `authorization_denied`
- `api_rate_limited`
- `protected_request_succeeded`
- `api_failure`

Successful protected requests record the operation category, authenticated user and organization identifiers, HTTP method, UUID-redacted route, and response status. Authorization and throttling events contain only the minimum identifiers needed for investigation.

## Identity and Network Pseudonymization

Login account and network references are process-salted SHA-256 prefixes. They allow correlation during one application process lifetime without logging the original email address or client IP address.

Because the salt is process-local, the same source is not intended to have a stable identifier across backend restarts. Pseudonymization reduces exposure but does not turn operational logs into public data.

## Prohibited Log Data

Application and platform logs must never contain:

- Passwords or password hashes
- Session identifiers or CSRF tokens
- API keys, database credentials, cookies, or authorization headers
- Raw email addresses or client IP addresses
- Request or response bodies
- Client, request, task, comment, activity, or quote text
- Attachment filenames, file content, signed URLs, or storage credentials
- AI prompts, request context, or generated output
- Database connection strings containing credentials
- Exception messages or stack traces from API failures

## Operational Use

Alert or investigate repeated occurrences of:

- `auth_login_rate_limited`
- `authorization_denied`
- `api_rate_limited`
- `api_failure`

Correlate an incident using time range, deployed revision, operation category, pseudonymized identifiers, redacted route, and response status. Do not copy sensitive browser data or provider secrets into an incident ticket.

## Retention and Access

- Keep production logs access-controlled in the hosting provider.
- Use encrypted provider storage and transport.
- Retain logs only for the operational and security period that has been explicitly selected.
- Restrict export and deletion permissions.
- Verify provider retention and alert settings after deployment changes.
- Remove exported logs securely when an investigation ends.

Retention, alert routing, and immutable storage are provider operations; they are not guaranteed by the application repository.

## Scope and Limitation

These events are structured operational security logs, not a complete business audit ledger or immutable audit datastore. They intentionally exclude business content. A future compliance-oriented deployment should add a separately designed, access-controlled, tamper-evident audit destination with explicit retention and privacy requirements.
