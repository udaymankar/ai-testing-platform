# TestPilot AI — AI Testing Platform

[![Frontend Live on Render](https://img.shields.io/badge/Frontend-Live%20on%20Render-46B36B?logo=render&logoColor=white)](https://ai-testing-platform-frontend.onrender.com/) ![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=black) ![Vite](https://img.shields.io/badge/Vite-6-646CFF?logo=vite&logoColor=white) ![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)

**🚀 Live Frontend Preview: [https://ai-testing-platform-frontend.onrender.com/](https://ai-testing-platform-frontend.onrender.com/)**

TestPilot AI is a development MVP for AI-assisted website QA. It combines a React dashboard, Java/Spring Boot REST API, PostgreSQL persistence, Selenium browser automation, optional AI test planning and a separate human manual-review workflow.

### Deployment status

- **Frontend:** Deployed on Render as a Static Site. Open the live link above to preview the user interface.
- **Frontend CI:** The React/Vite production build passes in GitHub Actions.
- **Backend, database and Selenium:** These are separate components and must be deployed and connected before registration, login, saved requests and real browser automation will work on the public site.
- **Project stage:** Development MVP; not yet production-audited.

> The live link currently demonstrates the deployed frontend UI. It should not be treated as a fully functional online testing service until the backend and database are connected.

## Features

- Register with a name, email and password, then sign in with the new account.
- Passwords are stored as BCrypt hashes; the API never returns password hashes.
- Each new testing request is associated with the account that created it. Request, plan, results, manual-review and finding routes check ownership before returning or mutating data.
- Submit an authorized HTTP/HTTPS website URL and choose a testing scope.
- Server-side URL validation rejects local/private destinations before navigation. This is defense-in-depth, not a complete SSRF defense.
- Persist testing requests, automation runs/results, manual review requests and manual findings.
- Optional AI planning chooses IDs from a fixed read-only check allow-list; a deterministic plan is used when no API key is configured or the provider call fails.
- Selenium executes deterministic checks for page title, final URL, links, image alt text, headings and form discovery. The runner does not click links, submit forms or execute AI-generated code.
- The dashboard previews a test plan, shows outcomes and lets a human tester record findings with severity, description/steps and optional evidence URLs.
- Docker Compose starts PostgreSQL, Selenium Chrome, backend and frontend.
- GitHub Actions builds/tests the backend, builds the frontend and validates the Docker Compose configuration.

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
5. Select **Create an account**, register with a valid email and password of at least 12 characters. The password must not exceed 72 UTF-8 bytes for BCrypt compatibility.
6. The API health endpoint is http://localhost:8080/actuator/health.

The bundled browser worker is Selenium Standalone Chrome at http://selenium:4444. The browser worker should not be exposed to the public internet.

To run the backend outside Docker, use Java 21 and Maven. Start PostgreSQL and Selenium separately, set the variables from `.env.example`, and run `mvn spring-boot:run` from `backend/`. For local Chrome, leave `SELENIUM_REMOTE_URL` empty and install a compatible Chrome/ChromeDriver.

## AI configuration

Set `OPENAI_API_KEY` and optionally `OPENAI_MODEL` in the backend environment. The key must remain server-side. The model can select only supported check IDs, never code or arbitrary browser actions. A deterministic plan is used if the key is blank or the provider call fails.

## API overview

### Account

- `POST /api/auth/register` — create an account; JSON contains `name`, `email` and `password`.
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

Do not add database credentials, HTTP Basic credentials or the OpenAI key to frontend environment variables. When the backend is deployed, set frontend variable `VITE_API_BASE_URL` to the backend's HTTPS origin (without `/api`) and set backend variable `FRONTEND_ORIGIN` to this site's exact origin. The visual frontend can load without a backend, but account creation/login and testing actions need a reachable backend and database.

## Deploy the full application on Oracle Cloud Always Free

The production configuration runs the frontend, Spring Boot API, PostgreSQL and Selenium with Docker Compose on one Ubuntu ARM64 VM. Caddy provides HTTPS and routes API requests to the backend. Only ports 80 and 443 are published; PostgreSQL and Selenium have no public port mappings.

### 1. Create an Always Free VM

1. Sign in to [Oracle Cloud Infrastructure](https://cloud.oracle.com/) and switch to your tenancy's **home region**. Always Free compute must be created in that region.
2. Open **Compute → Instances → Create instance** and name it `testpilot-ai-vm`.
3. Select **Ubuntu 24.04 ARM64/AArch64**.
4. Choose `VM.Standard.A1.Flex`. Proceed only if the console explicitly marks the shape **Always Free eligible**. The free allowance can cover up to 2 OCPUs and 12 GB RAM across eligible A1 instances, but availability is not guaranteed.
5. If marked eligible, select 2 OCPUs and 12 GB RAM for the browser workload. You may use 1 OCPU / 6 GB if necessary, but Chrome runs may be slower.
6. Choose a boot volume within the Always Free storage allowance (for example, 50 GB). Before creating, confirm the console shows the expected free eligibility/cost. **Do not upgrade the account or choose a paid shape** to work around capacity issues.
7. Use a public subnet and assign a public IPv4 address. Generate/upload an SSH key; save the **private key securely on your Mac** and never put it in GitHub.
8. Create the VM and note its public IPv4 address.

See Oracle's [Always Free resource list](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) and [VM creation guide](https://docs.oracle.com/en-us/iaas/Content/Compute/Tasks/launchinginstance.htm). If capacity is unavailable, retry later rather than upgrading solely to obtain capacity.

### 2. Configure the network rules

In the VM's VCN Security List or Network Security Group, allow these inbound rules:

| Port | Source | Purpose |
|---|---|---|
| TCP 22 | Your current public IP only, using a `/32` CIDR | SSH administration |
| TCP 80 | `0.0.0.0/0` | HTTPS certificate challenge and redirect |
| TCP 443 | Initially your public IP only, using a `/32` CIDR | Private smoke testing |

**Do not** open ports 5432, 4444, 8080 or 5173. Do not leave SSH open to the entire internet. Keep HTTPS source restricted during initial testing; see the security note below before making the demo public.

### 3. SSH from your Mac

Use the private key you downloaded. If it is stored as `~/.ssh/oci_testpilot_rsa`, run:

```bash
chmod 400 ~/.ssh/oci_testpilot_rsa
ssh -i ~/.ssh/oci_testpilot_rsa ubuntu@YOUR_ORACLE_PUBLIC_IP
```

Replace the IP placeholder with your VM address. The username `ubuntu` is for Ubuntu images.

### 4. Install Docker Engine and Compose

Run these commands **inside the SSH session**:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo ${UBUNTU_CODENAME:-$VERSION_CODENAME}) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo docker run --rm hello-world
sudo docker compose version
```

These follow [Docker's official Ubuntu installation instructions](https://docs.docker.com/engine/install/ubuntu/).

### 5. Create a hostname with HTTPS

Caddy needs a real hostname to obtain a trusted certificate. Use a domain you own, or create a free [DuckDNS](https://www.duckdns.org/) subdomain:

1. Create a subdomain in DuckDNS.
2. Set the subdomain's IPv4 address to your Oracle VM public IP.
3. Wait for DNS to resolve to the VM. Port 80 must be reachable from the internet and port 443 must be allowed to your IP for the private smoke test.
4. Do not share your DuckDNS account token; this deployment does not need it.

Caddy will request and renew the HTTPS certificate automatically when the hostname and ports are correctly configured. See the [Caddy HTTPS quick start](https://caddyserver.com/docs/quick-starts/https).

### 6. Get the code and set production secrets

On the VM:

```bash
git clone https://github.com/udaymankar/ai-testing-platform.git
cd ai-testing-platform
cp .env.production.example .env.production
nano .env.production
```

Edit `.env.production`:

- `SITE_DOMAIN`: your real hostname, with no `https://` or trailing slash.
- `ACME_EMAIL`: an email address for certificate notices.
- `POSTGRES_PASSWORD`: generate a strong value using `openssl rand -hex 32` and paste the output.
- `OPENAI_API_KEY`: optional; leave blank to use the deterministic test-plan fallback.

Never commit or share `.env.production`; only the placeholder `.env.production.example` belongs in GitHub.

### 7. Validate and start all services

Run these commands from the cloned repository directory:

```bash
sudo docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
sudo docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
sudo docker compose --env-file .env.production -f docker-compose.prod.yml ps
```

The first build downloads/builds multiple images and can take a while. The Selenium standalone Chrome image is multi-architecture and supports ARM64; see [Selenium's Docker image documentation](https://github.com/SeleniumHQ/docker-selenium).

### 8. Verify the app

Check logs:

```bash
sudo docker compose --env-file .env.production -f docker-compose.prod.yml logs --tail=100 caddy backend frontend postgres selenium
```

Open `https://YOUR_SITE_DOMAIN`. Try creating an account, signing in, creating one testing request for a website you own or have permission to test, and running one browser check. If anything fails, the logs should show which component needs attention.

### 9. Update after new commits

```bash
cd ~/ai-testing-platform
git pull
sudo docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

If the repository is in a different folder, `cd` to that folder first. Preserve `.env.production` when pulling updates.

### Security note before public launch

This project is still a prototype. It does not yet have registration/login rate limits, email verification, or a complete browser egress boundary for every redirect and DNS change. Initial URL validation alone cannot prevent a browser from following a redirect to a private/internal destination. Keep HTTPS restricted to your IP for initial testing, invite only trusted testers, and do not broadly advertise open registration until browser-worker egress controls and abuse safeguards are implemented. Never expose the Selenium or database ports directly.

## Security limitations

This is a prototype, not a production-audited service. Account registration does not yet include email verification, password reset, login/registration rate limiting or account recovery. Use HTTPS in hosted environments and do not reuse important passwords. HTTP Basic credentials are sent with API requests and must only be used over HTTPS.

URL validation plus post-navigation URL checking is not enough to secure arbitrary browser navigation: the browser might already have followed a redirect before the final URL is checked. Before inviting public users, isolate the browser worker behind an egress-restricted network or policy-enforcing proxy that blocks loopback, private, link-local and cloud-metadata destinations on every DNS resolution and redirect. Do not expose Selenium's WebDriver port publicly. Do not test websites without permission, bypass CAPTCHA/MFA or run arbitrary generated code.

Before production use, add registration/login rate limits, email verification and password recovery, explicit database migrations, quotas and hard execution deadlines, durable job queuing/retry policies, audit logs, retention rules, monitoring, private screenshot storage and a security review.
