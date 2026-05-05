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
    def pushImg = params.PUSH_IMAGE != false
    def runSonar = params.RUN_SONARQUBE != false

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
            booleanParam(name: "TRIGGER_DOWNSTREAM", value: false)
        ]
    }

    if (parallelRun) {
        parallel ids.collectEntries { sid -> [(sid): { runOne(sid) }] }
    } else {
        ids.each { sid -> runOne(sid) }
    }
}
