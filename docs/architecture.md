# Architecture and flows

## Request flow

1. React submits a target URL, test scope, browser, and authorization confirmation.
2. Spring Boot validates and stores the request.
3. The service creates a separate manual review with PENDING status.
4. A manual reviewer can update status and create findings with severity, steps/details, and optional evidence URLs.
5. When automation starts, a bounded asynchronous executor queues the run.
6. The test planner returns structured check IDs from a fixed allow-list. An AI provider may select optional supported checks; otherwise a deterministic fallback is used.
7. Selenium maps each accepted ID to a deterministic handler. Unknown IDs are not executed.
8. Outcomes and screenshot paths are persisted; the dashboard displays automation and manual findings separately.

## AI planning

TestPlanService asks an OpenAI-compatible API for a JSON object with checkIds. The only optional IDs accepted are LINKS_PRESENT, IMAGES_ALT_TEXT, HEADINGS_PRESENT, and FORMS_DISCOVERED. PAGE_REACHED and PAGE_TITLE are always included. BrowserAutomationWorker executes fixed handlers for page title, final URL, link discovery, image alt text, heading presence, and form discovery. It does not click, type, submit, purchase, or execute generated code.

A final URL check is run after navigation as defense-in-depth. It is not a primary SSRF boundary because redirects may already have been followed by that point.

## Authentication model

The API uses Spring Security HTTP Basic authentication with a single in-memory account configured by APP_USERNAME and APP_PASSWORD. The dashboard collects the credentials at sign-in and keeps them in component state instead of browser storage. This does not provide per-user identity, tenant isolation, or per-record ownership. Replace it with an identity provider and per-record authorization before a multi-user launch.

## Security gaps before public launch

- Place the browser worker behind an egress-restricted network or policy-enforcing proxy, and block loopback, private, link-local, and metadata destinations on every DNS resolution and redirect.
- Replace the shared HTTP Basic account with an identity provider and per-user authorization for requests, results, manual reviews, and findings.
- Add request quotas, rate limiting, hard job deadlines, audit logs, monitoring, retention, and a durable job queue/retry policy.
- Keep API keys in hosting secrets; never expose them in frontend build variables.
- Store screenshots in private object storage and serve them only through authorized, expiring URLs.
- Add database migrations and integration tests against the deployment environment.
- Do not support CAPTCHA/MFA bypass, destructive actions, purchases, or arbitrary generated-code execution.
