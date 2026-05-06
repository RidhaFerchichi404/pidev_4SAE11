/**
 * Build/push Keycloak server image (keycloak-start/Dockerfile: realm import + conf).
 * cfg: dockerCredentialsId, imageRepo, imageTag, pushImage (boolean, default true)
 */
def buildAndPushKeycloakServerImage(Map cfg) {
    def dockerCredsId = (cfg.dockerCredentialsId ?: "DockerHubCrendentials").trim()
    def imageRepo = cfg.imageRepo
    def tag = cfg.imageTag?.toString()?.trim() ?: error("imageTag is required")
    def pushImage = cfg.pushImage != false
    def dockerCleanupEnabled = cfg.dockerCleanupEnabled != false
    def dockerImagePruneEnabled = cfg.dockerImagePrune != false
    def dockerBuilderPruneEnabled = cfg.dockerBuilderPrune == true
    def dockerBuilderKeepStorage = (cfg.dockerBuilderKeepStorage ?: "8GB").toString().trim()
    def dockerImage = "${imageRepo}/keycloak"
    def fullImage = "${dockerImage}:${tag}"
    sh """
      docker build -f keycloak-start/Dockerfile -t '${fullImage}' -t '${dockerImage}:latest' .
    """
    if (!pushImage) {
        echo "Skipping Keycloak image push (pushImage is false)."
    } else {
        withCredentials([usernamePassword(credentialsId: dockerCredsId, usernameVariable: "DH_USER", passwordVariable: "DH_PASS")]) {
            sh """
              echo "\$DH_PASS" | docker login docker.io -u "\$DH_USER" --password-stdin
            """
            def pushTargets = [full: fullImage, base: dockerImage]
            script {
                def ir = (imageRepo ?: "").trim()
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

    if (dockerCleanupEnabled) {
        def dockerCleanup = load("ci/pipelines/dockerDiskCleanup.groovy")
        dockerCleanup.cleanupLocalDockerArtifacts([
            tags              : [fullImage, "${dockerImage}:latest"],
            imagePrune        : dockerImagePruneEnabled,
            builderPrune      : dockerBuilderPruneEnabled,
            builderKeepStorage: dockerBuilderKeepStorage
        ])
    } else {
        echo "Skipping local Docker cleanup for Keycloak image."
    }
}

return this
