/**
 * Kubernetes deploy steps (no checkout). Caller must have the repo workspace at the job root.
 * cfg keys: kubeContext, kubeNamespace, manifestPath, imageRepo, imageTag,
 *   deployMonitoring, monitoringManifestPath, deployEnvironment, requireProdApproval, dryRunOnly,
 *   rollbackOnFailure, rolloutTimeoutSeconds, renderDir (optional), deployIngress (optional, default true),
 *   githubTokenCredentialsId (optional Jenkins secret text credential ID),
 *   mdpFileCredentialsId (optional Jenkins secret file; mounted as mdp.local source),
 *   firebaseCredentialsId (optional Jenkins secret file),
 *   planningCalendarCredentialsId (optional Jenkins secret file),
 *   meetingCalendarCredentialsId (optional Jenkins secret file)
 */
def runKubernetesDeploy(Map cfg) {
    def renderDir = (cfg.renderDir ?: ".cd-rendered").toString()
    def kubeContext = cfg.kubeContext?.trim()
    def kubeNamespace = cfg.kubeNamespace?.trim() ?: error("kubeNamespace is required")
    def manifestPath = cfg.manifestPath?.trim() ?: "k8s"
    def imageRepo = cfg.imageRepo?.trim() ?: error("imageRepo is required")
    def imageTag = cfg.imageTag?.toString()?.trim() ?: error("imageTag is required")
    def deployIngress = cfg.deployIngress != false
    def githubTokenCredentialsId = cfg.githubTokenCredentialsId?.toString()?.trim()
    def mdpFileCredentialsId = cfg.mdpFileCredentialsId?.toString()?.trim()
    def firebaseCredentialsId = cfg.firebaseCredentialsId?.toString()?.trim()
    def planningCalendarCredentialsId = cfg.planningCalendarCredentialsId?.toString()?.trim()
    def meetingCalendarCredentialsId = cfg.meetingCalendarCredentialsId?.toString()?.trim()
    def deployMonitoring = cfg.deployMonitoring != false
    def monitoringManifestPath = cfg.monitoringManifestPath?.trim() ?: "k8s/monitoring"
    def deployEnvironment = (cfg.deployEnvironment ?: "dev").toString()
    def requireProdApproval = cfg.requireProdApproval != false
    def dryRunOnly = cfg.dryRunOnly == true
    def rollbackOnFailure = cfg.rollbackOnFailure != false
    def rolloutTimeoutSeconds = (cfg.rolloutTimeoutSeconds ?: "600").toString()
    def deployUnitsRaw = cfg.deployUnits?.toString()
    def unitRenderDir = "${renderDir}/app-units"

    def dryShell = dryRunOnly ? "true" : "false"
    def withClusterAccess = { boolean strictContext = true, Closure body ->
        if (kubeContext) {
            def suffix = strictContext ? "" : " || true"
            sh "kubectl config use-context '${kubeContext}'${suffix}"
        }
        body()
    }

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
            withClusterAccess(true) {
                sh """
                  set -e
                  kubectl cluster-info
                  kubectl get nodes -o wide
                """
            }
        }

        stage("Render App Manifests") {
            def renderEnv = [
                "KD_IMAGE_REPO=${imageRepo}",
                "KD_IMAGE_TAG=${imageTag}",
                "KD_KUBE_NAMESPACE=${kubeNamespace}",
                "KD_RENDER_DIR=${renderDir}",
                "KD_DEPLOY_INGRESS=${deployIngress ? 'true' : 'false'}",
                "KD_MDP_FILE=mdp.local"
            ]
            def renderScript = """
                  set -e
                  rm -rf "${renderDir}"
                  mkdir -p "${renderDir}/app"
                  cp -R "${manifestPath}/." "${renderDir}/app/"
                  rm -rf "${renderDir}/app/monitoring"
                  if [ "\$KD_DEPLOY_INGRESS" != "true" ]; then
                    rm -f "${renderDir}/app/10-ingress.yaml"
                  fi
                  python3 scripts/render-k8s-secrets.py \
                    --repo-root . \
                    --mdp-file "\$KD_MDP_FILE" \
                    --output "${renderDir}/app/02-secrets.generated.yaml" \
                    --namespace "${kubeNamespace}"
                  rm -f "${renderDir}/app/02-secrets.yaml"
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
                  if [ -f "${renderDir}/app/02-secrets.generated.yaml" ] && grep -nE ':[[:space:]]*""[[:space:]]*(#.*)?' "${renderDir}/app/02-secrets.generated.yaml" >/dev/null; then
                    echo "WARNING: Incomplete values detected in generated secrets; deployment will continue with empty entries where applicable."
                  fi
                """
            def credentialBindings = []
            if (githubTokenCredentialsId) {
                credentialBindings << string(credentialsId: githubTokenCredentialsId, variable: "GITHUB_TOKEN")
            }
            if (mdpFileCredentialsId) {
                credentialBindings << file(credentialsId: mdpFileCredentialsId, variable: "KD_MDP_FILE")
            }
            if (firebaseCredentialsId) {
                credentialBindings << file(credentialsId: firebaseCredentialsId, variable: "FIREBASE_CREDENTIALS_PATH")
            }
            if (planningCalendarCredentialsId) {
                credentialBindings << file(credentialsId: planningCalendarCredentialsId, variable: "PLANNING_CALENDAR_CREDENTIALS_PATH")
            }
            if (meetingCalendarCredentialsId) {
                credentialBindings << file(credentialsId: meetingCalendarCredentialsId, variable: "MEETING_CALENDAR_CREDENTIALS_PATH")
            }

            if (credentialBindings) {
                withCredentials(credentialBindings) {
                    withEnv(renderEnv) {
                        sh renderScript
                    }
                }
            } else {
                withEnv(renderEnv) {
                    sh renderScript
                }
            }
        }

        stage("Prepare Deployment Units") {
            withEnv([
                "KD_RENDER_DIR=${renderDir}",
                "KD_UNIT_RENDER_DIR=${unitRenderDir}"
            ]) {
                sh '''
                  set -e
                  mkdir -p "$KD_UNIT_RENDER_DIR"
                  python3 - <<'PY'
import os
import pathlib
import re

render_dir = pathlib.Path(os.environ["KD_RENDER_DIR"]).resolve()
app_dir = render_dir / "app"
unit_dir = pathlib.Path(os.environ["KD_UNIT_RENDER_DIR"]).resolve()
unit_dir.mkdir(parents=True, exist_ok=True)

units = {
    "foundation": [],
    "shared": [],
    "keycloak": [],
    "eureka": [],
    "config-server": [],
    "keycloak-auth": [],
    "user": [],
    "project": [],
    "offer": [],
    "contract": [],
    "portfolio": [],
    "review": [],
    "planning": [],
    "task": [],
    "notification": [],
    "gamification": [],
    "chat": [],
    "meeting": [],
    "freelancia-job": [],
    "aimodel": [],
    "api-gateway": [],
    "frontend": [],
    "ingress": [],
    "phpmyadmin": [],
    "ollama": [],
}

file_unit_map = {
    "00-namespace.yaml": "foundation",
    "01-configmap.yaml": "foundation",
    "02-secrets.generated.yaml": "foundation",
    "03-mysql.yaml": "foundation",
    "13-user-storage.yaml": "foundation",
    "04-keycloak.yaml": "keycloak",
    "05-eureka.yaml": "eureka",
    "06-config-server.yaml": "config-server",
    "07-api-gateway.yaml": "api-gateway",
    "09-frontend.yaml": "frontend",
    "10-ingress.yaml": "ingress",
    "11-phpmyadmin.yaml": "phpmyadmin",
    "12-ollama.yaml": "ollama",
}

ms_name_to_unit = {
    "keycloak-auth": "keycloak-auth",
    "user": "user",
    "user-uploads-pvc": "user",
    "project": "project",
    "offer": "offer",
    "contract": "contract",
    "portfolio": "portfolio",
    "review": "review",
    "planning": "planning",
    "task": "task",
    "notification": "notification",
    "gamification": "gamification",
    "chat": "chat",
    "meeting": "meeting",
    "freelancia-job": "freelancia-job",
    "aimodel": "aimodel",
}

name_re = re.compile(r"^\\s*name:\\s*([^\\s#]+)\\s*$", re.MULTILINE)
kind_re = re.compile(r"^\\s*kind:\\s*([^\\s#]+)\\s*$", re.MULTILINE)

for file_path in sorted(app_dir.glob("*.y*ml")):
    base = file_path.name
    content = file_path.read_text(encoding="utf-8")
    if not content.strip():
        continue
    mapped = file_unit_map.get(base)
    if mapped:
        units[mapped].append(content.rstrip() + "\\n")
        continue
    if base != "08-microservices.yaml":
        units["shared"].append(content.rstrip() + "\\n")
        continue

    docs = re.split(r"(?m)^---\\s*$", content)
    for doc in docs:
        if not doc.strip():
            continue
        kind_match = kind_re.search(doc)
        name_match = name_re.search(doc)
        name = name_match.group(1).strip() if name_match else ""
        unit_name = ms_name_to_unit.get(name, "shared")
        normalized_doc = doc.strip() + "\\n"
        if kind_match:
            kind = kind_match.group(1).strip()
            # Global safety rule: PVCs must exist before workloads that mount them.
            if kind == "PersistentVolumeClaim":
                units["foundation"].append(normalized_doc)
            elif kind in ("Service", "Deployment"):
                units[unit_name].append(normalized_doc)
            else:
                units["shared"].append(normalized_doc)
        else:
            units["shared"].append(normalized_doc)

for unit_name, docs in units.items():
    if not docs:
        continue
    out_file = unit_dir / f"{unit_name}.yaml"
    out_file.write_text("---\\n".join(docs).strip() + "\\n", encoding="utf-8")
PY
                '''
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

        def unitDefs = [
            [name: "foundation", deployments: []],
            [name: "shared", deployments: []],
            [name: "keycloak", deployments: ["keycloak"]],
            [name: "eureka", deployments: ["eureka"]],
            [name: "config-server", deployments: ["config-server"]],
            [name: "keycloak-auth", deployments: ["keycloak-auth"]],
            [name: "user", deployments: ["user"]],
            [name: "project", deployments: ["project"]],
            [name: "offer", deployments: ["offer"]],
            [name: "contract", deployments: ["contract"]],
            [name: "portfolio", deployments: ["portfolio"]],
            [name: "review", deployments: ["review"]],
            [name: "planning", deployments: ["planning"]],
            [name: "task", deployments: ["task"]],
            [name: "notification", deployments: ["notification"]],
            [name: "gamification", deployments: ["gamification"]],
            [name: "chat", deployments: ["chat"]],
            [name: "meeting", deployments: ["meeting"]],
            [name: "freelancia-job", deployments: ["freelancia-job"]],
            [name: "aimodel", deployments: ["aimodel"]],
            [name: "api-gateway", deployments: ["api-gateway"]],
            [name: "frontend", deployments: ["frontend"]],
            [name: "ingress", deployments: []],
            [name: "phpmyadmin", deployments: ["phpmyadmin"]],
            [name: "ollama", deployments: ["ollama"]]
        ]
        def allowedUnits = unitDefs.collect { it.name } as Set
        def selectedUnits = selectDeployUnits(deployUnitsRaw, allowedUnits)

        def shouldDeployUnit = { String unitName ->
            if (selectedUnits.contains("all")) {
                return true
            }
            if (unitName == "foundation") {
                return true
            }
            if (unitName == "ingress" && !deployIngress) {
                return false
            }
            return selectedUnits.contains(unitName)
        }

        def deployUnit = { Map unit ->
            def unitName = unit.name
            def unitFile = "${unitRenderDir}/${unitName}.yaml"
            stage("Deploy ${unitName}") {
                if (!fileExists(unitFile)) {
                    echo "Skipping ${unitName}: rendered manifest file not found (${unitFile})."
                    return
                }
                withClusterAccess(true) {
                    sh """
                      set -e
                      kubectl create namespace "${kubeNamespace}" --dry-run=client -o yaml | kubectl apply -f -

                      if [ "${dryShell}" = "true" ]; then
                        kubectl apply --server-side --dry-run=server -f "${unitFile}"
                      else
                        kubectl apply -f "${unitFile}"
                      fi
                    """
                    if (!dryRunOnly && unit.deployments) {
                        unit.deployments.each { dep ->
                            sh """
                              set -e
                              if kubectl -n "${kubeNamespace}" get deploy "${dep}" >/dev/null 2>&1; then
                                echo "Waiting rollout for deployment/${dep}"
                                kubectl -n "${kubeNamespace}" rollout status "deployment/${dep}" --timeout="${rolloutTimeoutSeconds}s"
                              else
                                echo "Deployment ${dep} not found in namespace ${kubeNamespace}; skipping rollout wait."
                              fi
                            """
                        }
                    }
                }
            }
        }

        stage("Deploy Application Units") {
            def unitByName = unitDefs.collectEntries { [(it.name): it] }
            def bootstrapOrder = ["foundation", "shared", "keycloak", "eureka", "config-server", "keycloak-auth"]
            def coreParallelUnits = ["user", "project", "offer", "contract", "portfolio", "review", "planning", "task", "notification", "gamification", "chat", "meeting", "freelancia-job", "aimodel"]
            def tailSequential = ["api-gateway", "frontend", "ingress"]
            def optionalParallelUnits = ["phpmyadmin", "ollama"]

            bootstrapOrder.each { name ->
                if (shouldDeployUnit(name)) {
                    deployUnit(unitByName[name])
                }
            }

            def coreBranches = [:]
            coreParallelUnits.each { name ->
                if (shouldDeployUnit(name)) {
                    coreBranches[name] = { deployUnit(unitByName[name]) }
                }
            }
            if (coreBranches) {
                parallel coreBranches
            }

            tailSequential.each { name ->
                if (shouldDeployUnit(name)) {
                    deployUnit(unitByName[name])
                }
            }

            def optionalBranches = [:]
            optionalParallelUnits.each { name ->
                if (shouldDeployUnit(name)) {
                    optionalBranches[name] = { deployUnit(unitByName[name]) }
                }
            }
            if (optionalBranches) {
                parallel optionalBranches
            }
        }

        if (deployMonitoring) {
            stage("Deploy Monitoring Stack") {
                withClusterAccess(true) {
                    sh """
                      set -e
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
            stage("Post Deploy Smoke Checks") {
                withClusterAccess(true) {
                    sh """
                      set -e
                      kubectl -n "${kubeNamespace}" get pods -o wide
                      kubectl -n "${kubeNamespace}" get svc
                      kubectl -n monitoring get pods,svc || true
                    """
                }
            }
        }
    } catch (Throwable t) {
        archiveAndRollbackKube(
            kubeContext,
            kubeNamespace,
            dryRunOnly,
            rollbackOnFailure
        )
        throw t
    }
}

def archiveAndRollbackKube(String kubeContext, String kubeNamespace, boolean dryRunOnly, boolean rollbackOnFailure) {
    sh """
      set +e
      ${kubeContext ? "kubectl config use-context \"${kubeContext}\" || true" : "true"}
      kubectl -n "${kubeNamespace}" get events --sort-by=.metadata.creationTimestamp > kube-events.log 2>/dev/null || true
      kubectl -n "${kubeNamespace}" get pods -o wide > kube-pods.log 2>/dev/null || true
      exit 0
    """
    archiveArtifacts allowEmptyArchive: true, artifacts: "kube-events.log,kube-pods.log"
    if (!dryRunOnly && rollbackOnFailure) {
        def rollbackScript = """
          set +e
          ${kubeContext ? "kubectl config use-context \"${kubeContext}\" || true" : "true"}
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
        sh rollbackScript
    }
}

def selectDeployUnits(String rawUnits, Set allowedUnits) {
    if (!rawUnits?.trim()) {
        return ["all"] as Set
    }
    def parsed = rawUnits
        .split(",")
        .collect { it?.trim()?.toLowerCase() }
        .findAll { it }
        .toSet()
    if (!parsed) {
        return ["all"] as Set
    }
    if (parsed.contains("all")) {
        return ["all"] as Set
    }
    def unknown = parsed.findAll { !allowedUnits.contains(it) }
    if (unknown) {
        error("Unknown DEPLOY_UNITS values: ${unknown.join(', ')}. Allowed: all, ${allowedUnits.sort().join(', ')}")
    }
    return parsed
}

return this
