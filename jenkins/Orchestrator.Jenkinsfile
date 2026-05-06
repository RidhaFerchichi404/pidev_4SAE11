#!groovy
// Triggers per-service jobs under services/. Params mirror shared CI inputs; each child keeps its own SERVICE_PATH default.
node {
    def defaultOrder = [
        "keycloak-server",
        "eureka",
        "config-server",
        "keycloak-auth",
        "user",
        "project",
        "notification",
        "contract",
        "portfolio",
        "chat",
        "meeting",
        "freelancia-job",
        "aimodel",
        "aimodel-node",
        "planning",
        "task",
        "review",
        "offer",
        "gamification",
        "ticket-service",
        "subcontracting",
        "api-gateway",
        "frontend"
    ]

    def rawIds = params.SERVICE_IDS?.trim()
    def ids = rawIds ? rawIds.split(",").collect { it.trim() }.findAll { it } : defaultOrder
    def parallelRun = params.PARALLEL?.toString()?.equalsIgnoreCase("true")

    def branchVal = (params.BRANCH ?: "main").toString()
    def repoVal = (params.REPO_URL ?: "https://github.com/RidhaFerchichi404/pidev_4SAE11.git").toString()
    def imageRepoVal = (params.IMAGE_REPO ?: "docker.io/ridhaferchichi").toString()
    def imageTagVal = (params.IMAGE_TAG ?: "").toString()
    def gitCreds = (params.GIT_CREDENTIALS_ID ?: "GithubCredentials").toString()
    def dockerCreds = (params.DOCKER_CREDENTIALS_ID ?: "DockerHubCrendentials").toString()
    def dockerCleanupAfterBuild = params.DOCKER_CLEANUP_AFTER_BUILD != false
    def dockerImagePrune = params.DOCKER_IMAGE_PRUNE != false
    def dockerBuilderPrune = params.DOCKER_BUILDER_PRUNE == true
    def dockerBuilderKeepStorage = (params.DOCKER_BUILDER_KEEP_STORAGE ?: "8GB").toString()
    def pushImg = params.PUSH_IMAGE != false
    def runSonar = params.RUN_SONARQUBE != false
    def deployK8s = params.DEPLOY_TO_K8S == true

    def runOne = { String sid ->
        build job: "services/${sid}", wait: true, propagate: true, parameters: [
            string(name: "REPO_URL", value: repoVal),
            string(name: "BRANCH", value: branchVal),
            string(name: "IMAGE_REPO", value: imageRepoVal),
            string(name: "IMAGE_TAG", value: imageTagVal),
            booleanParam(name: "PUSH_IMAGE", value: pushImg),
            booleanParam(name: "RUN_SONARQUBE", value: runSonar),
            string(name: "GIT_CREDENTIALS_ID", value: gitCreds),
            string(name: "DOCKER_CREDENTIALS_ID", value: dockerCreds),
            booleanParam(name: "DOCKER_CLEANUP_AFTER_BUILD", value: dockerCleanupAfterBuild),
            booleanParam(name: "DOCKER_IMAGE_PRUNE", value: dockerImagePrune),
            booleanParam(name: "DOCKER_BUILDER_PRUNE", value: dockerBuilderPrune),
            string(name: "DOCKER_BUILDER_KEEP_STORAGE", value: dockerBuilderKeepStorage),
            booleanParam(name: "DEPLOY_TO_K8S", value: deployK8s),
            string(name: "KUBE_CONTEXT", value: (params.KUBE_CONTEXT ?: "").toString()),
            string(name: "KUBE_NAMESPACE", value: (params.KUBE_NAMESPACE ?: "freelance").toString()),
            string(name: "MANIFEST_PATH", value: (params.MANIFEST_PATH ?: "k8s").toString()),
            booleanParam(name: "DEPLOY_MONITORING", value: params.DEPLOY_MONITORING != false),
            string(name: "MONITORING_MANIFEST_PATH", value: (params.MONITORING_MANIFEST_PATH ?: "k8s/monitoring").toString()),
            string(name: "ENVIRONMENT", value: (params.ENVIRONMENT ?: "dev").toString()),
            booleanParam(name: "REQUIRE_PROD_APPROVAL", value: params.REQUIRE_PROD_APPROVAL != false),
            booleanParam(name: "ROLLBACK_ON_FAILURE", value: params.ROLLBACK_ON_FAILURE != false),
            booleanParam(name: "DRY_RUN_ONLY", value: params.DRY_RUN_ONLY == true),
            string(name: "ROLLOUT_TIMEOUT_SECONDS", value: (params.ROLLOUT_TIMEOUT_SECONDS ?: "600").toString()),
            booleanParam(name: "DEPLOY_INGRESS", value: params.DEPLOY_INGRESS != false),
            string(name: "PUBLIC_API_GATEWAY_URL", value: (params.PUBLIC_API_GATEWAY_URL ?: "http://api.smartfreelance.example.com").toString()),
            string(name: "GITHUB_TOKEN_CREDENTIALS_ID", value: (params.GITHUB_TOKEN_CREDENTIALS_ID ?: "").toString()),
            string(name: "MDP_FILE_CREDENTIALS_ID", value: (params.MDP_FILE_CREDENTIALS_ID ?: "").toString()),
            string(name: "FIREBASE_CREDENTIALS_ID", value: (params.FIREBASE_CREDENTIALS_ID ?: "").toString()),
            string(name: "PLANNING_CALENDAR_CREDENTIALS_ID", value: (params.PLANNING_CALENDAR_CREDENTIALS_ID ?: "").toString()),
            string(name: "MEETING_CALENDAR_CREDENTIALS_ID", value: (params.MEETING_CALENDAR_CREDENTIALS_ID ?: "").toString()),
            booleanParam(name: "K8S_CLEANUP_ENABLED", value: params.K8S_CLEANUP_ENABLED != false),
            string(name: "K8S_CLEANUP_RETENTION_HOURS", value: (params.K8S_CLEANUP_RETENTION_HOURS ?: "24").toString()),
            string(name: "K8S_CLEANUP_LABEL_SELECTOR", value: (params.K8S_CLEANUP_LABEL_SELECTOR ?: "").toString()),
            booleanParam(name: "K8S_PRUNE_HELM_HISTORY", value: params.K8S_PRUNE_HELM_HISTORY == true),
            string(name: "K8S_HELM_RELEASE", value: (params.K8S_HELM_RELEASE ?: "").toString()),
            string(name: "K8S_HELM_CHART", value: (params.K8S_HELM_CHART ?: "").toString()),
            string(name: "K8S_HELM_HISTORY_MAX", value: (params.K8S_HELM_HISTORY_MAX ?: "10").toString()),
            booleanParam(name: "K8S_EPHEMERAL_NAMESPACE_CLEANUP", value: params.K8S_EPHEMERAL_NAMESPACE_CLEANUP == true),
            booleanParam(name: "TRIGGER_DOWNSTREAM", value: false)
        ]
    }

    if (parallelRun) {
        parallel ids.collectEntries { sid -> [(sid): { runOne(sid) }] }
    } else {
        ids.each { sid -> runOne(sid) }
    }
}
