pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: "30", artifactNumToKeepStr: "20"))
    }

    parameters {
        string(name: "REPO_URL", defaultValue: "https://github.com/RidhaFerchichi404/pidev_4SAE11.git", description: "Git repository URL containing Kubernetes manifests")
        string(name: "BRANCH", defaultValue: "main", description: "Branch to deploy from")
        string(name: "KUBECONFIG_CREDENTIALS_ID", defaultValue: "kubeconfig", description: "Jenkins secret file credential ID for kubeconfig")
        string(name: "KUBE_CONTEXT", defaultValue: "kubernetes-admin@kubernetes", description: "Kubernetes context name from kubeconfig (required)")
        string(name: "KUBE_NAMESPACE", defaultValue: "smart-freelance-dev", description: "Application namespace to deploy")
        string(name: "MANIFEST_PATH", defaultValue: "k8s", description: "Path to app manifests in repo")
        string(name: "IMAGE_REPO", defaultValue: "docker.io/ridhaferchichi", description: "Registry/repository prefix")
        string(name: "IMAGE_TAG", defaultValue: "", description: "Immutable image tag to deploy (required)")
        booleanParam(name: "DEPLOY_MONITORING", defaultValue: true, description: "Deploy Prometheus and Grafana stack")
        string(name: "MONITORING_MANIFEST_PATH", defaultValue: "k8s/monitoring", description: "Path to monitoring manifests")
        booleanParam(name: "REQUIRE_PROD_APPROVAL", defaultValue: true, description: "Ask for manual approval when ENVIRONMENT=prod")
        choice(name: "ENVIRONMENT", choices: ["dev", "staging", "prod"], description: "Deployment target environment")
        booleanParam(name: "ROLLBACK_ON_FAILURE", defaultValue: true, description: "Rollback Deployments when rollout verification fails")
        booleanParam(name: "DRY_RUN_ONLY", defaultValue: false, description: "Render/apply using server dry-run only")
    }

    environment {
        GITHUB_CREDS_ID = "GithubCredentials"
        RENDER_DIR = ".cd-rendered"
    }

    stages {
        stage("Checkout") {
            steps {
                checkout([
                    $class: "GitSCM",
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[url: params.REPO_URL, credentialsId: env.GITHUB_CREDS_ID]]
                ])
            }
        }

        stage("Validate Inputs") {
            steps {
                script {
                    if (!params.KUBE_CONTEXT?.trim()) {
                        error("KUBE_CONTEXT is required")
                    }
                    if (!params.IMAGE_TAG?.trim()) {
                        error("IMAGE_TAG is required for immutable releases")
                    }
                }
                sh """
                  set -e
                  command -v kubectl >/dev/null 2>&1 || { echo 'kubectl is not installed on this Jenkins agent'; exit 1; }
                  command -v python3 >/dev/null 2>&1 || { echo 'python3 is not installed on this Jenkins agent'; exit 1; }
                  test -d "${params.MANIFEST_PATH}" || { echo "Manifest path not found: ${params.MANIFEST_PATH}"; exit 1; }
                """
            }
        }

        stage("Production Approval") {
            when {
                expression { return params.ENVIRONMENT == "prod" && params.REQUIRE_PROD_APPROVAL }
            }
            steps {
                timeout(time: 20, unit: "MINUTES") {
                    input message: "Approve production deployment for tag ${params.IMAGE_TAG}?"
                }
            }
        }

        stage("Cluster Connectivity") {
            steps {
                withCredentials([file(credentialsId: params.KUBECONFIG_CREDENTIALS_ID, variable: "KUBECONFIG_FILE")]) {
                    sh """
                      set -e
                      export KUBECONFIG="$KUBECONFIG_FILE"
                      kubectl config use-context "${params.KUBE_CONTEXT}"
                      kubectl cluster-info
                      kubectl get nodes -o wide
                    """
                }
            }
        }

        stage("Render App Manifests") {
            steps {
                sh """
                  set -e
                  rm -rf "${env.RENDER_DIR}"
                  mkdir -p "${env.RENDER_DIR}/app"
                  cp -R "${params.MANIFEST_PATH}/." "${env.RENDER_DIR}/app/"
                  rm -rf "${env.RENDER_DIR}/app/monitoring"
                  rm -f "${env.RENDER_DIR}/app/10-ingress.yaml"
                  python3 - <<'PY'
import pathlib
import re

repo = "${params.IMAGE_REPO}".strip().rstrip("/")
tag = "${params.IMAGE_TAG}".strip()
namespace = "${params.KUBE_NAMESPACE}".strip()
target = pathlib.Path("${env.RENDER_DIR}") / "app"

pattern = re.compile(r"YOUR_DOCKERHUB_USERNAME/([A-Za-z0-9._-]+):latest")
namespace_pattern = re.compile(r"^(\s*namespace:\s*)smart-freelance\s*$", re.MULTILINE)

for file in target.rglob("*.y*ml"):
    data = file.read_text(encoding="utf-8")
    updated = pattern.sub(lambda m: f"{repo}/{m.group(1)}:{tag}", data)
    updated = namespace_pattern.sub(lambda m: f"{m.group(1)}{namespace}", updated)
    if file.name == "00-namespace.yaml":
        updated = re.sub(r"^(\s*name:\s*)smart-freelance\s*$", lambda m: f"{m.group(1)}{namespace}", updated, flags=re.MULTILINE)
    if updated != data:
        file.write_text(updated, encoding="utf-8")
PY
                  if [ -f "${env.RENDER_DIR}/app/02-secrets.yaml" ] && rg -n ':\s*""\s*(#.*)?$' "${env.RENDER_DIR}/app/02-secrets.yaml" >/dev/null; then
                    echo "WARNING: Incomplete values detected in 02-secrets.yaml; skipping this manifest for this deploy."
                    mv "${env.RENDER_DIR}/app/02-secrets.yaml" "${env.RENDER_DIR}/app/02-secrets.yaml.skipped"
                  fi
                """
            }
        }

        stage("Render Monitoring Manifests") {
            when {
                expression { return params.DEPLOY_MONITORING }
            }
            steps {
                sh """
                  set -e
                  rm -rf "${env.RENDER_DIR}/monitoring"
                  mkdir -p "${env.RENDER_DIR}/monitoring"
                  cp -R "${params.MONITORING_MANIFEST_PATH}/." "${env.RENDER_DIR}/monitoring/"
                  python3 - <<'PY'
import pathlib
import re

app_namespace = "${params.KUBE_NAMESPACE}".strip()
target = pathlib.Path("${env.RENDER_DIR}") / "monitoring"

for file in target.rglob("*.y*ml"):
    data = file.read_text(encoding="utf-8")
    updated = re.sub(r"smart-freelance-dev", app_namespace, data)
    if updated != data:
        file.write_text(updated, encoding="utf-8")
PY
                """
            }
        }

        stage("Deploy Application") {
            steps {
                withCredentials([file(credentialsId: params.KUBECONFIG_CREDENTIALS_ID, variable: "KUBECONFIG_FILE")]) {
                    sh """
                      set -e
                      export KUBECONFIG="$KUBECONFIG_FILE"
                      kubectl config use-context "${params.KUBE_CONTEXT}"
                      kubectl create namespace "${params.KUBE_NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -

                      if [ "${params.DRY_RUN_ONLY}" = "true" ]; then
                        kubectl apply --server-side --dry-run=server -f "${env.RENDER_DIR}/app"
                      else
                        kubectl apply -f "${env.RENDER_DIR}/app"
                      fi
                    """
                }
            }
        }

        stage("Deploy Monitoring Stack") {
            when {
                expression { return params.DEPLOY_MONITORING }
            }
            steps {
                withCredentials([file(credentialsId: params.KUBECONFIG_CREDENTIALS_ID, variable: "KUBECONFIG_FILE")]) {
                    sh """
                      set -e
                      export KUBECONFIG="$KUBECONFIG_FILE"
                      kubectl config use-context "${params.KUBE_CONTEXT}"
                      test -d "${params.MONITORING_MANIFEST_PATH}" || { echo "Monitoring manifest path not found: ${params.MONITORING_MANIFEST_PATH}"; exit 1; }
                      test -d "${env.RENDER_DIR}/monitoring" || { echo "Rendered monitoring path not found: ${env.RENDER_DIR}/monitoring"; exit 1; }

                      if [ "${params.DRY_RUN_ONLY}" = "true" ]; then
                        kubectl apply --server-side --dry-run=server -f "${env.RENDER_DIR}/monitoring"
                      else
                        kubectl apply -f "${env.RENDER_DIR}/monitoring"
                      fi
                    """
                }
            }
        }

        stage("Rollout Verification") {
            when {
                expression { return !params.DRY_RUN_ONLY }
            }
            steps {
                withCredentials([file(credentialsId: params.KUBECONFIG_CREDENTIALS_ID, variable: "KUBECONFIG_FILE")]) {
                    sh """
                      set -e
                      export KUBECONFIG="$KUBECONFIG_FILE"
                      kubectl config use-context "${params.KUBE_CONTEXT}"

                      deployments=$(kubectl -n "${params.KUBE_NAMESPACE}" get deploy -o jsonpath='{range .items[*]}{.metadata.name}{"\\n"}{end}')
                      if [ -z "$deployments" ]; then
                        echo "No deployments found in namespace ${params.KUBE_NAMESPACE}"
                      else
                        for d in $deployments; do
                          echo "Waiting rollout for deployment/$d"
                          kubectl -n "${params.KUBE_NAMESPACE}" rollout status "deployment/$d" --timeout=180s
                        done
                      fi
                    """
                }
            }
        }

        stage("Post Deploy Smoke Checks") {
            when {
                expression { return !params.DRY_RUN_ONLY }
            }
            steps {
                withCredentials([file(credentialsId: params.KUBECONFIG_CREDENTIALS_ID, variable: "KUBECONFIG_FILE")]) {
                    sh """
                      set -e
                      export KUBECONFIG="$KUBECONFIG_FILE"
                      kubectl config use-context "${params.KUBE_CONTEXT}"
                      kubectl -n "${params.KUBE_NAMESPACE}" get pods -o wide
                      kubectl -n "${params.KUBE_NAMESPACE}" get svc
                      kubectl -n monitoring get pods,svc || true
                    """
                }
            }
        }
    }

    post {
        unsuccessful {
            withCredentials([file(credentialsId: params.KUBECONFIG_CREDENTIALS_ID, variable: "KUBECONFIG_FILE")]) {
                sh """
                  set +e
                  export KUBECONFIG="$KUBECONFIG_FILE"
                  kubectl config use-context "${params.KUBE_CONTEXT}"
                  kubectl -n "${params.KUBE_NAMESPACE}" get events --sort-by=.metadata.creationTimestamp > kube-events.log 2>/dev/null
                  kubectl -n "${params.KUBE_NAMESPACE}" get pods -o wide > kube-pods.log 2>/dev/null
                """
            }
            archiveArtifacts allowEmptyArchive: true, artifacts: "kube-events.log,kube-pods.log"
            script {
                if (!params.DRY_RUN_ONLY && params.ROLLBACK_ON_FAILURE) {
                    withCredentials([file(credentialsId: params.KUBECONFIG_CREDENTIALS_ID, variable: "KUBECONFIG_FILE")]) {
                        sh """
                          set +e
                          export KUBECONFIG="$KUBECONFIG_FILE"
                          kubectl config use-context "${params.KUBE_CONTEXT}"
                          deployments=$(kubectl -n "${params.KUBE_NAMESPACE}" get deploy -o jsonpath='{range .items[*]}{.metadata.name}{"\\n"}{end}')
                          for d in $deployments; do
                            echo "Attempting rollback for deployment/$d"
                            kubectl -n "${params.KUBE_NAMESPACE}" rollout undo "deployment/$d" || true
                          done
                        """
                    }
                }
            }
        }
        always {
            cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        }
    }
}
