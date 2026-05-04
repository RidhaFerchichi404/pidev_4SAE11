/**
 * Kubernetes deploy steps (no checkout). Caller must have the repo workspace at the job root.
 * cfg keys: kubeContext, kubeNamespace, manifestPath, imageRepo, imageTag, kubeconfigCredentialsId,
 *   deployMonitoring, monitoringManifestPath, deployEnvironment, requireProdApproval, dryRunOnly,
 *   rollbackOnFailure, rolloutTimeoutSeconds, renderDir (optional), deployIngress (optional, default true)
 */
def runKubernetesDeploy(Map cfg) {
    def renderDir = (cfg.renderDir ?: ".cd-rendered").toString()
    def kubeContext = cfg.kubeContext?.trim() ?: error("kubeContext is required")
    def kubeNamespace = cfg.kubeNamespace?.trim() ?: error("kubeNamespace is required")
    def manifestPath = cfg.manifestPath?.trim() ?: "k8s"
    def imageRepo = cfg.imageRepo?.trim() ?: error("imageRepo is required")
    def imageTag = cfg.imageTag?.toString()?.trim() ?: error("imageTag is required")
    def kubeconfigCredentialsId = cfg.kubeconfigCredentialsId?.trim() ?: "kubeconfig"
    def deployIngress = cfg.deployIngress != false
    def deployMonitoring = cfg.deployMonitoring != false
    def monitoringManifestPath = cfg.monitoringManifestPath?.trim() ?: "k8s/monitoring"
    def deployEnvironment = (cfg.deployEnvironment ?: "dev").toString()
    def requireProdApproval = cfg.requireProdApproval != false
    def dryRunOnly = cfg.dryRunOnly == true
    def rollbackOnFailure = cfg.rollbackOnFailure != false
    def rolloutTimeoutSeconds = (cfg.rolloutTimeoutSeconds ?: "600").toString()

    def dryShell = dryRunOnly ? "true" : "false"

    stage("Validate K8s Inputs") {
        sh """
          set -e
          command -v kubectl >/dev/null 2>&1 || { echo 'kubectl is not installed on this Jenkins agent'; exit 1; }
          command -v python3 >/dev/null 2>&1 || { echo 'python3 is not installed on this Jenkins agent'; exit 1; }
          test -d "${manifestPath}" || { echo "Manifest path not found: ${manifestPath}"; exit 1; }
        """
    }

    if (deployEnvironment == "prod" && requireProdApproval) {
        stage("Production Approval") {
            timeout(time: 20, unit: "MINUTES") {
                input message: "Approve production deployment for tag ${imageTag}?"
            }
        }
    }

    try {
        stage("Cluster Connectivity") {
            withCredentials([file(credentialsId: kubeconfigCredentialsId, variable: "KUBECONFIG_FILE")]) {
                sh """
                  set -e
                  export KUBECONFIG="\$KUBECONFIG_FILE"
                  kubectl config use-context "${kubeContext}"
                  kubectl cluster-info
                  kubectl get nodes -o wide
                """
            }
        }

        stage("Render App Manifests") {
            withEnv([
                "KD_IMAGE_REPO=${imageRepo}",
                "KD_IMAGE_TAG=${imageTag}",
                "KD_KUBE_NAMESPACE=${kubeNamespace}",
                "KD_RENDER_DIR=${renderDir}",
                "KD_DEPLOY_INGRESS=${deployIngress ? 'true' : 'false'}"
            ]) {
                sh """
                  set -e
                  rm -rf "${renderDir}"
                  mkdir -p "${renderDir}/app"
                  cp -R "${manifestPath}/." "${renderDir}/app/"
                  rm -rf "${renderDir}/app/monitoring"
                  if [ "\$KD_DEPLOY_INGRESS" != "true" ]; then
                    rm -f "${renderDir}/app/10-ingress.yaml"
                  fi
                  python3 - <<'PY'
import pathlib
import re
import os

repo = os.environ["KD_IMAGE_REPO"].strip().rstrip("/")
tag = os.environ["KD_IMAGE_TAG"].strip()
namespace = os.environ["KD_KUBE_NAMESPACE"].strip()
target = pathlib.Path(os.environ["KD_RENDER_DIR"]) / "app"

if repo.startswith("docker.io/"):
    repo = repo[len("docker.io/"):]
elif repo.startswith("index.docker.io/"):
    repo = repo[len("index.docker.io/"):]

pattern = re.compile(r"YOUR_DOCKERHUB_USERNAME/([A-Za-z0-9._-]+)(?::[A-Za-z0-9._-]+)?")
namespace_pattern = re.compile(r"^(\\s*namespace:\\s*)smart-freelance\\s*(\\r?\\n|\\Z)", re.MULTILINE)

for file in target.rglob("*.y*ml"):
    data = file.read_text(encoding="utf-8")
    updated = pattern.sub(lambda m: f"{repo}/{m.group(1)}:{tag}", data)
    updated = namespace_pattern.sub(lambda m: f"{m.group(1)}{namespace}{m.group(2)}", updated)
    if file.name == "00-namespace.yaml":
        updated = re.sub(r"^(\\s*name:\\s*)smart-freelance\\s*(\\r?\\n|\\Z)", lambda m: f"{m.group(1)}{namespace}{m.group(2)}", updated, flags=re.MULTILINE)
    if updated != data:
        file.write_text(updated, encoding="utf-8")
PY
                  if [ -f "${renderDir}/app/02-secrets.yaml" ] && grep -nE ':[[:space:]]*""[[:space:]]*(#.*)?' "${renderDir}/app/02-secrets.yaml" >/dev/null; then
                    echo "WARNING: Incomplete values detected in 02-secrets.yaml; skipping this manifest for this deploy."
                    mv "${renderDir}/app/02-secrets.yaml" "${renderDir}/app/02-secrets.yaml.skipped"
                  fi
                """
            }
        }

        if (deployMonitoring) {
            stage("Render Monitoring Manifests") {
                withEnv([
                    "KD_KUBE_NAMESPACE=${kubeNamespace}",
                    "KD_RENDER_DIR=${renderDir}"
                ]) {
                    sh """
                      set -e
                      rm -rf "${renderDir}/monitoring"
                      mkdir -p "${renderDir}/monitoring"
                      cp -R "${monitoringManifestPath}/." "${renderDir}/monitoring/"
                      python3 - <<'PY'
import pathlib
import re
import os

app_namespace = os.environ["KD_KUBE_NAMESPACE"].strip()
target = pathlib.Path(os.environ["KD_RENDER_DIR"]) / "monitoring"

for file in target.rglob("*.y*ml"):
    data = file.read_text(encoding="utf-8")
    updated = re.sub(r"smart-freelance-dev", app_namespace, data)
    if updated != data:
        file.write_text(updated, encoding="utf-8")
PY
                    """
                }
            }
        }

        stage("Deploy Application") {
            withCredentials([file(credentialsId: kubeconfigCredentialsId, variable: "KUBECONFIG_FILE")]) {
                sh """
                  set -e
                  export KUBECONFIG="\$KUBECONFIG_FILE"
                  kubectl config use-context "${kubeContext}"
                  kubectl create namespace "${kubeNamespace}" --dry-run=client -o yaml | kubectl apply -f -

                  if [ "${dryShell}" = "true" ]; then
                    kubectl apply --server-side --dry-run=server -f "${renderDir}/app"
                  else
                    kubectl apply -f "${renderDir}/app"
                  fi
                """
            }
        }

        if (deployMonitoring) {
            stage("Deploy Monitoring Stack") {
                withCredentials([file(credentialsId: kubeconfigCredentialsId, variable: "KUBECONFIG_FILE")]) {
                    sh """
                      set -e
                      export KUBECONFIG="\$KUBECONFIG_FILE"
                      kubectl config use-context "${kubeContext}"
                      test -d "${monitoringManifestPath}" || { echo "Monitoring manifest path not found: ${monitoringManifestPath}"; exit 1; }
                      test -d "${renderDir}/monitoring" || { echo "Rendered monitoring path not found: ${renderDir}/monitoring"; exit 1; }
                      kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -

                      if [ "${dryShell}" = "true" ]; then
                        kubectl apply --server-side --dry-run=server -f "${renderDir}/monitoring"
                      else
                        kubectl apply -f "${renderDir}/monitoring"
                      fi
                    """
                }
            }
        }

        if (!dryRunOnly) {
            stage("Rollout Verification") {
                withCredentials([file(credentialsId: kubeconfigCredentialsId, variable: "KUBECONFIG_FILE")]) {
                    sh """
                      set -e
                      export KUBECONFIG="\$KUBECONFIG_FILE"
                      kubectl config use-context "${kubeContext}"

                      deployments=\$(kubectl -n "${kubeNamespace}" get deploy -o jsonpath='{range .items[*]}{.metadata.name}{"\\n"}{end}')
                      if [ -z "\$deployments" ]; then
                        echo "No deployments found in namespace ${kubeNamespace}"
                      else
                        for d in \$deployments; do
                          echo "Waiting rollout for deployment/\$d"
                          kubectl -n "${kubeNamespace}" rollout status "deployment/\$d" --timeout="${rolloutTimeoutSeconds}s"
                        done
                      fi
                    """
                }
            }

            stage("Post Deploy Smoke Checks") {
                withCredentials([file(credentialsId: kubeconfigCredentialsId, variable: "KUBECONFIG_FILE")]) {
                    sh """
                      set -e
                      export KUBECONFIG="\$KUBECONFIG_FILE"
                      kubectl config use-context "${kubeContext}"
                      kubectl -n "${kubeNamespace}" get pods -o wide
                      kubectl -n "${kubeNamespace}" get svc
                      kubectl -n monitoring get pods,svc || true
                    """
                }
            }
        }
    } catch (Throwable t) {
        archiveAndRollbackKube(
            kubeconfigCredentialsId,
            kubeContext,
            kubeNamespace,
            dryRunOnly,
            rollbackOnFailure
        )
        throw t
    }
}

def archiveAndRollbackKube(String kubeconfigCredentialsId, String kubeContext, String kubeNamespace, boolean dryRunOnly, boolean rollbackOnFailure) {
    withCredentials([file(credentialsId: kubeconfigCredentialsId, variable: "KUBECONFIG_FILE")]) {
        sh """
          set +e
          export KUBECONFIG="\$KUBECONFIG_FILE"
          kubectl config use-context "${kubeContext}" || true
          kubectl -n "${kubeNamespace}" get events --sort-by=.metadata.creationTimestamp > kube-events.log 2>/dev/null || true
          kubectl -n "${kubeNamespace}" get pods -o wide > kube-pods.log 2>/dev/null || true
          exit 0
        """
    }
    archiveArtifacts allowEmptyArchive: true, artifacts: "kube-events.log,kube-pods.log"
    if (!dryRunOnly && rollbackOnFailure) {
        withCredentials([file(credentialsId: kubeconfigCredentialsId, variable: "KUBECONFIG_FILE")]) {
            sh """
              set +e
              export KUBECONFIG="\$KUBECONFIG_FILE"
              kubectl config use-context "${kubeContext}"
              deployments=\$(kubectl -n "${kubeNamespace}" get deploy -o jsonpath='{range .items[*]}{.metadata.name}{"\\n"}{end}')
              for d in \$deployments; do
                echo "Attempting rollback for deployment/\$d"
                if kubectl -n "${kubeNamespace}" rollout history "deployment/\$d" >/dev/null 2>&1; then
                  kubectl -n "${kubeNamespace}" rollout undo "deployment/\$d" || true
                else
                  echo "No rollout history for deployment/\$d; skipping rollback"
                fi
              done
            """
        }
    }
}

return this
