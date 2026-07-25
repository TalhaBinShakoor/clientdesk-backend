# ClientDesk Deployment Safety

## AI Demo Modes

ClientDesk can run safely in three AI modes.

### AI Spending Off

Use this for public demos when OpenAI spending should be fully disabled.

Required environment variables:

```text
AI_ENABLED=false
OPENAI_API_KEY=
```

Expected behavior:

- AI assistant endpoints stay available.
- Responses use the local fallback generator.
- No OpenAI API calls are made.
- The app remains demoable without AI spending.

### Fallback AI Only

Use this when no OpenAI key is configured.

Required environment variables:

```text
AI_ENABLED=true
OPENAI_API_KEY=
```

Expected behavior:

- AI assistant endpoints stay available.
- Responses use the local fallback generator.
- No OpenAI API calls are made because the API key is blank.

### Real AI Enabled

Use this only when OpenAI spending is intentionally enabled.

Required environment variables:

```text
AI_ENABLED=true
OPENAI_API_KEY=<production OpenAI API key>
```

Expected behavior:

- AI assistant endpoints call OpenAI.
- If OpenAI is unavailable or returns an unusable response, ClientDesk falls back to the local generator.

## Backend Environment Variables

Required for deployment:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=<production PostgreSQL JDBC URL>
SPRING_DATASOURCE_USERNAME=<least-privilege application database username>
SPRING_DATASOURCE_PASSWORD=<application database password>
SPRING_FLYWAY_USER=<database migration username>
SPRING_FLYWAY_PASSWORD=<database migration password>
FRONTEND_ORIGIN=https://<production frontend host>
TRUSTED_PROXY_IP_PATTERN=<constrained hosting proxy IP regex>
ATTACHMENT_STORAGE_PROVIDER=cloudinary
CLOUDINARY_CLOUD_NAME=<production Cloudinary cloud name>
CLOUDINARY_API_KEY=<dedicated production API key>
CLOUDINARY_API_SECRET=<dedicated production API secret>
CLOUDINARY_FOLDER_PREFIX=clientdesk/production/attachments
AI_ENABLED=false
OPENAI_API_KEY=
```

Optional AI and rate-limit tuning:

```text
OPENAI_MODEL=gpt-5-nano
AI_RATE_LIMIT_PER_USER=20
AI_RATE_LIMIT_PER_ORGANIZATION=100
API_RATE_LIMIT_WINDOW_SECONDS=60
AUTH_RATE_LIMIT_WINDOW_SECONDS=900
```

Keep rate limiting enabled. The complete environment, database, backup, attachment, monitoring, and restore checklist is in [PRODUCTION_OPERATIONS.md](./PRODUCTION_OPERATIONS.md). The reviewed security posture and remaining deployment gates are in [SECURITY_BASELINE.md](./SECURITY_BASELINE.md).

## OpenAI Budget And Usage Checklist

Before enabling real AI in production:

- Create a dedicated OpenAI project for ClientDesk.
- Add only the production API key to the deployment platform.
- Set a monthly project budget.
- Set a low alert threshold before the full budget is reached.
- Start with `AI_ENABLED=false` after deployment.
- Turn on `AI_ENABLED=true` only for an intentional demo.
- Check OpenAI usage after each public demo.
- Rotate the API key if it was exposed or copied into an unsafe place.
- Turn `AI_ENABLED=false` again when real AI is not needed.

## Public Deployment Checklist

Before sharing the public demo URL:

- Confirm the backend health endpoint is available.
- Choose and document the Day 16 demo-access strategy; do not combine the known demo seed identities with the `prod` profile.
- Confirm AI summary works with `AI_ENABLED=false`.
- Confirm AI draft reply works with `AI_ENABLED=false`.
- Confirm rate limiting is enabled.
- Confirm no secret values are committed to Git.
- Confirm a `prod` environment contains no known production-forbidden demo identities.
- Confirm the frontend production build points to the deployed HTTPS API.
- Confirm the database restore and Cloudinary attachment inventory/recovery checks have passed.
