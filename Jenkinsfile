// Unified CI (build/test/push all services) + optional CD (kubectl apply from k8s/).
// Requires agent with Docker, Git, kubectl, python3. Credentials: GithubCredentials, DockerHubCrendentials, kubeconfig file, optional Sonar.

pipeline {
    agent none
    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: "30", artifactNumToKeepStr: "20"))
    }
    parameters {
        string(name: "REPO_URL", defaultValue: "https://github.com/RidhaFerchichi404/pidev_4SAE11.git", description: "Git repository URL")
        string(name: "BRANCH", defaultValue: "main", description: "Branch to build and deploy")
        string(name: "IMAGE_REPO", defaultValue: "docker.io/ridhaferchichi", description: "Registry/repo prefix for images and manifest rendering")
        string(name: "IMAGE_TAG", defaultValue: "", description: "Optional immutable tag; if empty, BUILD_NUMBER is used for build and deploy")
        booleanParam(name: "PUSH_IMAGE", defaultValue: true, description: "Push images to Docker Hub")
        booleanParam(name: "RUN_SONARQUBE", defaultValue: true, description: "Run SonarQube analysis per service")
        booleanParam(name: "DEPLOY_TO_K8S", defaultValue: true, description: "After successful CI, render manifests and deploy to Kubernetes")
        string(name: "GIT_CREDENTIALS_ID", defaultValue: "GithubCredentials", description: "Jenkins credentials ID used for Git checkout")
        string(name: "DOCKER_CREDENTIALS_ID", defaultValue: "DockerHubCrendentials", description: "Jenkins username/password credentials ID for Docker Hub")

        string(name: "KUBECONFIG_CREDENTIALS_ID", defaultValue: "kubeconfig", description: "Jenkins secret file credential ID for kubeconfig")
        string(name: "KUBE_CONTEXT", defaultValue: "kubernetes-admin@kubernetes", description: "Kubernetes context name from kubeconfig")
        string(name: "KUBE_NAMESPACE", defaultValue: "smart-freelance-dev", description: "Application namespace to deploy")
        string(name: "MANIFEST_PATH", defaultValue: "k8s", description: "Path to app manifests in repo")
        booleanParam(name: "DEPLOY_MONITORING", defaultValue: true, description: "Deploy Prometheus and Grafana stack")
        string(name: "MONITORING_MANIFEST_PATH", defaultValue: "k8s/monitoring", description: "Path to monitoring manifests")
        booleanParam(name: "REQUIRE_PROD_APPROVAL", defaultValue: true, description: "Ask for manual approval when ENVIRONMENT=prod")
        choice(name: "ENVIRONMENT", choices: ["dev", "staging", "prod"], description: "Deployment target environment")
        booleanParam(name: "ROLLBACK_ON_FAILURE", defaultValue: true, description: "Rollback Deployments when rollout verification fails")
        booleanParam(name: "DRY_RUN_ONLY", defaultValue: false, description: "Render/apply using server dry-run only (no live rollout checks)")
        string(name: "ROLLOUT_TIMEOUT_SECONDS", defaultValue: "600", description: "Timeout in seconds for each deployment rollout check")
        booleanParam(name: "DEPLOY_INGRESS", defaultValue: true, description: "Apply k8s/10-ingress.yaml (requires ingress-nginx controller on cluster)")
        string(name: "DEPLOY_UNITS", defaultValue: "", description: "Optional comma-separated deploy units (for example: eureka,config-server,user,api-gateway,frontend). Empty = deploy all.")
        string(name: "PUBLIC_API_GATEWAY_URL", defaultValue: "http://api.smartfreelance.example.com", description: "Browser-reachable API URL baked into Angular production build")
        string(name: "GITHUB_TOKEN_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret text credential ID to export GITHUB_TOKEN during secrets rendering")
        string(name: "MDP_FILE_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret file credential ID for mdp.local contents")
        string(name: "FIREBASE_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret file credential ID for firebase admin JSON")
        string(name: "PLANNING_CALENDAR_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret file credential ID for planning calendar service account JSON")
        string(name: "MEETING_CALENDAR_CREDENTIALS_ID", defaultValue: "", description: "Optional Jenkins secret file credential ID for meeting calendar service account JSON")
    }
    environment {
        EFFECTIVE_IMAGE_TAG = "${params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER}"
    }
    stages {
        stage("Checkout") {
            agent any
            steps {
                checkout([
                    $class: "GitSCM",
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[url: params.REPO_URL, credentialsId: (params.GIT_CREDENTIALS_ID?.trim() ?: "GithubCredentials")]]
                ])
                stash name: "repo-source", includes: "**/*", useDefaultExcludes: false
            }
        }
        stage("Keycloak server image") {
            agent any
            steps {
                script {
                    runInIsolatedWorkspace("keycloak-image") {
                        buildAndPushKeycloakServerImage()
                    }
                }
            }
        }
        stage("Infrastructure Foundation") {
            stages {
                stage("eureka") {
                    agent any
                    steps {
                        script {
                            runInIsolatedWorkspace("eureka") {
                                runMs("backEnd/Eureka", "eureka")
                            }
                        }
                    }
                }
                stage("config+keycloak") {
                    parallel {
                        stage("config-server") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("config-server") {
                                        runMs("backEnd/ConfigServer", "config-server")
                                    }
                                }
                            }
                        }
                        stage("keycloak-auth") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("keycloak-auth") {
                                        runMs("backEnd/KeyCloak", "keycloak-auth")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        stage("Core Services Parallel") {
            stages {
                stage("parallel-core") {
                    parallel {
                        stage("user") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("user") {
                                        runMs("backEnd/Microservices/user", "user")
                                    }
                                }
                            }
                        }
                        stage("project") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("project") {
                                        runMs("backEnd/Microservices/Project", "project")
                                    }
                                }
                            }
                        }
                        stage("notification") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("notification") {
                                        runMs("backEnd/Microservices/Notification", "notification")
                                    }
                                }
                            }
                        }
                        stage("contract") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("contract") {
                                        runMs("backEnd/Microservices/Contract", "contract")
                                    }
                                }
                            }
                        }
                        stage("portfolio") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("portfolio") {
                                        runMs("backEnd/Microservices/Portfolio", "portfolio")
                                    }
                                }
                            }
                        }
                        stage("chat") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("chat") {
                                        runMs("backEnd/Microservices/Chat", "chat")
                                    }
                                }
                            }
                        }
                        stage("meeting") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("meeting") {
                                        runMs("backEnd/Microservices/Meeting", "meeting")
                                    }
                                }
                            }
                        }
                        stage("freelancia-job") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("freelancia-job") {
                                        runMs("backEnd/Microservices/FreelanciaJob", "freelancia-job")
                                    }
                                }
                            }
                        }
                    }
                }
                stage("aimodel") {
                    agent any
                    steps {
                        script {
                            runInIsolatedWorkspace("aimodel") {
                                runMs("backEnd/Microservices/AImodel", "aimodel")
                            }
                        }
                    }
                }
            }
        }
        stage("Dependent Services") {
            stages {
                stage("planning") {
                    agent any
                    steps {
                        script {
                            runInIsolatedWorkspace("planning") {
                                runMs("backEnd/Microservices/planning", "planning")
                            }
                        }
                    }
                }
                stage("parallel-dependent") {
                    parallel {
                        stage("task") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("task") {
                                        // task depends on planning + aimodel; stage order keeps this safe.
                                        runMs("backEnd/Microservices/task", "task")
                                    }
                                }
                            }
                        }
                        stage("review") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("review") {
                                        runMs("backEnd/Microservices/review", "review")
                                    }
                                }
                            }
                        }
                        stage("offer") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("offer") {
                                        runMs("backEnd/Microservices/Offer", "offer")
                                    }
                                }
                            }
                        }
                        stage("gamification") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("gamification") {
                                        runMs("backEnd/Microservices/gamification", "gamification")
                                    }
                                }
                            }
                        }
                        stage("ticket-service") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("ticket-service") {
                                        runMs("backEnd/Microservices/ticket-service", "ticket-service")
                                    }
                                }
                            }
                        }
                        stage("subcontracting") {
                            agent any
                            steps {
                                script {
                                    runInIsolatedWorkspace("subcontracting") {
                                        runMs("backEnd/Microservices/Subcontracting", "subcontracting")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        stage("Gateway and Frontend Parallel") {
            parallel {
                stage("api-gateway") {
                    agent any
                    steps {
                        script {
                            runInIsolatedWorkspace("api-gateway") {
                                runMs("backEnd/apiGateway", "api-gateway")
                            }
                        }
                    }
                }
                stage("frontend") {
                    agent any
                    steps {
                        script {
                            runInIsolatedWorkspace("frontend") {
                                runMs("frontend/smart-freelance-app", "frontend", [
                                    dockerBuildArgs: "--build-arg API_GATEWAY_PUBLIC_URL=${(params.PUBLIC_API_GATEWAY_URL ?: 'http://api.smartfreelance.example.com').trim()}"
                                ])
                            }
                        }
                    }
                }
            }
        }
        stage("Deploy Kubernetes") {
            agent any
            when {
                expression { return params.DEPLOY_TO_K8S }
            }
            steps {
                script {
                    runInIsolatedWorkspace("deploy") {
                    if (!params.KUBE_CONTEXT?.trim()) {
                        error("KUBE_CONTEXT is required when DEPLOY_TO_K8S is enabled")
                    }
                    def kd = load("ci/pipelines/kubeDeployLib.groovy")
                    kd.runKubernetesDeploy([
                        kubeContext              : params.KUBE_CONTEXT.trim(),
                        kubeNamespace            : params.KUBE_NAMESPACE.trim(),
                        manifestPath             : params.MANIFEST_PATH.trim(),
                        imageRepo                : params.IMAGE_REPO.trim(),
                        imageTag                 : env.EFFECTIVE_IMAGE_TAG,
                        kubeconfigCredentialsId  : params.KUBECONFIG_CREDENTIALS_ID.trim(),
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
    }
    post {
        always {
            script {
                node {
                    cleanWs(deleteDirs: true, disableDeferredWipeout: true)
                }
            }
        }
    }
}

def microRunnerOnce() {
    return load("ci/pipelines/microservicePipeline.groovy")
}

def runInIsolatedWorkspace(String workspaceSuffix, Closure body) {
    ws("${env.WORKSPACE}@${workspaceSuffix}") {
        deleteDir()
        unstash "repo-source"
        body()
    }
}

def runMs(String servicePath, String imageName, Map opts = [:]) {
    def m = [
        servicePath        : servicePath,
        imageName          : imageName,
        skipCheckout       : true,
        skipCleanWs        : true,
        applyJobProperties : false,
        dockerCredentialsId: (params.DOCKER_CREDENTIALS_ID ?: "DockerHubCrendentials").trim()
    ]
    if (opts) {
        m.putAll(opts)
    }
    microRunnerOnce().runMicroservicePipeline(m)
}

/**
 * Same image as docker-compose keycloak service: keycloak-start/Dockerfile (realm import + conf).
 * Must run before deploy so k8s can pull YOUR_REPO/keycloak:<tag>.
 */
def buildAndPushKeycloakServerImage() {
    def dockerCredsId = (params.DOCKER_CREDENTIALS_ID ?: "DockerHubCrendentials").trim()
    def imageRepo = params.IMAGE_REPO
    def tag = (params.IMAGE_TAG?.trim()) ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER
    def dockerImage = "${imageRepo}/keycloak"
    def fullImage = "${dockerImage}:${tag}"
    sh """
      docker build -f keycloak-start/Dockerfile -t '${fullImage}' -t '${dockerImage}:latest' .
    """
    if (!params.PUSH_IMAGE) {
        echo "Skipping Keycloak image push (PUSH_IMAGE is false)."
        return
    }
    withCredentials([usernamePassword(credentialsId: dockerCredsId, usernameVariable: "DH_USER", passwordVariable: "DH_PASS")]) {
        sh """
          echo "\$DH_PASS" | docker login docker.io -u "\$DH_USER" --password-stdin
        """
        def pushTargets = [full: fullImage, base: dockerImage]
        script {
            def ir = (params.IMAGE_REPO ?: "").trim()
            def irLower = ir.toLowerCase()
            if (irLower.startsWith("docker.io/")) {
                def rest = ir.substring(ir.indexOf("/") + 1)
                def ns = rest.split("/")[0]?.trim()
                def u = env.DH_USER?.trim()
                if (ns && u && !ns.equalsIgnoreCase(u)) {
                    echo "IMAGE_REPO namespace '${ns}' does not match Docker Hub login '${u}'. Retagging keycloak image."
                    pushTargets.full = "docker.io/${u}/keycloak:${tag}"
                    pushTargets.base = "docker.io/${u}/keycloak"
                    sh """
                      docker tag '${fullImage}' '${pushTargets.full}'
                      docker tag '${dockerImage}:latest' '${pushTargets.base}:latest'
                    """
                }
            }
        }
        sh "docker push ${pushTargets.full}"
        def latestPushStatus = sh(script: """
          set +e
          docker push ${pushTargets.base}:latest
          status=\$?
          if [ "\$status" -ne 0 ]; then
            echo "Retrying latest push after re-login..."
            echo "\$DH_PASS" | docker login docker.io -u "\$DH_USER" --password-stdin
            docker push ${pushTargets.base}:latest
            status=\$?
          fi
          exit "\$status"
        """, returnStatus: true)
        if (latestPushStatus != 0) {
            unstable("Failed to push ${pushTargets.base}:latest; versioned image ${pushTargets.full} was pushed.")
        }
        sh "docker logout docker.io || docker logout || true"
    }
}
