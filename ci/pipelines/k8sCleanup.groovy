/**
 * Kubernetes cleanup helpers.
 * All operations are guarded by flags and namespace scoping.
 */
def runK8sCleanup(Map cfg = [:]) {
    if (cfg.enabled == false) {
        echo "Kubernetes cleanup disabled."
        return
    }

    def ns = (cfg.namespace ?: "").toString().trim()
    if (!ns) {
        error("namespace is required for Kubernetes cleanup")
    }

    def retentionHours = (cfg.retentionHours ?: "24").toString().trim()
    def labelSelector = (cfg.labelSelector ?: "").toString().trim()
    def labelArg = labelSelector ? "-l '${labelSelector}'" : ""
    def pruneCompletedJobs = cfg.cleanupCompletedJobs != false
    def pruneFailedPods = cfg.cleanupFailedPods != false
    def pruneHelm = cfg.pruneHelmHistory == true
    def helmRelease = (cfg.helmRelease ?: "").toString().trim()
    def helmChart = (cfg.helmChart ?: "").toString().trim()
    def helmHistoryMax = (cfg.helmHistoryMax ?: "10").toString().trim()
    def cleanupEphemeralNamespace = cfg.cleanupEphemeralNamespace == true
    def allowNamespaceDelete = cfg.allowNamespaceDelete == true

    int retentionSeconds
    try {
        retentionSeconds = Integer.parseInt(retentionHours) * 3600
    } catch (ignored) {
        retentionSeconds = 24 * 3600
    }

    if (pruneCompletedJobs) {
        sh """
          now=\$(date +%s)
          kubectl -n '${ns}' get jobs ${labelArg} -o jsonpath='{range .items[*]}{.metadata.name}{"|"}{.status.completionTime}{"\\n"}{end}' 2>/dev/null | while IFS='|' read -r name completion; do
            [ -z "\$name" ] && continue
            [ -z "\$completion" ] && continue
            ts=\$(date -d "\$completion" +%s 2>/dev/null || true)
            [ -z "\$ts" ] && continue
            age=\$((now - ts))
            if [ "\$age" -gt ${retentionSeconds} ]; then
              kubectl -n '${ns}' delete job "\$name" --ignore-not-found=true || true
            fi
          done
        """
    }

    if (pruneFailedPods) {
        sh """
          now=\$(date +%s)
          kubectl -n '${ns}' get pods ${labelArg} --field-selector=status.phase=Failed -o jsonpath='{range .items[*]}{.metadata.name}{"|"}{.metadata.creationTimestamp}{"\\n"}{end}' 2>/dev/null | while IFS='|' read -r name created; do
            [ -z "\$name" ] && continue
            [ -z "\$created" ] && continue
            ts=\$(date -d "\$created" +%s 2>/dev/null || true)
            [ -z "\$ts" ] && continue
            age=\$((now - ts))
            if [ "\$age" -gt ${retentionSeconds} ]; then
              kubectl -n '${ns}' delete pod "\$name" --ignore-not-found=true || true
            fi
          done
        """
    }

    if (pruneHelm && helmRelease && helmChart) {
        sh "helm upgrade --install '${helmRelease}' '${helmChart}' -n '${ns}' --history-max '${helmHistoryMax}' --reuse-values || true"
    }

    if (cleanupEphemeralNamespace && allowNamespaceDelete) {
        sh "kubectl delete namespace '${ns}' --ignore-not-found=true || true"
    }
}

return this
