# AGENTS.md

## Cursor Cloud specific instructions

### Architecture overview

This is a microservices platform (Smart Freelance) with:
- **Frontend**: Angular 21 SPA at `frontend/smart-freelance-app/` (port 4200)
- **Backend**: ~20 Java 17 / Spring Boot 4.x / Maven services under `backEnd/`
- **Infrastructure**: MySQL 8, Keycloak 26 (OAuth2/JWT), Eureka, Config Server, API Gateway
- **AI**: Node.js AImodel service + Ollama, Python ML inference (FastAPI)

### Running services (Docker Compose)

The full stack is defined in `docker-compose.yml`. Start infrastructure + core services:

```bash
# Start all infrastructure and core services
sudo docker compose up -d mysql keycloak eureka config-server keycloak-auth user project offer contract review planning task api-gateway
```

**Gotcha**: The `project` service has `mem_limit: 768m` which fails in nested Docker (cgroup v2 limitation). Use a `docker-compose.override.yml` with `services: project: mem_limit: 0` or remove the limit.

**Startup order** (enforced by healthchecks): MySQL → Keycloak → Eureka → Config Server → Keycloak Auth → Microservices → API Gateway → Frontend

### Running the frontend locally

```bash
cd frontend/smart-freelance-app
npm install
npx ng serve --host 0.0.0.0 --port 4200
```

The frontend connects to the API Gateway at `http://localhost:8078` (see `src/environments/environment.ts`).

### Keycloak / Auth notes

- Keycloak realm: `smart-freelance` (auto-imported from `keycloak/smart-freelance-realm.json`)
- Keycloak client: `smart-freelance-backend` (confidential, secret: `PpZuiHLBeLfl3xsxaQsmBDjT9nYgfoJE`)
- Default admin: `admin` / `admin`
- JWT issuer is `http://keycloak:8080/realms/smart-freelance` — add `127.0.0.1 keycloak` to `/etc/hosts` for local dev
- The realm uses email as username; when registering via the auth API, the actual Keycloak username becomes the email address

### Building backend services

```bash
cd backEnd/Microservices/<service>
mvn compile -DskipTests      # compile only
mvn test                     # run tests
mvn spring-boot:run          # run locally (requires MySQL + Eureka + Config Server)
```

Java 21 is installed and is backwards-compatible with the `java.version=17` target in pom.xml files.

### Running tests

- **Frontend**: `cd frontend/smart-freelance-app && npx ng test --no-watch --browsers=ChromeHeadless`
- **Backend**: `cd backEnd/<service-dir> && mvn test` (most require MySQL running)
- **CI tests**: `npm run test:ci` in the frontend runs tests with JUnit reporter

### Lint

No ESLint is configured for the frontend. TypeScript compilation (`ng build`) serves as the primary static check. Backend uses standard Java/Spring conventions (no explicit linter configured).

### Docker daemon in Cloud Agent

Docker requires special setup in the nested container environment:
- Storage driver: `fuse-overlayfs` (configured in `/etc/docker/daemon.json`)
- iptables: must use legacy mode (`iptables-legacy`, `ip6tables-legacy`)
- Start daemon manually: `sudo dockerd &>/tmp/dockerd.log &`

### Environment files

- `.env` at repo root (from `.env.example`) — contains MySQL password, Keycloak credentials, etc.
- Placeholder credential files needed for Docker mounts: `credentials/calendar-service-account.json`, `credentials/meeting-service-account.json`, `firebase-credentials/firebase-credentials.json`
