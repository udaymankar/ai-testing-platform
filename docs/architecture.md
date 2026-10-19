# Architecture and flows

## Request flow
1. React submits URL, test scope, browser, and authorization confirmation.
2. Spring Boot validates the request and persists it.
3. A manual review is created as PENDING for every accepted request.
4. User starts automation; a run record is created and queued asynchronously.
5. Selenium worker opens the target and performs fixed read-only checks.
6. Results and screenshots are persisted.
7. Dashboard polls request state and shows automation and manual review separately.

## AI planning
`TestPlanService` optionally calls an OpenAI-compatible API to suggest safe, informational test names. AI output is not executable code and is not used to run arbitrary browser actions. The current browser runner implements a small fixed set of safe checks. Extend this by defining a strict allow-listed JSON action schema, validating every action, and implementing deterministic Selenium handlers.

## Security gaps to close before public launch
- Add authentication and per-user authorization for all endpoints.
- Add server-side authorization checks for requests, results, and manual reviews.
- Put browser worker on an egress-restricted network. URL validation alone is insufficient against DNS rebinding and redirects.
- Revalidate each redirect/final destination; consider a proxy that enforces network policy.
- Apply per-user quotas, rate limiting, concurrency limits, and hard execution deadlines.
- Keep API keys in hosting secret settings; never expose them in frontend variables.
- Store screenshots in private object storage and serve them with expiring signed URLs.
- Add migrations (Flyway), retention policies, audit logs, monitoring, and job queue/retry.
- Do not support CAPTCHA/MFA bypass, destructive actions, purchases, or arbitrary generated code execution.
