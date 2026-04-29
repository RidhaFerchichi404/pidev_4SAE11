pipeline {
    agent any
    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: "25", artifactNumToKeepStr: "10"))
    }
    parameters {
        string(name: "REPO_URL", defaultValue: "https://github.com/YOUR_ORG/YOUR_REPO.git", description: "Git repository URL")
        string(name: "BRANCH", defaultValue: "main", description: "Branch to build")
        string(name: "IMAGE_REPO", defaultValue: "docker.io/YOUR_DOCKERHUB_USERNAME", description: "Registry/repo prefix")
        string(name: "IMAGE_TAG", defaultValue: "", description: "Optional tag override")
        booleanParam(name: "PUSH_IMAGE", defaultValue: true, description: "Push images to Docker Hub")
        booleanParam(name: "RUN_SONARQUBE", defaultValue: true, description: "Run SonarQube analysis in child jobs")
    }
    environment {
        ORCH_TAG = "${params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER}"
        GITHUB_CREDS_ID = "GithubCredentials"
    }
    stages {
        stage("Checkout") {
            steps {
                checkout([
                    $class: "GitSCM",
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[url: params.REPO_URL, credentialsId: env.GITHUB_CREDS_ID]]
                ])
            }
        }
        stage("Infrastructure Order") {
            steps {
                script {
                    runService("services/eureka")
                    runService("services/config-server")
                    runService("services/keycloak-auth")
                }
            }
        }
        stage("Core Services Parallel") {
            steps {
                script {
                    parallel(
                        user: { runService("services/user") },
                        project: { runService("services/project") },
                        notification: { runService("services/notification") },
                        contract: { runService("services/contract") },
                        portfolio: { runService("services/portfolio") },
                        chat: { runService("services/chat") },
                        meeting: { runService("services/meeting") },
                        freelanciaJob: { runService("services/freelancia-job") },
                        aimodel: { runService("services/aimodel") }
                    )
                }
            }
        }
        stage("Dependent Services") {
            steps {
                script {
                    runService("services/planning")
                    runService("services/task")
                    parallel(
                        review: { runService("services/review") },
                        offer: { runService("services/offer") },
                        gamification: { runService("services/gamification") },
                        ticketService: { runService("services/ticket-service") },
                        subcontracting: { runService("services/subcontracting") }
                    )
                }
            }
        }
        stage("Gateway Then Frontend") {
            steps {
                script {
                    runService("services/api-gateway")
                    runService("services/frontend")
                }
            }
        }
    }
    post {
        always {
            cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        }
    }
}

def runService(String jobName) {
    def child = build job: jobName, wait: true, propagate: false, parameters: [
        string(name: "REPO_URL", value: params.REPO_URL),
        string(name: "BRANCH", value: params.BRANCH),
        string(name: "IMAGE_REPO", value: params.IMAGE_REPO),
        string(name: "IMAGE_TAG", value: ORCH_TAG),
        booleanParam(name: "PUSH_IMAGE", value: params.PUSH_IMAGE),
        booleanParam(name: "RUN_SONARQUBE", value: params.RUN_SONARQUBE),
        booleanParam(name: "TRIGGER_DOWNSTREAM", value: false)
    ]

    if (child.result == "FAILURE" || child.result == "ABORTED") {
        error("${jobName} finished with result ${child.result}")
    }

    if (child.result == "UNSTABLE") {
        unstable("${jobName} finished UNSTABLE")
    }
}
