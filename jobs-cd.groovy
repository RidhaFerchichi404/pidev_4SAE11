def repoUrl = 'https://github.com/RidhaFerchichi404/pidev_4SAE11.git'
def branchSpec = '*/main'

folder('orchestration') {
  description('Auto-generated orchestration pipelines')
}

pipelineJob('orchestration/cd-k8s-main') {
  description('Standalone CD pipeline for kubeadm deployment and monitoring stack.')
  logRotator {
    numToKeep(30)
    daysToKeep(30)
  }
  parameters {
    stringParam('REPO_URL', repoUrl, 'Git repository URL containing Kubernetes manifests')
    stringParam('BRANCH', 'main', 'Git branch to deploy from')
    stringParam('KUBECONFIG_CREDENTIALS_ID', 'kubeconfig', 'Jenkins secret file credential ID for kubeconfig')
    stringParam('KUBE_CONTEXT', 'kubernetes-admin@kubernetes', 'Kubernetes context name from kubeconfig')
    stringParam('KUBE_NAMESPACE', 'smart-freelance-dev', 'Namespace to deploy application manifests into')
    stringParam('MANIFEST_PATH', 'k8s', 'Path to app manifests in repository')
    stringParam('IMAGE_REPO', 'docker.io/ridhaferchichi', 'Registry/repo prefix')
    stringParam('IMAGE_TAG', '', 'Immutable image tag to deploy (required)')
    booleanParam('DEPLOY_MONITORING', true, 'Deploy Prometheus and Grafana stack')
    stringParam('MONITORING_MANIFEST_PATH', 'k8s/monitoring', 'Path to monitoring manifests')
    choiceParam('ENVIRONMENT', ['dev', 'staging', 'prod'], 'Deployment target environment')
    booleanParam('REQUIRE_PROD_APPROVAL', true, 'Require manual approval for prod deploy')
    booleanParam('ROLLBACK_ON_FAILURE', true, 'Rollback Deployments on rollout failure')
    booleanParam('DRY_RUN_ONLY', false, 'Use server-side dry-run mode only')
  }
  definition {
    cpsScm {
      scm {
        git {
          remote {
            url(repoUrl)
            credentials('GithubCredentials')
          }
          branch(branchSpec)
        }
      }
      scriptPath('ci/pipelines/kubeCdPipeline.groovy')
      lightweight(true)
    }
  }
}
