# Architecture and flows

## Request flow

1. A visitor creates an account with a name, email and password.
2. Spring Security loads accounts from PostgreSQL and verifies BCrypt password hashes for HTTP Basic API authentication.
3. React submits an authorized target URL, test scope, browser and authorization confirmation.
4. Spring Boot validates and stores the request with the authenticated account as its owner.
5. The service creates a separate manual review with PENDING status.
6. A manual reviewer can update review status and add findings with severity, steps/details and optional evidence URLs.
7. When automation starts, a bounded asynchronous executor queues the run.
8. The test planner returns structured check IDs from a fixed allow-list. An AI provider may select optional supported checks; otherwise a deterministic fallback is used.
9. Selenium maps each accepted ID to a deterministic handler. Unknown IDs are not executed.
10. Outcomes and screenshot paths are persisted; the dashboard displays automation and manual findings separately.

## Multi-account ownership

Every newly created testing request stores its owner. Request lists and per-request endpoints require the authenticated email to match that owner. Manual-review and finding operations are scoped through the owning request. Account password hashes are not included in API responses. The browser UI keeps credentials in memory rather than browser storage.

HTTP Basic authentication is stateless, so the frontend sends the registered email and password over each API request. Hosted deployments must use HTTPS. This prototype does not yet implement email verification, password reset, recovery, or login/registration rate limiting.

## AI planning

TestPlanService asks an OpenAI-compatible API for a JSON object with checkIds. The only optional IDs accepted are LINKS_PRESENT, IMAGES_ALT_TEXT, HEADINGS_PRESENT, and FORMS_DISCOVERED. PAGE_REACHED and PAGE_TITLE are always included. BrowserAutomationWorker executes fixed handlers for page title, final URL, link discovery, image alt text, heading presence and form discovery. It does not click, type, submit, purchase or execute generated code.

A final URL check is run after navigation as defense-in-depth. It is not a primary SSRF boundary because redirects may already have been followed by that point.

## Security gaps before public launch

- Place the browser worker behind an egress-restricted network or policy-enforcing proxy, and block loopback, private, link-local and metadata destinations on every DNS resolution and redirect.
- Add registration/login rate limiting, email verification and account recovery.
- Add request quotas, hard job deadlines, audit logs, monitoring, retention and a durable job queue/retry policy.
- Keep API keys in hosting secrets; never expose them in frontend build variables.
- Store screenshots in private object storage and serve them only through authorized, expiring URLs.
- Add database migrations and integration tests against the deployment environment.
- Do not support CAPTCHA/MFA bypass, destructive actions, purchases or arbitrary generated-code execution.
