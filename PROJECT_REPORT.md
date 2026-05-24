# Smart Freelance & Project Matching Platform — Project Report

**Institution:** ESPRIT — École Supérieure Privée d’Ingénierie et de Technologie, Tunisia  
**Course:** PI Dev — 4SAE11  
**Academic year:** 2024/2025  
**Document language:** English  

---

## 1. Executive summary

This report describes **Smart Freelance & Project Matching Platform**, a full-stack web application that connects clients and freelancers for project-based collaboration. The solution implements a **microservices architecture** with an **Angular** single-page application, a **Spring Cloud** backend (Eureka, Config Server, API Gateway), **Keycloak** for identity and access management, **MySQL** databases per service, and multiple integrations (notifications, calendars, GitHub, LLM-based assistance, and optional AI skill verification).

The project delivers **role-based experiences** for freelancers, clients, and administrators, covering the end-to-end lifecycle from discovery and applications through contracts, tasks, planning, reviews, and operational support (tickets, subcontracting, vendor workflows). DevOps coverage includes **Jenkins** seed jobs, standardized per-service pipelines, and **Kubernetes** deployment paths aligned with the repository’s `k8s` manifests and orchestration documentation.

---

## 2. Context and objectives

### 2.1 Context

The platform was developed in the context of an academic **integration project (PI Dev)** requiring analysis, design, implementation, and documentation of a realistic software product. The domain—freelance marketplaces and project matching—demands secure authentication, clear separation of concerns across teams or modules, and extensibility for new features (chat, meetings, gamification, AI assistance).

### 2.2 Primary objectives

- Provide a **secure, scalable** backend using **distributed services** rather than a monolith.
- Offer a **modern web client** with dashboards tailored to **Freelancer**, **Client**, and **Admin** roles.
- Support **core business flows**: projects, offers, contracts, portfolio, reviews, planning, tasks, notifications, and cross-cutting concerns (support tickets, subcontracting, jobs, chat, meetings).
- Integrate **AI/ML capabilities** where appropriate: LLM-assisted flows (Ollama/Spring AI), portfolio skill verification, and a separate **ML workspace** for experimentation (classification, regression, clustering on schema-driven datasets).
- Establish **CI/CD** practices: Jenkins Job DSL, Docker image build/push, and optional **kubeadm**-oriented Kubernetes deployment.

---

## 3. Scope and functional achievements

### 3.1 Freelancers

- Portfolio management (experiences, skills).
- **AI-assisted skill verification** (where configured).
- Job and offer discovery, **applications**, and offer management.
- **Reviews and ratings** with **response messaging**; integration with notifications when responses occur.
- **Contracts**, **notifications**, **calendar** usage, and **GitHub** synchronization (via planning-related capabilities).
- Engagement via **gamification** (achievements, levels, XP).

### 3.2 Clients

- **Project CRUD** and job posting.
- Browsing freelancers and offers.
- **Progress tracking** and updates aligned with planning/task flows.

### 3.3 Administrators

- Oversight of **users**, **projects**, **contracts**, **offers**, **planning**, **reviews**, **skills**, **GitHub-related** configuration, and **evaluations**.

### 3.4 Cross-cutting modules

The following bounded contexts were implemented or integrated as dedicated services (non-exhaustive): **User**, **Project**, **Offer**, **Contract**, **Portfolio**, **Review**, **Planning**, **Notification** (Firebase), **Task**, **Gamification**, **Vendor**, **Ticket**, **Subcontracting**, **FreelanciaJob**, **Chat**, **Meeting**, and **AImodel** (Spring Boot + Ollama). Inter-service behavior is documented (e.g., Review → Notification on review responses; Planning/Task/Offer → Notification for relevant events).

---

## 4. Technical achievements

### 4.1 Frontend

- **Angular** SPA (modern TypeScript stack), **Bootstrap 5**, **Chart.js**, **SCSS**, and **view transitions** for route animations.
- Role-specific dashboards, KPIs, quick actions, and feeds as described in project documentation.

### 4.2 Backend and integration

- **Java 17**, **Spring Boot 3.4/4.0**, **Spring Cloud** (Eureka, Config, Gateway), **OpenFeign**, **Resilience4j**.
- **OpenAPI/SpringDoc (Swagger)** exposure for API contracts and testing via the gateway.
- **Keycloak** realm (`smart-freelance`) and a dedicated **Keycloak auth microservice** for OAuth2/JWT alignment with the gateway and SPA.
- **Per-service MySQL** databases (documented ports and schema names).
- Optional integrations: **Google Translate** (offers), **Firebase** (push), **GitHub** tokens for planning sync, **AImodel** configuration for Ollama-backed endpoints.

### 4.3 Machine learning workspace (analytical track)

A dedicated **`ml-workspace`** documents pipelines and exported metrics, including:

- **Classification** (`success_flag`): best F1 achieved with **Gradient Boosting** (example exported metrics: accuracy ≈ 0.97, F1 ≈ 0.98 on the documented synthetic run).
- **Regression** (`client_satisfaction_score`): **linear regression** reported as best by R² in the exported comparison (R² ≈ 0.86 on the documented run).
- **Clustering** (client segmentation): **K-Means** selected by silhouette in the documented experiment, with explicit notes on **DBSCAN** parameter sensitivity and synthetic-data limitations.

These results are framed in documentation as **pipeline validation** on synthetic/schema-driven data, not as production-ground-truth analytics.

### 4.4 DevOps and deployment

- **Jenkins Seed Job + Job DSL** (`jobs.groovy`) generating per-service pipelines and an **`orchestration/full-stack-main`** unified pipeline.
- Standardized stages: checkout, build, test, package, Docker build/tag/push, Kubernetes deploy, rollout verification; **build-tool autodetection** (Maven, Gradle, Node).
- Root **`Jenkinsfile`** with dependency-aware ordering; **`ci/service-catalog.yaml`** as a service inventory.
- **Kubernetes** manifests and ingress-oriented runbooks; secrets rendering scripts for non-interactive local kubeadm-style workflows.
- Documented credential model (GitHub, Docker Hub, kubeconfig, optional Firebase/calendar secrets) without committing secrets to the repository.

### 4.5 Documentation and quality

- Central **Documentation** hub: architecture, API gateway routes, per-service pages, test plans (e.g., portfolio), CD runbooks, and port matrices.
- Backend **README** files per major component; scripts for DB seeding and environment helpers.
- **Unit testing** documentation and reports under the backend (`UNIT_TEST_TYPES_REPORT.md`).

---

## 5. Architecture summary

The system follows a classic **edge → gateway → microservices** pattern:

1. **Angular** (port 4200) talks to the **API Gateway** (8078).
2. The gateway routes to domain microservices registered with **Eureka**; many services consume **Config Server** for externalized configuration (required for selected services such as Offer, Vendor, Subcontracting, Task, and Planning).
3. **Keycloak** provides identity; the **Keycloak auth** microservice bridges application security needs.
4. **CORS** and timeout policies on the gateway accommodate browser clients and long-running AI routes where applicable.

A high-level component view is maintained in `Documentation/architecture.md` (Mermaid and ASCII diagrams).

---

## 6. Project structure (deliverables)

The repository is organized for clarity and operability:

- **`backEnd/`** — Eureka, Config Server, API Gateway, Keycloak integration service, and all microservice modules.
- **`frontend/smart-freelance-app/`** — Angular application.
- **`Documentation/`** — Architecture, gateway, service-specific guides, CD templates.
- **`k8s/`**, **`scripts/`**, **`jobs.groovy`**, **`Jenkinsfile`** — deployment and automation assets.
- **`ml-workspace/`** — notebooks/scripts and ML documentation.
- **`plans/`**, **`UI_UX_Redesign_Spec.md`** — implementation and UX specifications.

---

## 7. Challenges and design decisions

- **Distributed complexity:** Multiple databases and network boundaries require clear API contracts (OpenAPI), resilient clients (Feign + resilience patterns), and disciplined startup order locally and in Kubernetes.
- **Security:** Centralized auth with Keycloak and JWT validation paths that must stay consistent across gateway, cluster ingress hostnames, and issuer/JWKS configuration (documented in deployment notes).
- **Observability and operations:** Log and PID management via provided `start-backend` / `stop-backend` scripts for developer ergonomics; Jenkins/Kubernetes paths for staging-style environments.
- **AI scope:** Separation between **interactive LLM** features (AImodel + Ollama) and **offline ML** experimentation (ml-workspace) keeps responsibilities clear and avoids overstating production ML maturity.

---

## 8. Conclusion

The **Smart Freelance & Project Matching Platform** meets its stated goals as a **broad, integration-rich** product: a microservices backend, a modern Angular frontend, strong security baselines with Keycloak, extensive domain coverage (projects, offers, contracts, portfolio, reviews, planning, tasks, notifications, gamification, support, subcontracting, jobs, chat, meetings), and a documented path to **containerized CI/CD** and **Kubernetes** deployment. Complementary **ML workspace** artifacts demonstrate structured experimentation for classification, satisfaction regression, and client segmentation, with transparent limitations on synthetic data.

The project constitutes a coherent **full-stack and DevOps** deliverable suitable for academic evaluation and as a foundation for future hardening (production monitoring, hardened secret management, broader automated tests, and real telemetry–driven ML).

---

## 9. References (in-repository)

- Root **`README.md`** — features, stack, ports, startup order, CI/CD overview.
- **`Documentation/README.md`** and **`Documentation/architecture.md`** — architecture and doc index.
- **`ml-workspace/docs/`** — ML pipeline summaries and limitations.
- **`Documentation/api-gateway.md`** — gateway routes and integration notes.

---

*End of report.*
