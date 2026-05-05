<div align="center">

[![ESPRIT Tunisia](https://upload.wikimedia.org/wikipedia/commons/b/b6/Logo_ESPRIT_-_Tunisie.png)](https://www.esprit.tn)

**École Supérieure Privée d'Ingénierie et de Technologie — Tunisie**

# Smart Freelance & Project Matching Platform

*PI Dev — 4SAE11 — Academic Year 2024/2025*

A microservices-based platform connecting freelancers and clients for project collaboration, featuring AI-powered skill verification, portfolio management, real-time notifications, and GitHub integration.

[Features](#features) • [Architecture](#architecture) • [Getting Started](#getting-started) • [Documentation](#documentation)

</div>

---

## Overview

**Smart Freelance & Project Matching Platform** is a full-stack web application built with a microservices architecture. Clients can post projects, browse freelancer profiles, and hire talent; freelancers can showcase portfolios, apply to jobs, manage offers, and track progress with calendar and GitHub integration. Role-specific dashboards (Client, Freelancer, Admin) provide tailored KPIs, quick actions, and feeds.

---

## Features

| Role | Capabilities |
|------|--------------|
| **Freelancers** | Portfolio with experiences & skills, AI skill verification, browse jobs, submit applications, manage offers, reviews & ratings (with response messaging), contracts, notifications, calendar, GitHub sync |
| **Clients** | Project CRUD, job posting, browse freelancers/offers, progress tracking, track progress updates |
| **Admins** | User management, projects/contracts/offers oversight, planning, reviews, skill management, GitHub, evaluations |

---

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌──────────────────────────────────────────────────────────────────┐
│   Angular   │──── │ API Gateway  │──── │ User │ Project │ Offer │ Contract │ Portfolio │ Review            │
│  Frontend   │     │   (8078)     │     │ Planning │ Notification │ Task │ Gamification │ Vendor            │
│  (4200)     │     └──────────────┘     │ Ticket │ Subcontracting │ AImodel (Spring + Ollama)               │
│             │                          │ FreelanciaJob │ Chat │ Meeting │ Keycloak auth MS                 │
└─────────────┘            │             └──────────────────────────────────────────────────────────────────┘
                           │
                    ┌──────┴──────┐
                    │   Config    │
                    │   Server    │
                    │   (8888)    │
                    └─────────────┘
```

**Inter-service calls:** Review → Notification (when a review response is received); Planning/Task/Offer → Notification.

### Tech Stack

| Layer | Technologies |
|-------|--------------|
| **Frontend** | Angular 21, Bootstrap 5, Chart.js, TypeScript 5.9, SCSS, View Transitions (route animations) |
| **Backend** | Java 17, Spring Boot 3.4/4.0, Spring Cloud (Eureka, Config, Gateway), OpenFeign, Resilience4j |
| **Security** | Keycloak (OAuth2/JWT) |
| **Database** | MySQL 8 (one DB per microservice) |
| **APIs** | SpringDoc / OpenAPI (Swagger) |
| **Extras** | Firebase (notifications), AImodel + Ollama (LLM), skill verification, GitHub integration |

---

## Getting Started

### Prerequisites

- **Java 17**
- **Maven 3.8+**
- **Node.js 18+** and **npm**
- **MySQL 8** — repos use `localhost:3307` (see [Documentation/services-and-ports.md](Documentation/services-and-ports.md))
- **Keycloak** (standalone) on `localhost:8080` with realm `smart-freelance`

### Service Ports

| Service | Port | Database |
|---------|------|----------|
| Eureka | 8420 | — |
| Config Server | 8888 | — |
| API Gateway | 8078 | — |
| Keycloak Auth | 8079 | — |
| User | 8090 | `userdb` |
| Planning | 8081 | `planningdb` |
| Offer | 8082 | `gestion_offre_db` |
| Contract | 8083 | `gestion_contract_db` |
| Project | 8084 | `projectdb` |
| Review | 8085 | `reviewdb` |
| Portfolio | 8086 | `portfolio_db` |
| Notification | 8098 | Firebase |
| Gamification | 8088 | `gamificationdb` |
| Task | 8091 | `taskdb` |
| FreelanciaJob | 8097 | `freelancia_job_db` |
| Vendor | 8093 | `gestion_vendor_db` |
| Ticket | 8094 | `ticketdb` |
| AImodel (Spring AI + Ollama) | 8095 | — (Ollama, no app DB) |
| Chat | 8096 | `chatdb` |
| Meeting | 8101 | `meetingdb` |
| Subcontracting | 8110 | `gestion_subcontracting_db` |

### Startup Order

1. MySQL  
2. **Eureka** → `backEnd/Eureka`  
3. **Config Server** → `backEnd/ConfigServer` *(required for **Offer**, **Vendor**, **Subcontracting**, **Task**, and **Planning**; optional for others)*  
4. **Keycloak server** (`keycloak-start` image/deployment)  
5. **Keycloak Auth** → `backEnd/KeyCloak`  
6. **API Gateway** → `backEnd/apiGateway`  
7. **Microservices** — User, Project, Offer, Contract, Portfolio, Review, Planning, Notification, Task, Gamification, Vendor, Ticket, Subcontracting, FreelanciaJob, Chat, Meeting, **AImodel** (Spring + Ollama if using AI)  

### Run the Backend

```bash
# Example: start Eureka
cd backEnd/Eureka
mvn spring-boot:run

# Example: start User service
cd backEnd/Microservices/user
mvn spring-boot:run
```

**Tip:** From the repository root, `start-backend.sh` or `.\start-backend.ps1` can boot Eureka, Config, the gateway, and microservices in waves (logs and PIDs under `logs/`). Use `stop-backend.sh` or `.\stop-backend.ps1` to tear down.

### Run the Frontend

```bash
cd frontend/smart-freelance-app
npm install
npm start
```

Open **http://localhost:4200**

### API Documentation

Swagger UI is available via the Gateway for services that expose it (use the gateway path prefix in the URL).

Example: `http://localhost:8078/user/swagger-ui.html`

Full route and port reference: [Documentation/api-gateway.md](Documentation/api-gateway.md).

---

## Project Structure

```
├── backEnd/
│   ├── apiGateway/          # Spring Cloud Gateway
│   ├── ConfigServer/        # Centralized configuration
│   ├── Eureka/              # Service discovery
│   ├── KeyCloak/            # Auth microservice (OAuth2/JWT)
│   └── Microservices/
│       ├── Contract/        # Contract management
│       ├── Notification/    # Push notifications (Firebase)
│       ├── Offer/           # Offers & applications
│       ├── planning/        # Calendar, GitHub sync, progress updates
│       ├── task/            # Tasks, subtasks, AI-assisted endpoints
│       ├── gamification/    # Achievements, levels, XP
│       ├── Vendor/          # Vendor / agrément workflows
│       ├── ticket-service/   # Support tickets
│       ├── Subcontracting/  # Subcontracting workflow management
│       ├── FreelanciaJob/   # Job posting and matching flows
│       ├── Chat/            # Real-time/direct messaging
│       ├── Meeting/         # Meeting scheduling and calendar integration
│       ├── AImodel/         # Spring Boot + Ollama LLM API
│       ├── Portfolio/       # Portfolio, skills, AI verification
│       ├── Project/         # Project management
│       ├── review/          # Reviews & ratings (sends notifications on response)
│       └── user/            # User profiles
├── frontend/
│   └── smart-freelance-app/ # Angular SPA
├── Documentation/           # Architecture, gateway, per-service docs (see README there)
├── scripts/                 # DB seed SQL, GitHub token helper (see scripts/README.md)
├── credentials/             # Local creds layout (gitignored files; see credentials/README.md)
├── firebase-credentials/  # Firebase key layout (see firebase-credentials/README.md)
├── logs/                    # Runtime logs / PID files when using start-backend.* scripts
├── plans/                   # Implementation specs
├── start-backend.bat        # Windows: launches start-backend.ps1
├── start-backend.ps1        # Windows: ordered backend + optional Angular
├── start-backend.sh         # Linux/macOS: same idea
├── stop-backend.bat         # Windows: launches stop-backend.ps1
├── stop-backend.ps1         # Windows: stop processes recorded in logs/pids.txt
└── stop-backend.sh          # Linux/macOS: stop via logs/pids.txt
```

---

## Jenkins Seed + kubeadm CI/CD

This repository now supports **Jenkins Seed Job + Job DSL** automation for microservices on `main`, without Dockerizing Jenkins.

### What was added

- `jobs.groovy` creates:
  - `services/<service-name>` pipeline jobs (one per verified buildable service)
  - `orchestration/full-stack-main` — single unified pipeline (CI for all services + optional Kubernetes deploy via root `Jenkinsfile`; no separate CD seed job)
- Per-service Jenkinsfiles now follow one standardized flow:
  - Checkout -> Build -> Test -> Package -> Docker build/tag -> Docker push -> Kubernetes deploy -> rollout verify
  - Build tool autodetection: Maven, Gradle, Node
- Root `Jenkinsfile` orchestrates dependency-aware build/deploy order:
  - infra first, then parallel-safe services, then dependent services, then gateway, then frontend
- Kubernetes manifests use `imagePullPolicy: IfNotPresent` for local kubeadm-friendly behavior.
- Service dependency inventory is documented in `ci/service-catalog.yaml`.

### Required Jenkins plugins

- Pipeline
- Git + Git Client
- Credentials + Credentials Binding
- Job DSL
- Pipeline: Groovy
- Pipeline: Build Step
- Workspace Cleanup

If Jenkins agents do not already have them, install these CLIs on the Jenkins VM/agent host:
- `docker`
- `kubectl`
- Java + Maven + Node/npm (according to jobs you run)

### One-time Jenkins UI bootstrap (single pipeline)

1. **Create credentials** (`Manage Jenkins` -> `Credentials`) using these IDs (or set custom IDs in job params):
   - `GithubCredentials` (**Username with password** or PAT) for Git checkout.
   - `DockerHubCrendentials` (**Username with password/token**) for image push.
   - `kubeconfig` (**Secret file**) containing kubeconfig with context `kubernetes-admin@kubernetes`.
   - Optional integrations:
     - `github-token` (**Secret text**) -> set `GITHUB_TOKEN_CREDENTIALS_ID=github-token`.
     - `mdp-local` (**Secret file**) -> set `MDP_FILE_CREDENTIALS_ID=mdp-local`.
     - `firebase-admin-json` (**Secret file**) -> set `FIREBASE_CREDENTIALS_ID=firebase-admin-json`.
     - `planning-calendar-json` (**Secret file**) -> set `PLANNING_CALENDAR_CREDENTIALS_ID=planning-calendar-json`.
     - `meeting-calendar-json` (**Secret file**) -> set `MEETING_CALENDAR_CREDENTIALS_ID=meeting-calendar-json`.
2. **Install Job DSL plugin** if missing.
3. **Create Seed job**:
   - New Item -> Freestyle -> name: `seed-jobs`
   - Source Code Management -> Git -> Repository URL: your repo URL
   - Branch: `*/main`
   - Build step -> **Process Job DSLs**
   - DSL scripts: `jobs.groovy`
   - Save
4. **Run `seed-jobs` once** to generate all pipeline jobs.
5. Open generated `orchestration/full-stack-main` and click **Build with Parameters**:
   - Keep defaults for one-click run (`BRANCH=main`, `PUSH_IMAGE=true`, `DEPLOY_TO_K8S=true`).
   - Set only project-specific values if needed: `REPO_URL`, `IMAGE_REPO`, `PUBLIC_API_GATEWAY_URL`.
   - If you used custom credential IDs, override the corresponding `*_CREDENTIALS_ID` parameters.
   - Optional CD-only dry test: set `DRY_RUN_ONLY=true`.
   - Keep Keycloak values consistent across secrets and auth service config:
     - `KEYCLOAK_CLIENT_SECRET` (realm client `smart-freelance-backend`)
     - `KEYCLOAK_ADMIN_USERNAME` / `KEYCLOAK_ADMIN_PASSWORD`
     - `KEYCLOAK_AUTH_SERVER_URL` (defaults to in-cluster `http://keycloak:8080`)

### How Jenkins connects to kubeadm securely

- Store kubeconfig in Jenkins as a **Secret file** credential (`kubeconfig-file-credential-id`).
- Pipelines copy it at runtime to workspace-local `.kube/config` with `chmod 600`.
- `kubectl` commands run with explicit namespace (`smart-freelance` by default).
- No kubeconfig is committed to git.

### Image build and deployment model

- Images are built in each service directory and pushed to Docker Hub as:
  - `${IMAGE_REPO}/${service}:${BUILD_NUMBER}` and `:latest`
- Deployments are updated with `kubectl set image` and validated via `kubectl rollout status`.
- Full-stack orchestration applies manifests under `k8s/` (including optional Ingress and phpMyAdmin when enabled).

### Browser access on the kubeadm VM (app, Keycloak, phpMyAdmin)

You do **not** need to change the app manifests for a standard ingress-nginx bare-metal install: `k8s/10-ingress.yaml` already uses `ingressClassName: nginx`. Optionally customize **hostnames** in that file and match them in Jenkins (`PUBLIC_API_GATEWAY_URL`) and `/etc/hosts`.

1. **Install ingress-nginx (bare-metal / kubeadm)** — official manifest (pin the version if you prefer):
   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.15.1/deploy/static/provider/baremetal/deploy.yaml
   ```
   Wait for pods in `ingress-nginx` to be ready. HTTP is usually on a **NodePort**; get it with:
   ```bash
   kubectl get svc -n ingress-nginx ingress-nginx-controller -o wide
   ```
   Open the app as `http://<node-ip>:<http-nodeport>/` while the browser sends the correct `Host` header (via `/etc/hosts` mapping those names to `<node-ip>`).
2. **Map hostnames on the VM** (e.g. `/etc/hosts`) to your **node IP** (same IP you use with the NodePort). Include at least:
   - `smartfreelance.example.com` (Angular frontend)
   - `api.smartfreelance.example.com` (API gateway; must match **`PUBLIC_API_GATEWAY_URL`** in the Jenkins job so the production bundle calls the right host)
   - `auth.smartfreelance.example.com` (Keycloak; must match **`KC_HOSTNAME`** in `k8s/04-keycloak.yaml` and Ingress)
   - `db.smartfreelance.example.com` (phpMyAdmin)
3. **Pipeline / Job DSL:** leave **`DEPLOY_INGRESS`** enabled (default) so `k8s/10-ingress.yaml` is applied. Disable only if you have no Ingress controller.
4. **Optional:** If JWT validation fails after logging in via the public auth hostname, see the comment block in `k8s/01-configmap.yaml` (issuer/JWKS vs in-cluster `keycloak` DNS).

No extra Jenkins plugins are required for browser access.

### Full deploy vs single service deploy

- **Full stack (single entrypoint):** run `orchestration/full-stack-main`.
  - Pipeline flow: Checkout -> Build/Test/Package -> Docker Build/Push (all deployable services; excludes `vendor` and `aimodel-node`) -> Render Secrets/Manifests (includes `ollama`, Firebase/Calendar/GitHub integrations) -> Apply -> Rollout checks.
  - No manual step is required for `dev`/`staging`; `prod` requires approval only when `REQUIRE_PROD_APPROVAL=true`.
- **Single service:** run `services/<service-name>` (per-service build/push only; no cluster deploy from that job).

### Adding a new service later

1. Add service directory + Dockerfile + service `Jenkinsfile`.
2. Add service entry to `jobs.groovy` and `ci/service-catalog.yaml` (path, image, dependencies, deployment name).
3. Add Kubernetes Deployment/Service manifests (or update `k8s/08-microservices.yaml`).
4. Re-run `seed-jobs` to regenerate jobs.

### Current scope note

- `vendor` and `aimodel-node` are intentionally excluded from CI/CD orchestration.
- `aimodel` (Spring Boot) is deployed with in-cluster `ollama` (`k8s/12-ollama.yaml`).
- Notification and Planning credentials are auto-rendered to `k8s/02-secrets.generated.yaml` by `scripts/render-k8s-secrets.py` using `mdp.local` + local credential files.

### Local kubeadm deploy (non-interactive)

```powershell
.\scripts\init-mdp-local.ps1
.\scripts\verify-cicd.ps1 -Namespace smart-freelance
.\scripts\deploy-kubeadm.ps1 -Namespace smart-freelance -ImageRepo docker.io/<dockerhub-user> -ImageTag latest
```

---

## Optional Integrations

| Integration | Purpose | Configuration |
|-------------|---------|---------------|
| **Google Translate** | Offer translations | API key in Offer service |
| **Firebase** | Push notifications (Firestore) | Credentials in Notification service |
| **GitHub** | Planning sync, commit history | See [credentials/README.md](credentials/README.md) — token in `githubToken.txt` or `$env:GITHUB_TOKEN` (never committed) |
| **AImodel** | LLM generation (Task/Copilot flows) | Ollama URL + model in AImodel service; see [Documentation/services/AImodel.md](Documentation/services/AImodel.md) |
| **Portfolio AI** | Skill verification (if enabled) | API keys / config in Portfolio service |

All credential files are gitignored. See [credentials/README.md](credentials/README.md) for setup.

---

## Documentation

- **[Documentation hub](Documentation/README.md)** — architecture, gateway, frontend guide, per-service pages
- [Keycloak Setup](backEnd/KeyCloak/README.md) — Auth & realm configuration
- [Credentials Setup](credentials/README.md) — GitHub token, Google Calendar, Firebase
- [Portfolio Test Plan](Documentation/TEST_PLAN_PORTFOLIO.md)
- [Implementation Specs](plans/)

---

## Contributing

This project is developed as part of the **PI Dev** course at ESPRIT Tunisia. For contributions, please follow the existing code style and open a pull request.

---

<div align="center">

**ESPRIT — École Supérieure Privée d'Ingénierie et de Technologie — Tunisie**

*4SAE11 • PI Dev • 2024/2025*

`#Angular` `#SpringBoot` `#Microservices` `#Keycloak` `#MySQL` `#Freelance` `#FullStack` `#TypeScript` `#Java` `#ESPIRIT` `#Tunisia`

</div>
