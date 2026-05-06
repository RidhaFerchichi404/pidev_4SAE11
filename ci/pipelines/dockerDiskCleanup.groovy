/**
 * Docker disk cleanup helpers for Jenkins agents.
 * Safe defaults:
 * - remove only tags created by this build
 * - prune dangling images
 * Optional:
 * - prune BuildKit cache with bounded keep storage
 */
def cleanupLocalDockerArtifacts(Map cfg = [:]) {
    def tags = (cfg.tags ?: [])
            .collect { it?.toString()?.trim() }
            .findAll { it }
            .unique()
    def runImagePrune = cfg.imagePrune != false
    def runBuilderPrune = cfg.builderPrune == true
    def keepStorage = (cfg.builderKeepStorage ?: "").toString().trim()

    if (tags) {
        echo "Cleaning local Docker tags: ${tags.join(', ')}"
        tags.each { tag ->
            sh "docker rmi '${tag}' || true"
        }
    }

    if (runImagePrune) {
        sh "docker image prune -f || true"
    }

    if (runBuilderPrune) {
        def keepArg = keepStorage ? "--keep-storage ${keepStorage}" : ""
        sh "docker builder prune -f ${keepArg} || true"
    }
}

def cleanupOrchestrationWorkspace() {
    deleteDir()
}

return this
