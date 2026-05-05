# CD Runbook (kubeadm + Prometheus/Grafana)

## 1) Purpose
This runbook explains the Kubernetes CD stages used by the single unified pipeline (`Jenkinsfile`, generated as `orchestration/full-stack-main`) and the standalone CD pipeline (`ci/pipelines/kubeCdPipeline.groovy`).

## 2) Prerequisites
- Jenkins agent has `kubectl` and `python3`.
- Jenkins has secret file credential `kubeconfig` (or equivalent ID passed via parameter) that contains kubeconfig for your kubeadm cluster.
- kubeconfig includes a valid context for `KUBE_CONTEXT`.
- Docker images with immutable tags are already pushed by CI.
- Optional local debug file `mdp.local` at repo root (gitignored), generated from `mdp.example`.

## 2.1) Preflight (before pipeline run)
- Run `seed-jobs` first to regenerate `orchestration/full-stack-main` from `jobs.groovy`.
- Confirm Keycloak server image build is enabled in root `Jenkinsfile` (`keycloak-start/Dockerfile` -> `${IMAGE_REPO}/keycloak:${IMAGE_TAG|BUILD_NUMBER}`).
- Ensure these secrets are available via `mdp.local` or Jenkins credentials-bound env for `scripts/render-k8s-secrets.py`:
  - `KEYCLOAK_CLIENT_SECRET` (realm client secret for `smart-freelance-backend`)
  - `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD`
  - `KEYCLOAK_ADMIN_CLIENT_SECRET` (if `admin-cli` client authentication is enabled)
- Keep issuer alignment: `KEYCLOAK_AUTH_SERVER_URL` / `KEYCLOAK_SERVER_URL` should target the same Keycloak host used by JWT issuer (`KEYCLOAK_ISSUER_URI` in `k8s/01-configmap.yaml`).

## 3) Required Inputs (single-click defaults)
- `REPO_URL`: defaults to project repository URL.
- `BRANCH`: defaults to `main`.
- `GIT_CREDENTIALS_ID`: defaults to `GithubCredentials`.
- `DOCKER_CREDENTIALS_ID`: defaults to `DockerHubCrendentials` (root pipeline only).
- `KUBECONFIG_CREDENTIALS_ID`: defaults to `kubeconfig`.
- `KUBE_CONTEXT`: defaults to `kubernetes-admin@kubernetes`.
- `KUBE_NAMESPACE`: defaults to `smart-freelance-dev`.
- `MANIFEST_PATH`: defaults to `k8s`.
- `IMAGE_REPO`: set once to your Docker Hub namespace/repo prefix.
- `IMAGE_TAG`: empty by default -> Jenkins `BUILD_NUMBER`.

## 4) Optional Inputs
- `DEPLOY_MONITORING`: apply monitoring stack from `k8s/monitoring`.
- `MONITORING_MANIFEST_PATH`: monitoring manifests location.
- `ENVIRONMENT` + `REQUIRE_PROD_APPROVAL`: adds manual gate for prod.
- `ROLLBACK_ON_FAILURE`: auto rollback Deployments on failed rollout checks.
- `DRY_RUN_ONLY`: server-side dry run without changing cluster state.
- `GITHUB_TOKEN_CREDENTIALS_ID`: secret text for Planning GitHub integration.
- `MDP_FILE_CREDENTIALS_ID`: secret file for `mdp.local` values.
- `FIREBASE_CREDENTIALS_ID`: secret file for Notification Firebase admin JSON.
- `PLANNING_CALENDAR_CREDENTIALS_ID`: secret file for Planning Calendar service account JSON.
- `MEETING_CALENDAR_CREDENTIALS_ID`: secret file for Meeting Calendar service account JSON.

## 5) First-Time Cluster Preparation (your VM)
Run these once on the VM that hosts kubeadm/Jenkins:

```bash
kubectl get nodes -o wide
kubectl get sc
kubectl create namespace smart-freelance-dev --dry-run=client -o yaml | kubectl apply -f -
```

Recommended:
- Ensure a default StorageClass exists for Prometheus/Grafana PVCs.
- Open Grafana NodePort `32000` on VM firewall/security rules.
- Install metrics-server if not already available:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

## 6) Deploy Procedure
1. Run `seed-jobs` (if Job DSL changed), then trigger `orchestration/full-stack-main` (or CD-only job).
2. Confirm `KUBE_CONTEXT`, namespace, and manifest path.
3. For production, approve the manual gate.
4. Verify rollout in Jenkins logs and smoke-check output.

`ci/pipelines/kubeDeployLib.groovy` auto-renders `02-secrets.generated.yaml` via `scripts/render-k8s-secrets.py` (from Jenkins credential bindings, `mdp.local`, env vars, and fallback local credential files) before applying manifests.
For Planning GitHub integration, set either `GITHUB_TOKEN_CREDENTIALS_ID` or keep `GITHUB_TOKEN`/`GITHUB_TOKEN_FILE` in `mdp.local`.

### Dev Validation (first execution checklist)
Use these CD job values for the first safe test:
- `ENVIRONMENT=dev`
- `KUBE_NAMESPACE=smart-freelance-dev`
- `DRY_RUN_ONLY=true` (first pass)
- `DEPLOY_MONITORING=true`

Then run a real deploy:
- Same values, but set `DRY_RUN_ONLY=false`.

Verification commands on the VM:

```bash
kubectl -n smart-freelance-dev get deploy,pods,svc
kubectl -n monitoring get deploy,pods,svc,pvc
kubectl -n monitoring port-forward svc/prometheus 9090:9090
kubectl -n monitoring port-forward svc/grafana 3000:3000
```

Local non-interactive deployment (outside Jenkins):

```powershell
.\scripts\init-mdp-local.ps1
.\scripts\deploy-kubeadm.ps1 -Namespace smart-freelance -ImageRepo docker.io/<dockerhub-user> -ImageTag latest
```

## 7) Monitoring Access
- Grafana service: `monitoring/grafana` on NodePort `32000`.
- URL from your machine: `http://<VM_IP>:32000`.
- Default credentials:
  - user: `admin`
  - password: `admin123`

Change admin password immediately after first login.

## 8) Rollback Procedure
If deployment fails and auto rollback is disabled:

```bash
kubectl -n <your_namespace> get deploy
kubectl -n <your_namespace> rollout undo deployment/<deployment_name>
kubectl -n <your_namespace> rollout status deployment/<deployment_name>
```

## 9) Troubleshooting
- **Context error:** verify `KUBE_CONTEXT` value exists in kubeconfig.
- **Forbidden/unauthorized:** update kubeconfig user/cluster credentials.
- **PVC pending:** check StorageClass and node disk availability.
- **No metrics in Grafana:** verify targets in Prometheus and service ports/paths.
- **Pods not ready:** inspect events and logs:

```bash
kubectl -n <your_namespace> get events --sort-by=.metadata.creationTimestamp
kubectl -n <your_namespace> logs deploy/<deployment_name> --tail=200
```
