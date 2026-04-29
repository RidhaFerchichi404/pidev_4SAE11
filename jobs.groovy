def repoUrl = 'https://github.com/RidhaFerchichi404/pidev_4SAE11.git'
def branchSpec = '*/main'

def services = [
  [id: 'eureka', path: 'backEnd/Eureka/Jenkinsfile', downstream: ['services/config-server']],
  [id: 'config-server', path: 'backEnd/ConfigServer/Jenkinsfile', downstream: ['services/user','services/project','services/notification','services/contract','services/portfolio','services/planning','services/task','services/review','services/offer','services/gamification','services/chat','services/meeting','services/freelancia-job','services/ticket-service','services/subcontracting']],
  [id: 'keycloak-auth', path: 'backEnd/KeyCloak/Jenkinsfile', downstream: ['services/user']],
  [id: 'user', path: 'backEnd/Microservices/user/Jenkinsfile', downstream: ['services/review','services/offer','services/task','services/subcontracting','services/ticket-service']],
  [id: 'project', path: 'backEnd/Microservices/Project/Jenkinsfile', downstream: ['services/planning','services/task','services/offer','services/subcontracting']],
  [id: 'notification', path: 'backEnd/Microservices/Notification/Jenkinsfile', downstream: ['services/review','services/offer','services/gamification','services/ticket-service','services/subcontracting']],
  [id: 'contract', path: 'backEnd/Microservices/Contract/Jenkinsfile', downstream: ['services/offer','services/subcontracting']],
  [id: 'portfolio', path: 'backEnd/Microservices/Portfolio/Jenkinsfile', downstream: ['services/subcontracting']],
  [id: 'planning', path: 'backEnd/Microservices/planning/Jenkinsfile', downstream: ['services/task','services/offer']],
  [id: 'task', path: 'backEnd/Microservices/task/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'review', path: 'backEnd/Microservices/review/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'offer', path: 'backEnd/Microservices/Offer/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'gamification', path: 'backEnd/Microservices/gamification/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'chat', path: 'backEnd/Microservices/Chat/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'meeting', path: 'backEnd/Microservices/Meeting/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'freelancia-job', path: 'backEnd/Microservices/FreelanciaJob/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'ticket-service', path: 'backEnd/Microservices/ticket-service/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'subcontracting', path: 'backEnd/Microservices/Subcontracting/Jenkinsfile', downstream: ['services/api-gateway']],
  [id: 'aimodel', path: 'backEnd/Microservices/AImodel/Jenkinsfile', downstream: ['services/task']],
  [id: 'api-gateway', path: 'backEnd/apiGateway/Jenkinsfile', downstream: ['services/frontend']],
  [id: 'frontend', path: 'frontend/smart-freelance-app/Jenkinsfile', downstream: []]
]

folder('services') {
  description('Auto-generated microservice pipeline jobs')
}

folder('orchestration') {
  description('Auto-generated orchestration pipelines')
}

services.each { svc ->
  pipelineJob("services/${svc.id}") {
    description("Auto-generated for ${svc.id}. Reads Jenkinsfile from main branch.")
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
      booleanParam('TRIGGER_DOWNSTREAM', false, 'Trigger downstream jobs')
      stringParam('DOWNSTREAM_JOBS', svc.downstream.join(','), 'Comma-separated downstream jobs')
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
        scriptPath(svc.path)
        lightweight(true)
      }
    }
  }
}

pipelineJob('orchestration/full-stack-main') {
  description('Dependency-aware full stack pipeline (infra -> services -> gateway -> frontend).')
  logRotator {
    numToKeep(30)
    daysToKeep(30)
  }
  parameters {
    stringParam('REPO_URL', repoUrl, 'Git repository URL')
    stringParam('BRANCH', 'main', 'Git branch to build')
    stringParam('IMAGE_REPO', 'docker.io/ridhaferchichi', 'Registry/repo prefix')
    stringParam('IMAGE_TAG', '', 'Leave empty to use build number')
    booleanParam('PUSH_IMAGE', true, 'Push all images')
    booleanParam('RUN_SONARQUBE', true, 'Run SonarQube analysis and quality gate')
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
      scriptPath('Jenkinsfile')
      lightweight(true)
    }
  }
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
