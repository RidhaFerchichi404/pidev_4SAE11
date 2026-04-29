# CD Inputs Fill-Out Template

Use this template before enabling the CD job in Jenkins.

## Kubernetes / Environment
- Kubeconfig Jenkins credential ID:
- Kubernetes context (`KUBE_CONTEXT`):
- Dev namespace:
- Staging namespace:
- Prod namespace:
- kube-apiserver endpoint reachable from Jenkins (yes/no):

## Release / Images
- Registry prefix (`IMAGE_REPO`):
- CI tag format (`IMAGE_TAG` style):
- Promotion mode (auto/manual):

## App Deployment Scope
- Manifest path (`MANIFEST_PATH`):
- Services to deploy (deployment names):
- Any service excluded from CD rollout:

## Monitoring
- Deploy monitoring from CD (`DEPLOY_MONITORING` true/false):
- Grafana exposure mode (NodePort/Ingress):
- If Ingress, host name:
- Dashboards/KPIs required:

## Access / Security
- Who can approve prod deployments:
- TLS requirement for Grafana:
- Alerting destination (optional now):
