# CD Runbook (kubeadm + Prometheus/Grafana)

## 1) Purpose
This runbook explains how to use the standalone CD pipeline at `ci/pipelines/kubeCdPipeline.groovy` to deploy Kubernetes manifests and monitoring components.

## 2) Prerequisites
- Jenkins agent has `kubectl` and `python3`.
- Jenkins has credential `KubeconfigFile` (type: **Secret file**) that contains kubeconfig for your kubeadm cluster.
- kubeconfig includes a valid context for `KUBE_CONTEXT`.
- Docker images with immutable tags are already pushed by CI.

## 3) Required Inputs
- `REPO_URL`: Git URL containing manifests.
- `BRANCH`: branch to deploy.
- `KUBECONFIG_CREDENTIALS_ID`: secret file credentials id.
- `KUBE_CONTEXT`: context from `kubectl config get-contexts`.
- `KUBE_NAMESPACE`: target namespace (for example `smart-freelance-dev`).
- `MANIFEST_PATH`: app manifest folder (default `k8s`).
- `IMAGE_REPO`: image prefix (for example `docker.io/myorg`).
- `IMAGE_TAG`: immutable release tag from CI.

## 4) Optional Inputs
- `DEPLOY_MONITORING`: apply monitoring stack from `k8s/monitoring`.
- `MONITORING_MANIFEST_PATH`: monitoring manifests location.
- `ENVIRONMENT` + `REQUIRE_PROD_APPROVAL`: adds manual gate for prod.
- `ROLLBACK_ON_FAILURE`: auto rollback Deployments on failed rollout checks.
- `DRY_RUN_ONLY`: server-side dry run without changing cluster state.

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
1. Trigger CD job with CI-produced `IMAGE_TAG`.
2. Confirm `KUBE_CONTEXT`, namespace, and manifest path.
3. For production, approve the manual gate.
4. Verify rollout in Jenkins logs and smoke-check output.

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
