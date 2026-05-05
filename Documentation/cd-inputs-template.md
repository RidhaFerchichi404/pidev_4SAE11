# CD Inputs Fill-Out Template

Use this template before enabling the CD job in Jenkins.

Primary entrypoint: `orchestration/full-stack-main` (root `Jenkinsfile`).

## Kubernetes / Environment
- Kubeconfig Jenkins credential ID: kubeconfig
- Kubernetes context (`kubernetes-admin@kubernetes`):
- Dev namespace: smart-freelance-dev
- Staging namespace: smart-freelance-staging
- Prod namespace: smart-freelance-prod
- kube-apiserver endpoint reachable from Jenkins (yes/no): YES

## Release / Images
- Registry prefix (`IMAGE_REPO`): docker.io/ridhaferchichi
- CI tag format (`IMAGE_TAG` style): Jenkins build number by default when IMAGE_TAG is empty; optional manual override supported.
- Promotion mode (auto/manual): manual (recommended currently); can be switched to auto by triggering CD downstream from CI with IMAGE_TAG.

## App Deployment Scope
- Manifest path (`MANIFEST_PATH`): k8s
- Services to deploy (deployment names): keycloak, eureka, config-server, keycloak-auth, user, project, offer, contract, portfolio, review, planning, task, notification, gamification, chat, meeting, freelancia-job, aimodel, api-gateway, frontend
- Any service excluded from CD rollout: vendor, aimodel-node, ticket-service, subcontracting (not present in current root k8s deployment manifests)

## Monitoring
- Deploy monitoring from CD (`DEPLOY_MONITORING` true/false):true
- Grafana exposure mode (NodePort/Ingress): NodePort
- If Ingress, host name: 
- Dashboards/KPIs required: platform overview baseline (healthy pods, CPU by pod, memory by pod, HTTP request rate). Add latency and error-rate panels once all services expose Prometheus metrics consistently.

## Access / Security
- Who can approve prod deployments: admin
- TLS requirement for Grafana: admin
- Alerting destination (optional now): me 
- Planning GitHub PAT source (choose one): `mdp.local` (`GITHUB_TOKEN` or `GITHUB_TOKEN_FILE`) / Jenkins credential ID / GitHub Actions secret
- Keycloak values source (required): `mdp.local` and/or Jenkins credentials mapped into render env
  - `KEYCLOAK_CLIENT_SECRET` (realm client `smart-freelance-backend`)
  - `KEYCLOAK_ADMIN_USERNAME`
  - `KEYCLOAK_ADMIN_PASSWORD`
  - `KEYCLOAK_ADMIN_CLIENT_SECRET` (when admin-cli authentication is enabled)
- Jenkins credential IDs in use:
  - Git checkout: `GithubCredentials`
  - Docker push: `DockerHubCrendentials`
  - kubeconfig secret file: `kubeconfig`
  - Optional: `github-token`, `mdp-local`, `firebase-admin-json`, `planning-calendar-json`, `meeting-calendar-json`
