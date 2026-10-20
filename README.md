# TestPilot AI — AI Testing Platform

TestPilot AI is a development MVP for AI-assisted website QA. It combines a React dashboard, Java/Spring Boot REST API, PostgreSQL persistence, Selenium browser automation, optional AI test planning, and a separate human manual-review workflow.

> **Status:** The React frontend can be hosted as a free Render Static Site. Real registration, login, saved requests and Selenium automation require the Spring Boot backend and PostgreSQL to be deployed and connected. The application is not production-audited.

## Features

- Register with a name, email and password, then sign in with the new account.
- Passwords are stored as BCrypt hashes; the API never returns password hashes.
- Each new testing request is associated with the account that created it. Request, plan, results, manual-review and finding routes check ownership before returning or mutating data.
- Submit an authorized HTTP/HTTPS website URL and choose a testing scope.
- Server-side URL validation rejects local/private destinations before navigation. This is defense-in-depth, not a complete SSRF defense.
- Persist testing requests, automation runs/results, manual review requests, and manual findings.
- Optional AI planning chooses IDs from a fixed read-only check allow-list; a deterministic plan is used when no API key is configured or the provider call fails.
- Selenium executes deterministic checks for page title, final URL, links, image alt text, headings, and form discovery. The runner does not click links, submit forms, or execute AI-generated code.
- The dashboard previews a test plan, shows outcomes, and lets a human tester record findings with severity, description/steps, and optional evidence URLs.
- Docker Compose starts PostgreSQL, Selenium Chrome, backend, and frontend.
- GitHub Actions builds/tests the backend and builds the frontend.

## Technology stack

- Java 21, Spring Boot 3.4.x, Spring Data JPA, Spring Security
- BCrypt password hashing and HTTP Basic API authentication
- Selenium WebDriver
- PostgreSQL
- React + Vite
- Docker Compose and GitHub Actions

## Run locally

1. Install Docker Desktop.
2. Copy `.env.example` to `.env`, then replace the local PostgreSQL password with a strong unique value.
3. Start services from the repository root:

   ```bash
   docker compose up --build
   ```

4. Open http://localhost:5173.
5. Select **Create an account**, register with a valid email and a password of 12–72 characters, then sign in.
6. The API health endpoint is http://localhost:8080/actuator/health.

The bundled browser worker is Selenium Standalone Chrome at http://selenium:4444. The browser worker should not be exposed to the public internet.

To run the backend outside Docker, use Java 21 and Maven. Start PostgreSQL and Selenium separately, set the variables from `.env.example`, and run `mvn spring-boot:run` from `backend/`. For local Chrome, leave `SELENIUM_REMOTE_URL` empty and install a compatible Chrome/ChromeDriver.

## AI configuration

Set `OPENAI_API_KEY` and optionally `OPENAI_MODEL` in the backend environment. The key must remain server-side. The model can select only supported check IDs, never code or arbitrary browser actions. A deterministic plan is used if the key is blank or the provider call fails.

## API overview

### Account

- `POST /api/auth/register` — create an account; JSON contains `name`, `email`, and `password`.
- `POST /api/auth/login` — validate HTTP Basic credentials and return the signed-in user's public profile. Send the registered email as the HTTP Basic username.
- `GET /api/health`

### Testing workspace

The following endpoints require HTTP Basic authentication and only return or mutate requests owned by the authenticated account:

- `POST /api/testing-requests`
- `GET /api/testing-requests`
- `GET /api/testing-requests/{id}`
- `GET /api/testing-requests/{id}/plan`
- `POST /api/testing-requests/{id}/automation`
- `GET /api/testing-requests/{id}/results`
- `PATCH /api/manual-reviews/{id}`
- `POST /api/manual-reviews/{id}/findings`
- `GET /api/manual-reviews/{id}/findings`

## Free Render frontend preview

Create a Render **Static Site** from this repository with:

- Branch: `main`
- Root Directory: `frontend`
- Build Command: `npm install && npm run build`
- Publish Directory: `dist`
- Free Static Site plan

Do not add database credentials, HTTP Basic credentials, or the OpenAI key to frontend environment variables. When the backend is deployed, set frontend variable `VITE_API_BASE_URL` to the backend's HTTPS origin (without `/api`) and set backend variable `FRONTEND_ORIGIN` to this site's exact origin. The visual frontend can load without a backend, but account creation/login and testing actions need a reachable backend and database.

## Security limitations

This is a prototype, not a production-audited service. Account registration does not yet include email verification, password reset, login/registration rate limiting, or account recovery. Use HTTPS in hosted environments and do not reuse important passwords. HTTP Basic credentials are sent with API requests and must only be used over HTTPS.

URL validation plus post-navigation URL checking is not enough to secure arbitrary browser navigation: the browser might already have followed a redirect before the final URL is checked. Before inviting public users, isolate the browser worker behind an egress-restricted network or policy-enforcing proxy that blocks loopback, private, link-local and cloud-metadata destinations on every DNS resolution and redirect. Do not expose Selenium's WebDriver port publicly. Do not test websites without permission, bypass CAPTCHA/MFA, or run arbitrary generated code.

Before production use, add registration/login rate limits, email verification and password recovery, explicit database migrations, quotas and hard execution deadlines, durable job queuing/retry policies, audit logs, retention rules, monitoring, private screenshot storage, and a security review.
