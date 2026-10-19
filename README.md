# AI Testing Platform

A development MVP for AI-assisted website QA. It combines a React dashboard, Java/Spring Boot REST API, PostgreSQL persistence, Selenium browser automation, optional AI test planning, and a separate human manual-review workflow.

## Included functionality

- Submit an authorized HTTP/HTTPS website URL and choose a testing scope.
- Server-side URL checks reject local/private destinations before navigation.
- Persist testing requests, automation runs/results, manual review requests, and manual findings.
- Optional AI planning selects IDs from a fixed read-only check allow-list; a deterministic plan is used when no API key is configured or the provider call fails.
- Selenium executes deterministic checks for page title, final URL, links, image alt text, headings, and form discovery. The runner does not click links, submit forms, or execute AI-generated code.
- The dashboard previews the test plan, shows outcomes, and lets a human tester record findings with severity, description/steps, and optional evidence URLs.
- Spring Security HTTP Basic authentication protects API routes. Credentials come from APP_USERNAME and APP_PASSWORD and are held in page memory by the UI.
- Docker Compose starts PostgreSQL, Selenium Chrome, backend, and frontend.
- GitHub Actions builds/tests the backend and builds the frontend.

## Important security limitations

This is not a production-audited service. Authentication is currently one shared workspace account, not a multi-user system with per-user record ownership. Before a public launch, add an identity provider and record-level authorization, quotas/rate limits, audit logs, monitoring, data retention, and private screenshot storage.

URL validation plus post-navigation URL checking is not enough to secure arbitrary browser navigation: a browser might already have followed a redirect before the final URL is checked. The browser worker must be isolated behind an egress-restricted network or policy-enforcing proxy that blocks loopback, private, link-local and cloud-metadata destinations on every DNS resolution and redirect. Do not expose the default local credentials. Do not test sites without permission, bypass CAPTCHA/MFA, or run arbitrary generated code.

## Stack

- Java 21, Spring Boot 3.4.x, Spring Data JPA, Spring Security
- Selenium WebDriver
- PostgreSQL
- React + Vite
- Docker Compose

## Quick start

1. Install Docker Desktop.
2. Copy .env.example to .env and change the database and application passwords. APP_PASSWORD must be at least 12 characters.
3. Start services with: docker compose up --build
4. Open http://localhost:5173 and sign in using APP_USERNAME and APP_PASSWORD from .env.
5. Health endpoint: http://localhost:8080/actuator/health. Health is public; application APIs require Basic authentication.

The bundled Docker browser worker is Selenium Standalone Chrome at http://selenium:4444. The UI currently offers Chrome only.

## AI configuration

Set OPENAI_API_KEY and optionally OPENAI_MODEL in the backend environment. The key is server-side only. The model may select only supported check IDs, never code or arbitrary browser actions. A deterministic plan is used if the key is blank or the model call fails.

## API overview

- GET /api/health
- POST /api/testing-requests
- GET /api/testing-requests
- GET /api/testing-requests/{id}
- GET /api/testing-requests/{id}/plan
- POST /api/testing-requests/{id}/automation
- GET /api/testing-requests/{id}/results
- PATCH /api/manual-reviews/{id}
- POST /api/manual-reviews/{id}/findings
- GET /api/manual-reviews/{id}/findings

Except for health endpoints and CORS preflight, routes require HTTP Basic authentication.

## Run backend locally

Requires Java 21 and Maven. Start PostgreSQL and Selenium separately, then run mvn spring-boot:run from the backend directory. Configure environment variables based on .env.example. For local Chrome, leave SELENIUM_REMOTE_URL empty and install compatible Chrome/ChromeDriver.

## Before deployment

- Use HTTPS and unique secrets in hosting configuration.
- Replace the shared account with an identity provider and per-user record ownership.
- Put browser automation behind a restricted egress proxy/network, including protections for every redirect and DNS rebinding.
- Use managed PostgreSQL and private object storage for screenshots.
- Configure CORS for only your deployed frontend origin.
- Add quotas, hard execution deadlines, a durable job queue/retry strategy, migrations, retention rules, audit logs and monitoring.
