// CD-only Jenkins Pipeline: checkout repo then delegate to kubeDeployLib (same deploy logic as unified root Jenkinsfile).

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
        string(name: "GIT_CREDENTIALS_ID", defaultValue: "GithubCredentials", description: "Jenkins credentials ID used for Git checkout")
        string(name: "KUBE_CONTEXT", defaultValue: "", description: "Optional Kubernetes context name (empty to use current/in-cluster context)")
        string(name: "KUBE_NAMESPACE", defaultValue: "freelance", description: "Application namespace to deploy")
        string(name: "MANIFEST_PATH", defaultValue: "k8s", description: "Path to app manifests in repo")
        string(name: "IMAGE_REPO", defaultValue: "ridhaferchichi", description: "Registry/repository prefix (example: ridhaferchichi)")
        string(name: "IMAGE_TAG", defaultValue: "", description: "Immutable image tag; if empty, BUILD_NUMBER is used")
        booleanParam(name: "DEPLOY_MONITORING", defaultValue: true, description: "Deploy Prometheus and Grafana stack")
        string(name: "MONITORING_MANIFEST_PATH", defaultValue: "k8s/monitoring", description: "Path to monitoring manifests")
        booleanParam(name: "REQUIRE_PROD_APPROVAL", defaultValue: true, description: "Ask for manual approval when ENVIRONMENT=prod")
        choice(name: "ENVIRONMENT", choices: ["dev", "staging", "prod"], description: "Deployment target environment")
        booleanParam(name: "ROLLBACK_ON_FAILURE", defaultValue: true, description: "Rollback Deployments when rollout verification fails")
        booleanParam(name: "DRY_RUN_ONLY", defaultValue: false, description: "Render/apply using server dry-run only")
        string(name: "ROLLOUT_TIMEOUT_SECONDS", defaultValue: "600", description: "Timeout in seconds for each deployment rollout check")
        booleanParam(name: "DEPLOY_INGRESS", defaultValue: true, description: "Apply k8s Ingress (requires ingress-nginx)")
        string(name: "DEPLOY_UNITS", defaultValue: "", description: "Optional comma-separated deploy units (for example: eureka,config-server,user,api-gateway,frontend). Empty = deploy all.")
        string(name: "GITHUB_TOKEN_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret text credential ID for Planning GitHub PAT (exports GITHUB_TOKEN)")
        string(name: "MDP_FILE_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret file credential ID for mdp.local contents")
        string(name: "FIREBASE_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret file credential ID for firebase admin JSON")
        string(name: "PLANNING_CALENDAR_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret file credential ID for planning calendar service account JSON")
        string(name: "MEETING_CALENDAR_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret file credential ID for meeting calendar service account JSON")
    }

    environment {
        GITHUB_CREDS_ID = "GithubCredentials"
        EFFECTIVE_IMAGE_TAG = "${params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER}"
    }

    stages {
        stage("Checkout") {
            steps {
                checkout([
                    $class: "GitSCM",
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[url: params.REPO_URL, credentialsId: (params.GIT_CREDENTIALS_ID?.trim() ?: env.GITHUB_CREDS_ID)]]
                ])
            }
        }

        stage("Validate Inputs") {
            steps {
                sh """
                  set -e
                  command -v kubectl >/dev/null 2>&1 || { echo 'kubectl is not installed on this Jenkins agent'; exit 1; }
                  command -v python3 >/dev/null 2>&1 || { echo 'python3 is not installed on this Jenkins agent'; exit 1; }
                  test -d "${params.MANIFEST_PATH}" || { echo "Manifest path not found: ${params.MANIFEST_PATH}"; exit 1; }
                """
            }
        }

        stage("Deploy Kubernetes") {
            steps {
                script {
                    def kd = load("ci/pipelines/kubeDeployLib.groovy")
                    kd.runKubernetesDeploy([
                        kubeContext              : params.KUBE_CONTEXT?.trim(),
                        kubeNamespace            : params.KUBE_NAMESPACE.trim(),
                        manifestPath             : params.MANIFEST_PATH.trim(),
                        imageRepo                : params.IMAGE_REPO.trim(),
                        imageTag                 : env.EFFECTIVE_IMAGE_TAG,
                        deployMonitoring         : params.DEPLOY_MONITORING,
                        monitoringManifestPath   : params.MONITORING_MANIFEST_PATH.trim(),
                        deployEnvironment        : params.ENVIRONMENT,
                        requireProdApproval      : params.REQUIRE_PROD_APPROVAL,
                        dryRunOnly               : params.DRY_RUN_ONLY,
                        rollbackOnFailure        : params.ROLLBACK_ON_FAILURE,
                        rolloutTimeoutSeconds    : (params.ROLLOUT_TIMEOUT_SECONDS ?: "600").toString().trim(),
                        deployIngress            : params.DEPLOY_INGRESS,
                        deployUnits              : params.DEPLOY_UNITS?.trim(),
                        githubTokenCredentialsId : params.GITHUB_TOKEN_CREDENTIALS_ID?.trim(),
                        mdpFileCredentialsId     : params.MDP_FILE_CREDENTIALS_ID?.trim(),
                        firebaseCredentialsId    : params.FIREBASE_CREDENTIALS_ID?.trim(),
                        planningCalendarCredentialsId: params.PLANNING_CALENDAR_CREDENTIALS_ID?.trim(),
                        meetingCalendarCredentialsId : params.MEETING_CALENDAR_CREDENTIALS_ID?.trim()
                    ])
                }
            }
        }
    }

    post {
        always {
            cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        }
    }
}
