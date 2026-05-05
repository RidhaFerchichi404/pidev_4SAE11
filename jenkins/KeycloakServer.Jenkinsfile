#!groovy
// Keycloak container image from keycloak-start/Dockerfile (realm import). Job: services/keycloak-server
node {
    checkout scm
    def kc = load("ci/pipelines/keycloakServerImage.groovy")
    def tag = (params.IMAGE_TAG?.trim()) ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER
    kc.buildAndPushKeycloakServerImage([
        dockerCredentialsId: (params.DOCKER_CREDENTIALS_ID ?: "DockerHubCrendentials").trim(),
        imageRepo            : (params.IMAGE_REPO ?: "docker.io/ridhaferchichi").trim(),
        imageTag             : tag,
        pushImage            : params.PUSH_IMAGE != false
    ])
    cleanWs(deleteDirs: true, disableDeferredWipeout: true)
}
