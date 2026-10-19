# AI Testing Platform

A starter monorepo for an AI-assisted Selenium testing platform with a React dashboard, Java/Spring Boot API, PostgreSQL persistence, Selenium browser worker, and a mandatory human manual-review workflow.

## Current implementation scope

- Submit an authorized website URL and testing scope.
- Basic URL validation and rejection of localhost/private IP destinations.
- Persist requests, automation runs, test results, manual-review requests, and manual findings.
- Queue automation asynchronously in the API process for the starter version.
- Selenium checks page title, page reachability, and basic links; screenshots are saved to configured storage.
- Optional AI test-plan generation via OpenAI-compatible Chat Completions API; deterministic plan fallback when no API key is configured.
- Manual review is created as `PENDING` for every request; it is not falsely marked complete.
- React dashboard for request submission, run status, automation results, and manual review status.
- Docker Compose for PostgreSQL, Selenium Chrome, backend, and frontend.
- GitHub Actions workflow for Maven and frontend checks.

## Important status

This is a development MVP/scaffold, not a production-audited service. The source has not been built or exercised in the target hosting environment from this generated package. Run the verification steps below and fix any environment-specific issues before public launch.

Public arbitrary-URL browser automation is security-sensitive. For production, isolate the browser worker on a restricted network, block private/link-local/metadata destinations at the network layer, revalidate every redirect and DNS resolution, enforce quotas/timeouts, add robust authentication and authorization, and store screenshots in private object storage. Never run arbitrary AI-generated Java code. The AI plan is limited to supported structured actions.

## Stack

- Java 21, Spring Boot 3.4.x, Spring Data JPA, Bean Validation
- Selenium WebDriver (Chrome)
- PostgreSQL
- React + Vite
- Docker Compose

## Quick start

1. Install Docker Desktop and Git.
2. Copy `.env.example` to `.env`; change all passwords.
3. Start services:

   ```bash
   docker compose up --build
   ```

4. Open the frontend at `http://localhost:5173`.
5. Backend health endpoint: `http://localhost:8080/actuator/health`.

The browser worker is Selenium Standalone Chrome at `http://selenium:4444` inside Docker Compose. The backend connects to it using `SELENIUM_REMOTE_URL`.

## Run backend locally (without Docker)

Requires Java 21 and Maven. Start PostgreSQL and Selenium separately, then:

```bash
cd backend
mvn spring-boot:run
```

Configure environment variables from `.env.example`. For local Chrome instead of a remote browser, set `SELENIUM_REMOTE_URL` to empty and ensure Chrome/ChromeDriver are installed and compatible.

## AI configuration

Set `OPENAI_API_KEY` and optionally `OPENAI_MODEL` in the backend environment. When no key is configured, the application uses a deterministic safe plan. Keep API keys server-side only.

## API overview

- `GET /api/health`
- `POST /api/testing-requests`
- `GET /api/testing-requests`
- `GET /api/testing-requests/{id}`
- `POST /api/testing-requests/{id}/automation`
- `GET /api/testing-requests/{id}/results`
- `PATCH /api/manual-reviews/{id}`
- `POST /api/manual-reviews/{id}/findings`

All mutating endpoints should be protected by real user authentication before public deployment. This starter intentionally binds to local development defaults and must not be exposed publicly as-is.

## Before deployment

- Configure frontend `VITE_API_BASE_URL`.
- Use managed PostgreSQL and private object storage for screenshots.
- Deploy backend and browser worker separately where possible.
- Configure CORS for only your deployed frontend origin.
- Add authentication, role-based access control, rate limits, per-user quotas, audit logs, job queue/retry strategy, data retention, and monitoring.
- Do not test sites without authorization; do not attempt to bypass CAPTCHA, MFA, or access controls.
