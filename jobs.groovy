def repoUrl = 'https://github.com/RidhaFerchichi404/pidev_4SAE11.git'
def branchSpec = '*/main'

// Each entry becomes jobs/services/<id> using jenkins/Microservice.Jenkinsfile (or Keycloak server image job).
def services = [
  [kind: 'keycloak-image', id: 'keycloak-server'],
  [id: 'eureka', path: 'backEnd/Eureka', image: 'eureka', deployUnit: 'eureka', dockerArgs: '', downstream: ['services/config-server']],
  [id: 'config-server', path: 'backEnd/ConfigServer', image: 'config-server', deployUnit: 'config-server', dockerArgs: '', downstream: ['services/user', 'services/project', 'services/notification', 'services/contract', 'services/portfolio', 'services/planning', 'services/task', 'services/review', 'services/offer', 'services/gamification', 'services/chat', 'services/meeting', 'services/freelancia-job', 'services/ticket-service', 'services/subcontracting']],
  [id: 'keycloak-auth', path: 'backEnd/KeyCloak', image: 'keycloak-auth', deployUnit: 'keycloak-auth', dockerArgs: '', downstream: ['services/user']],
  [id: 'user', path: 'backEnd/Microservices/user', image: 'user', deployUnit: 'user', dockerArgs: '', downstream: ['services/review', 'services/offer', 'services/task', 'services/subcontracting', 'services/ticket-service']],
  [id: 'project', path: 'backEnd/Microservices/Project', image: 'project', deployUnit: 'project', dockerArgs: '', downstream: ['services/planning', 'services/task', 'services/offer', 'services/subcontracting']],
  [id: 'notification', path: 'backEnd/Microservices/Notification', image: 'notification', deployUnit: 'notification', dockerArgs: '', downstream: ['services/review', 'services/offer', 'services/gamification', 'services/ticket-service', 'services/subcontracting']],
  [id: 'contract', path: 'backEnd/Microservices/Contract', image: 'contract', deployUnit: 'contract', dockerArgs: '', downstream: ['services/offer', 'services/subcontracting']],
  [id: 'portfolio', path: 'backEnd/Microservices/Portfolio', image: 'portfolio', deployUnit: 'portfolio', dockerArgs: '', downstream: ['services/subcontracting']],
  [id: 'planning', path: 'backEnd/Microservices/planning', image: 'planning', deployUnit: 'planning', dockerArgs: '', downstream: ['services/task', 'services/offer']],
  [id: 'task', path: 'backEnd/Microservices/task', image: 'task', deployUnit: 'task', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'review', path: 'backEnd/Microservices/review', image: 'review', deployUnit: 'review', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'offer', path: 'backEnd/Microservices/Offer', image: 'offer', deployUnit: 'offer', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'gamification', path: 'backEnd/Microservices/gamification', image: 'gamification', deployUnit: 'gamification', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'chat', path: 'backEnd/Microservices/Chat', image: 'chat', deployUnit: 'chat', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'meeting', path: 'backEnd/Microservices/Meeting', image: 'meeting', deployUnit: 'meeting', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'freelancia-job', path: 'backEnd/Microservices/FreelanciaJob', image: 'freelancia-job', deployUnit: 'freelancia-job', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'ticket-service', path: 'backEnd/Microservices/ticket-service', image: 'ticket-service', deployUnit: 'ticket-service', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'subcontracting', path: 'backEnd/Microservices/Subcontracting', image: 'subcontracting', deployUnit: 'subcontracting', dockerArgs: '', downstream: ['services/api-gateway']],
  [id: 'aimodel', path: 'backEnd/Microservices/AImodel', image: 'aimodel', deployUnit: 'aimodel', dockerArgs: '', downstream: ['services/task']],
  [id: 'aimodel-node', path: 'backEnd/Microservices/aimodel-node', image: 'aimodel-node', deployUnit: 'aimodel-node', dockerArgs: '', downstream: []],
  [id: 'api-gateway', path: 'backEnd/apiGateway', image: 'api-gateway', deployUnit: 'api-gateway', dockerArgs: '', downstream: ['services/frontend']],
  [id: 'frontend', path: 'frontend/smart-freelance-app', image: 'frontend', deployUnit: 'frontend', dockerArgs: '', downstream: []]
]

folder('services') {
  description('One pipeline job per service; shared jenkins/Microservice.Jenkinsfile')
}

folder('orchestration') {
  description('Full stack and multi-service triggers')
}

def keycloakSvc = services.find { it.kind == 'keycloak-image' }
def microSvcList = services.findAll { it.kind != 'keycloak-image' }

pipelineJob("services/${keycloakSvc.id}") {
  description("Auto-generated for ${keycloakSvc.id}. Script: jenkins/KeycloakServer.Jenkinsfile")
  logRotator {
    numToKeep(25)
    daysToKeep(30)
    artifactNumToKeep(10)
  }
  parameters {
    stringParam('REPO_URL', repoUrl, 'Git repository URL')
    stringParam('BRANCH', 'main', 'Git branch to build')
    stringParam('IMAGE_REPO', 'docker.io/ridhaferchichi', 'Registry/repo prefix')
    stringParam('IMAGE_TAG', '', 'Leave empty to use build number')
    booleanParam('PUSH_IMAGE', true, 'Push built image')
    stringParam('GIT_CREDENTIALS_ID', 'GithubCredentials', 'Unused by this job; required when triggered from orchestrator')
    stringParam('DOCKER_CREDENTIALS_ID', 'DockerHubCrendentials', 'Docker Hub credentials ID')
    booleanParam('DOCKER_CLEANUP_AFTER_BUILD', true, 'Remove local image tags and prune dangling images after build')
    booleanParam('DOCKER_IMAGE_PRUNE', true, 'Run docker image prune -f after build')
    booleanParam('DOCKER_BUILDER_PRUNE', false, 'Run docker builder prune after build')
    stringParam('DOCKER_BUILDER_KEEP_STORAGE', '8GB', 'BuildKit cache keep-storage value when builder prune is enabled')
    booleanParam('RUN_SONARQUBE', false, 'Unused for image build; orchestrator passes this for all children')
    booleanParam('TRIGGER_DOWNSTREAM', false, 'Unused; orchestrator sets false')
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
      scriptPath('jenkins/KeycloakServer.Jenkinsfile')
      lightweight(true)
    }
  }
}

microSvcList.each { svc ->
  pipelineJob("services/${svc.id}") {
    description("Auto-generated for ${svc.id}. Script: jenkins/Microservice.Jenkinsfile")
    logRotator {
      numToKeep(25)
      daysToKeep(30)
      artifactNumToKeep(10)
    }
    parameters {
      stringParam('REPO_URL', repoUrl, 'Git repository URL')
      stringParam('BRANCH', 'main', 'Git branch to build')
      stringParam('IMAGE_REPO', 'docker.io/ridhaferchichi', 'Registry/repo prefix')
      stringParam('IMAGE_TAG', '', 'Leave empty to use build number')
      booleanParam('PUSH_IMAGE', true, 'Push built image')
      booleanParam('RUN_SONARQUBE', true, 'Run SonarQube analysis and quality gate')
      stringParam('GIT_CREDENTIALS_ID', 'GithubCredentials', 'Jenkins credentials ID for Git checkout')
      stringParam('DOCKER_CREDENTIALS_ID', 'DockerHubCrendentials', 'Docker Hub credentials ID')
      booleanParam('DOCKER_CLEANUP_AFTER_BUILD', true, 'Remove local image tags and prune dangling images after build')
      booleanParam('DOCKER_IMAGE_PRUNE', true, 'Run docker image prune -f after build')
      booleanParam('DOCKER_BUILDER_PRUNE', false, 'Run docker builder prune after build')
      stringParam('DOCKER_BUILDER_KEEP_STORAGE', '8GB', 'BuildKit cache keep-storage value when builder prune is enabled')
      stringParam('SERVICE_NAME', svc.id, 'Service id (informational; matches job name)')
      stringParam('SERVICE_PATH', svc.path, 'Module path in repo')
      stringParam('IMAGE_NAME', svc.image, 'Docker image short name (registry prefix from IMAGE_REPO)')
      stringParam('DOCKER_BUILD_ARGS', svc.dockerArgs ?: '', 'Extra docker build args (frontend uses PUBLIC_API_GATEWAY_URL if empty)')
      stringParam('DEPLOY_UNIT', svc.deployUnit ?: svc.image, 'Kubernetes deploy unit name (kubeDeployLib deployUnits)')
      booleanParam('TRIGGER_DOWNSTREAM', false, 'Trigger downstream jobs')
      stringParam('DOWNSTREAM_JOBS', svc.downstream.join(','), 'Comma-separated Jenkins job names')
      booleanParam('DEPLOY_TO_K8S', false, 'After CI, deploy only this unit via kubeDeployLib')
      stringParam('KUBE_CONTEXT', '', 'Optional kubectl context (leave empty for current/in-cluster context)')
      stringParam('KUBE_NAMESPACE', 'freelance', 'Target namespace')
      stringParam('MANIFEST_PATH', 'k8s', 'Manifest directory in repo')
      booleanParam('DEPLOY_MONITORING', true, 'Deploy monitoring stack when deploying')
      stringParam('MONITORING_MANIFEST_PATH', 'k8s/monitoring', 'Monitoring manifests path')
      choiceParam('ENVIRONMENT', ['dev', 'staging', 'prod'], 'Deployment environment')
      booleanParam('REQUIRE_PROD_APPROVAL', true, 'Manual approval for prod')
      booleanParam('ROLLBACK_ON_FAILURE', true, 'Rollback failed rollouts')
      booleanParam('DRY_RUN_ONLY', false, 'kubectl server dry-run only')
      stringParam('ROLLOUT_TIMEOUT_SECONDS', '600', 'Per-deployment rollout timeout')
      booleanParam('DEPLOY_INGRESS', true, 'Apply ingress manifests')
      stringParam('PUBLIC_API_GATEWAY_URL', 'http://api.smartfreelance.example.com', 'API URL for Angular production build')
      stringParam('GITHUB_TOKEN_CREDENTIALS_ID', '', 'Optional secret text for manifest rendering')
      stringParam('MDP_FILE_CREDENTIALS_ID', '', 'Optional secret file (mdp.local)')
      stringParam('FIREBASE_CREDENTIALS_ID', '', 'Optional Firebase JSON secret file')
      stringParam('PLANNING_CALENDAR_CREDENTIALS_ID', '', 'Optional planning calendar JSON')
      stringParam('MEETING_CALENDAR_CREDENTIALS_ID', '', 'Optional meeting calendar JSON')
      booleanParam('K8S_CLEANUP_ENABLED', true, 'Run post-deploy Kubernetes cleanup tasks')
      stringParam('K8S_CLEANUP_RETENTION_HOURS', '24', 'Retention window for completed jobs/failed pods')
      stringParam('K8S_CLEANUP_LABEL_SELECTOR', '', 'Optional label selector for K8s cleanup (default app=<deploy_unit>)')
      booleanParam('K8S_PRUNE_HELM_HISTORY', false, 'Enforce Helm history max during deploy cleanup')
      stringParam('K8S_HELM_RELEASE', '', 'Helm release name when K8S_PRUNE_HELM_HISTORY=true')
      stringParam('K8S_HELM_CHART', '', 'Helm chart path/ref when K8S_PRUNE_HELM_HISTORY=true')
      stringParam('K8S_HELM_HISTORY_MAX', '10', 'Helm max history revisions when pruning is enabled')
      booleanParam('K8S_EPHEMERAL_NAMESPACE_CLEANUP', false, 'Delete namespace at end for ephemeral environments only')
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
        scriptPath('jenkins/Microservice.Jenkinsfile')
        lightweight(true)
      }
    }
  }
}

pipelineJob('orchestration/full-stack-main') {
  description('Orchestration fan-out pipeline: triggers services/* jobs with shared parameters.')
  logRotator {
    numToKeep(30)
    daysToKeep(30)
  }
  parameters {
    stringParam('REPO_URL', repoUrl, 'Git repository URL')
    stringParam('BRANCH', 'main', 'Git branch to build and deploy')
    stringParam('IMAGE_REPO', 'docker.io/ridhaferchichi', 'Registry/repo prefix for images and manifest rendering')
    stringParam('IMAGE_TAG', '', 'Optional tag; empty uses BUILD_NUMBER for build and deploy')
    booleanParam('PUSH_IMAGE', true, 'Push images to Docker Hub')
    booleanParam('RUN_SONARQUBE', true, 'Run SonarQube analysis per service')
    booleanParam('DEPLOY_TO_K8S', true, 'After successful CI, deploy to Kubernetes')
    stringParam('GIT_CREDENTIALS_ID', 'GithubCredentials', 'Jenkins credentials ID used for Git checkout')
    stringParam('DOCKER_CREDENTIALS_ID', 'DockerHubCrendentials', 'Jenkins username/password credentials ID for Docker Hub')
    booleanParam('DOCKER_CLEANUP_AFTER_BUILD', true, 'Pass local Docker cleanup toggle to child jobs')
    booleanParam('DOCKER_IMAGE_PRUNE', true, 'Pass docker image prune toggle to child jobs')
    booleanParam('DOCKER_BUILDER_PRUNE', false, 'Pass docker builder prune toggle to child jobs')
    stringParam('DOCKER_BUILDER_KEEP_STORAGE', '8GB', 'BuildKit keep-storage for child jobs when builder prune is enabled')

    stringParam('KUBE_CONTEXT', '', 'Optional Kubernetes context name (leave empty for current/in-cluster context)')
    stringParam('KUBE_NAMESPACE', 'freelance', 'Application namespace to deploy')
    stringParam('MANIFEST_PATH', 'k8s', 'Path to app manifests in repo')
    booleanParam('DEPLOY_MONITORING', true, 'Deploy Prometheus and Grafana stack')
    stringParam('MONITORING_MANIFEST_PATH', 'k8s/monitoring', 'Path to monitoring manifests')
    choiceParam('ENVIRONMENT', ['dev', 'staging', 'prod'], 'Deployment target environment')
    booleanParam('REQUIRE_PROD_APPROVAL', true, 'Manual approval when ENVIRONMENT=prod')
    booleanParam('ROLLBACK_ON_FAILURE', true, 'Rollback deployments when rollout verification fails')
    booleanParam('DRY_RUN_ONLY', false, 'kubectl apply server dry-run only')
    stringParam('ROLLOUT_TIMEOUT_SECONDS', '600', 'Rollout timeout per deployment (seconds)')
    booleanParam('DEPLOY_INGRESS', true, 'Apply k8s Ingress (requires ingress-nginx)')
    stringParam('DEPLOY_UNITS', '', 'Optional comma-separated deploy units; empty = all')
    stringParam('PUBLIC_API_GATEWAY_URL', 'http://api.smartfreelance.example.com', 'Public API URL for Angular production build')
    stringParam('GITHUB_TOKEN_CREDENTIALS_ID', '', 'Optional Jenkins secret text credential ID to export GITHUB_TOKEN during secrets rendering')
    stringParam('MDP_FILE_CREDENTIALS_ID', '', 'Optional Jenkins secret file credential ID for mdp.local contents')
    stringParam('FIREBASE_CREDENTIALS_ID', '', 'Optional Jenkins secret file credential ID for firebase admin JSON')
    stringParam('PLANNING_CALENDAR_CREDENTIALS_ID', '', 'Optional Jenkins secret file credential ID for planning calendar service account JSON')
    stringParam('MEETING_CALENDAR_CREDENTIALS_ID', '', 'Optional Jenkins secret file credential ID for meeting calendar service account JSON')
    booleanParam('K8S_CLEANUP_ENABLED', true, 'Pass K8s cleanup toggle to child jobs')
    stringParam('K8S_CLEANUP_RETENTION_HOURS', '24', 'Retention window passed to child jobs')
    stringParam('K8S_CLEANUP_LABEL_SELECTOR', '', 'Optional cleanup selector passed to child jobs')
    booleanParam('K8S_PRUNE_HELM_HISTORY', false, 'Pass Helm history prune toggle to child jobs')
    stringParam('K8S_HELM_RELEASE', '', 'Helm release passed to child jobs')
    stringParam('K8S_HELM_CHART', '', 'Helm chart passed to child jobs')
    stringParam('K8S_HELM_HISTORY_MAX', '10', 'Helm history max passed to child jobs')
    booleanParam('K8S_EPHEMERAL_NAMESPACE_CLEANUP', false, 'Pass ephemeral namespace cleanup toggle to child jobs')
    stringParam('SERVICE_IDS', '', 'Comma-separated services ids; empty = default dependency order')
    booleanParam('PARALLEL', false, 'Run selected services in parallel (no dependency ordering)')
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
      scriptPath('jenkins/Orchestrator.Jenkinsfile')
      lightweight(true)
    }
  }
}

pipelineJob('orchestration/trigger-all-services') {
  description('Runs each services/* job (sequential by default). Uses jenkins/Orchestrator.Jenkinsfile.')
  logRotator {
    numToKeep(15)
    daysToKeep(30)
  }
  parameters {
    stringParam('REPO_URL', repoUrl, 'Git repository URL')
    stringParam('BRANCH', 'main', 'Branch passed to each child job')
    stringParam('IMAGE_REPO', 'docker.io/ridhaferchichi', 'Registry prefix for child jobs')
    stringParam('IMAGE_TAG', '', 'Optional tag for child jobs')
    booleanParam('PUSH_IMAGE', true, 'Push images in child jobs')
    booleanParam('RUN_SONARQUBE', true, 'Sonar in child jobs')
    stringParam('GIT_CREDENTIALS_ID', 'GithubCredentials', 'Git credentials id')
    stringParam('DOCKER_CREDENTIALS_ID', 'DockerHubCrendentials', 'Docker credentials id')
    booleanParam('DOCKER_CLEANUP_AFTER_BUILD', true, 'Pass local Docker cleanup toggle to child jobs')
    booleanParam('DOCKER_IMAGE_PRUNE', true, 'Pass docker image prune toggle to child jobs')
    booleanParam('DOCKER_BUILDER_PRUNE', false, 'Pass docker builder prune toggle to child jobs')
    stringParam('DOCKER_BUILDER_KEEP_STORAGE', '8GB', 'BuildKit keep-storage for child jobs when builder prune is enabled')
    stringParam('SERVICE_IDS', '', 'Comma-separated services/keycloak-server ids; empty = default order')
    booleanParam('PARALLEL', false, 'Run all listed services in parallel (no dependency ordering)')
    booleanParam('DEPLOY_TO_K8S', false, 'Pass deploy toggle to child jobs')
    stringParam('KUBE_CONTEXT', '', 'Optional Kubernetes context for child jobs')
    stringParam('KUBE_NAMESPACE', 'freelance', 'Namespace for child jobs')
    stringParam('MANIFEST_PATH', 'k8s', 'Manifest path for child jobs')
    booleanParam('DEPLOY_MONITORING', true, 'Deploy monitoring in child jobs')
    stringParam('MONITORING_MANIFEST_PATH', 'k8s/monitoring', 'Monitoring manifest path for child jobs')
    choiceParam('ENVIRONMENT', ['dev', 'staging', 'prod'], 'Deployment environment for child jobs')
    booleanParam('REQUIRE_PROD_APPROVAL', true, 'Require prod approval in child jobs')
    booleanParam('ROLLBACK_ON_FAILURE', true, 'Rollback failed deploy in child jobs')
    booleanParam('DRY_RUN_ONLY', false, 'Dry-run only in child jobs')
    stringParam('ROLLOUT_TIMEOUT_SECONDS', '600', 'Rollout timeout passed to child jobs')
    booleanParam('DEPLOY_INGRESS', true, 'Deploy ingress in child jobs')
    stringParam('PUBLIC_API_GATEWAY_URL', 'http://api.smartfreelance.example.com', 'Frontend API URL passed to child jobs')
    stringParam('GITHUB_TOKEN_CREDENTIALS_ID', '', 'Optional secret text passed to child jobs')
    stringParam('MDP_FILE_CREDENTIALS_ID', '', 'Optional mdp file secret passed to child jobs')
    stringParam('FIREBASE_CREDENTIALS_ID', '', 'Optional firebase file secret passed to child jobs')
    stringParam('PLANNING_CALENDAR_CREDENTIALS_ID', '', 'Optional planning calendar secret passed to child jobs')
    stringParam('MEETING_CALENDAR_CREDENTIALS_ID', '', 'Optional meeting calendar secret passed to child jobs')
    booleanParam('K8S_CLEANUP_ENABLED', true, 'Pass K8s cleanup toggle to child jobs')
    stringParam('K8S_CLEANUP_RETENTION_HOURS', '24', 'Retention window passed to child jobs')
    stringParam('K8S_CLEANUP_LABEL_SELECTOR', '', 'Optional cleanup selector passed to child jobs')
    booleanParam('K8S_PRUNE_HELM_HISTORY', false, 'Pass Helm history prune toggle to child jobs')
    stringParam('K8S_HELM_RELEASE', '', 'Helm release passed to child jobs')
    stringParam('K8S_HELM_CHART', '', 'Helm chart passed to child jobs')
    stringParam('K8S_HELM_HISTORY_MAX', '10', 'Helm history max passed to child jobs')
    booleanParam('K8S_EPHEMERAL_NAMESPACE_CLEANUP', false, 'Pass ephemeral namespace cleanup toggle to child jobs')
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
      scriptPath('jenkins/Orchestrator.Jenkinsfile')
      lightweight(true)
    }
  }
}
