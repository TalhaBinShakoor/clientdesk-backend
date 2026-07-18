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
SPRING_DATASOURCE_URL=<production PostgreSQL JDBC URL>
SPRING_DATASOURCE_USERNAME=<production database username>
SPRING_DATASOURCE_PASSWORD=<production database password>
AI_ENABLED=false
OPENAI_API_KEY=
AI_RATE_LIMIT_ENABLED=true
AI_RATE_LIMIT_MAX_REQUESTS=20
AI_RATE_LIMIT_WINDOW_SECONDS=60
```

Optional AI tuning:

```text
AI_RATE_LIMIT_MAX_REQUESTS=20
AI_RATE_LIMIT_WINDOW_SECONDS=60
```

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

## Public Demo Checklist

Before sharing the public demo URL:

- Confirm the backend health endpoint is available.
- Confirm demo seed data is visible in the frontend.
- Confirm AI summary works with `AI_ENABLED=false`.
- Confirm AI draft reply works with `AI_ENABLED=false`.
- Confirm rate limiting is enabled.
- Confirm no secret values are committed to Git.
