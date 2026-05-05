#!groovy
// Shared pipeline for one service. Seed job sets SERVICE_PATH, IMAGE_NAME, defaults per job.
node {
    checkout scm

    def servicePath = params.SERVICE_PATH?.trim()
    def imageName = params.IMAGE_NAME?.trim()
    if (!servicePath) {
        error("SERVICE_PATH parameter is required (set by seed job for this service).")
    }
    if (!imageName) {
        error("IMAGE_NAME parameter is required (set by seed job for this service).")
    }

    def dockerExtra = (params.DOCKER_BUILD_ARGS ?: "").toString().trim()
    if (!dockerExtra && imageName == "frontend") {
        def apiUrl = (params.PUBLIC_API_GATEWAY_URL ?: "http://api.smartfreelance.example.com").trim()
        dockerExtra = "--build-arg API_GATEWAY_PUBLIC_URL=${apiUrl}"
    }

    def deployK8s = params.DEPLOY_TO_K8S == true
    def runner = load("ci/pipelines/microservicePipeline.groovy")
    runner.runMicroservicePipeline([
        servicePath        : servicePath,
        imageName          : imageName,
        dockerBuildArgs    : dockerExtra,
        dockerCredentialsId: (params.DOCKER_CREDENTIALS_ID ?: "DockerHubCrendentials").toString().trim(),
        githubCredentialsId: (params.GIT_CREDENTIALS_ID ?: "GithubCredentials").toString().trim(),
        skipCleanWs        : deployK8s
    ])

    if (deployK8s) {
        def kubeCtx = params.KUBE_CONTEXT?.trim()
        if (!kubeCtx) {
            error("KUBE_CONTEXT is required when DEPLOY_TO_K8S is enabled")
        }
        env.EFFECTIVE_IMAGE_TAG = (params.IMAGE_TAG?.trim()) ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER
        def kd = load("ci/pipelines/kubeDeployLib.groovy")
        def deployUnit = (params.DEPLOY_UNIT?.trim()) ?: imageName
        kd.runKubernetesDeploy([
            kubeContext                   : kubeCtx,
            kubeNamespace                 : (params.KUBE_NAMESPACE ?: "smart-freelance-dev").trim(),
            manifestPath                  : (params.MANIFEST_PATH ?: "k8s").trim(),
            imageRepo                     : (params.IMAGE_REPO ?: "docker.io/ridhaferchichi").trim(),
            imageTag                      : env.EFFECTIVE_IMAGE_TAG,
            kubeconfigCredentialsId       : (params.KUBECONFIG_CREDENTIALS_ID ?: "kubeconfig").trim(),
            deployMonitoring              : params.DEPLOY_MONITORING != false,
            monitoringManifestPath        : (params.MONITORING_MANIFEST_PATH ?: "k8s/monitoring").trim(),
            deployEnvironment             : (params.ENVIRONMENT ?: "dev").toString(),
            requireProdApproval           : params.REQUIRE_PROD_APPROVAL != false,
            dryRunOnly                    : params.DRY_RUN_ONLY == true,
            rollbackOnFailure             : params.ROLLBACK_ON_FAILURE != false,
            rolloutTimeoutSeconds         : (params.ROLLOUT_TIMEOUT_SECONDS ?: "600").toString().trim(),
            deployIngress                 : params.DEPLOY_INGRESS != false,
            deployUnits                   : deployUnit,
            githubTokenCredentialsId      : params.GITHUB_TOKEN_CREDENTIALS_ID?.trim(),
            mdpFileCredentialsId          : params.MDP_FILE_CREDENTIALS_ID?.trim(),
            firebaseCredentialsId         : params.FIREBASE_CREDENTIALS_ID?.trim(),
            planningCalendarCredentialsId : params.PLANNING_CALENDAR_CREDENTIALS_ID?.trim(),
            meetingCalendarCredentialsId  : params.MEETING_CALENDAR_CREDENTIALS_ID?.trim()
        ])
    }

    cleanWs(deleteDirs: true, disableDeferredWipeout: true)
}
