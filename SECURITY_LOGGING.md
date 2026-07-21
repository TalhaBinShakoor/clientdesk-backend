# Security Logging

ClientDesk writes structured security and audit events to the application log. Event messages use stable `key=value` fields so the deployment platform can search and alert on them.

## Events

- `auth_login_succeeded`
- `auth_login_failed`
- `auth_login_rate_limited`
- `auth_logout`
- `authentication_required`
- `authorization_denied`
- `api_rate_limited`
- `protected_request_succeeded`
- `api_failure`

Successful protected requests identify the operation category, authenticated user and organization IDs, HTTP method, UUID-redacted route, and response status. Authorization and throttling events contain only the minimum identifiers needed for investigation.

## Data Exclusions

Logs must never contain:

- passwords, password hashes, session IDs, or CSRF tokens
- API keys or authorization headers
- raw email addresses or client IP addresses
- request or response bodies
- client, request, task, comment, or quote text
- attachment filenames or file contents
- AI prompts or generated output
- exception messages or stack traces from API failures

Login account and network references are process-salted SHA-256 prefixes. They support correlation during one application process lifetime without exposing the original values.

## Operations

- Keep production application logs access-controlled.
- Use the deployment platform's encrypted log storage.
- Retain logs only for the period needed for security investigation and operational support.
- Alert on repeated `auth_login_rate_limited`, `authorization_denied`, `api_rate_limited`, and `api_failure` events.
