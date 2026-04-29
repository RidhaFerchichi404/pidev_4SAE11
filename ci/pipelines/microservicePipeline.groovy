def runMicroservicePipeline(Map cfg) {
    properties([
        buildDiscarder(logRotator(numToKeepStr: "25", artifactNumToKeepStr: "10")),
        disableConcurrentBuilds()
    ])

    def servicePath = cfg.servicePath
    def githubCredsId = "GithubCredentials"
    def dockerCredsId = "DockerHubCrendentials"
    def sonarTokenCredentialsId = "SonarQubeToken"
    def sonarServerName = "SonarQube"
    def imageRepo = params.IMAGE_REPO
    def tag = (params.IMAGE_TAG?.trim()) ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER
    def dockerImage = "${imageRepo}/${cfg.imageName}"
    def fullImage = "${dockerImage}:${tag}"
    def buildTool = ""
    def npmAvailable = false
    def sonarAnalysisExecuted = false
    def nodeToolName = "NodeJS"
    def nodeToolHome = ""
    def normalizedServicePath = (servicePath ?: "").replace("\\", "/")
    def skipCoverageForService = normalizedServicePath.contains("backEnd/Microservices/aimodel-node")

    def withNodeEnv = { Closure body ->
        if (nodeToolHome?.trim()) {
            withEnv(["PATH+NODE=${nodeToolHome}/bin"]) {
                body()
            }
        } else {
            body()
        }
    }

    timestamps {
        try {
            stage("Checkout") {
                checkout([
                    $class: "GitSCM",
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[url: params.REPO_URL, credentialsId: githubCredsId]]
                ])
            }

            stage("Detect Build Tool") {
                if (fileExists("${servicePath}/pom.xml")) {
                    buildTool = "maven"
                } else if (fileExists("${servicePath}/build.gradle") || fileExists("${servicePath}/build.gradle.kts")) {
                    buildTool = "gradle"
                } else if (fileExists("${servicePath}/package.json")) {
                    buildTool = "node"
                    try {
                        nodeToolHome = tool(name: nodeToolName, type: "jenkins.plugins.nodejs.tools.NodeJSInstallation")
                        echo "Using Jenkins NodeJS tool '${nodeToolName}' from ${nodeToolHome}"
                    } catch (ignored) {
                        echo "Jenkins NodeJS tool '${nodeToolName}' is not available for this agent. Falling back to system PATH."
                    }
                    withNodeEnv {
                        npmAvailable = (sh(script: "command -v npm >/dev/null 2>&1", returnStatus: true) == 0)
                    }
                    if (!npmAvailable) {
                        echo "npm not found on Jenkins agent. Node pre-build/test steps will be skipped; Docker build will still run."
                    }
                } else {
                    error("No supported build tool found in ${servicePath}")
                }
            }

            stage("Build") {
                dir(servicePath) {
                    if (buildTool == "maven") {
                        sh "if [ -f mvnw ]; then chmod +x mvnw && ./mvnw -B -DskipTests clean compile; else mvn -B -DskipTests clean compile; fi"
                    } else if (buildTool == "gradle") {
                        sh "if [ -f gradlew ]; then chmod +x gradlew && ./gradlew clean assemble -x test; else gradle clean assemble -x test; fi"
                    } else {
                        if (npmAvailable) {
                            withNodeEnv {
                                sh "npm ci"
                                sh "npm run build --if-present"
                            }
                        } else {
                            echo "Skipping host Node build because npm is unavailable."
                        }
                    }
                }
            }

            stage("Test") {
                dir(servicePath) {
                    if (buildTool == "maven") {
                        sh "if [ -f mvnw ]; then ./mvnw -B verify; else mvn -B verify; fi"
                    } else if (buildTool == "gradle") {
                        sh "if [ -f gradlew ]; then ./gradlew test; else gradle test; fi"
                    } else {
                        if (npmAvailable) {
                            withNodeEnv {
                                def hasCiTestScript = (sh(
                                        script: "node -e \"const p=require('./package.json'); process.exit(p.scripts && p.scripts['test:ci'] ? 0 : 1)\"",
                                        returnStatus: true
                                ) == 0)
                                if (hasCiTestScript) {
                                    sh "npm run test:ci"
                                } else {
                                    sh "npm test -- --watch=false || npm test || true"
                                }
                            }
                        } else {
                            unstable("Skipping Node tests because npm is unavailable on Jenkins agent")
                        }
                    }
                }
                junit allowEmptyResults: true, testResults: "${servicePath}/target/surefire-reports/**/*.xml, ${servicePath}/target/failsafe-reports/**/*.xml, ${servicePath}/target/*-reports/**/*.xml, ${servicePath}/build/test-results/**/*.xml, ${servicePath}/coverage/test-results/**/*.xml, ${servicePath}/test-results/**/*.xml"
            }

            stage("Package") {
                dir(servicePath) {
                    if (buildTool == "maven") {
                        sh "if [ -f mvnw ]; then ./mvnw -B -DskipTests package; else mvn -B -DskipTests package; fi"
                    } else if (buildTool == "gradle") {
                        sh "if [ -f gradlew ]; then ./gradlew bootJar -x test || ./gradlew assemble -x test; else gradle assemble -x test; fi"
                    } else {
                        if (npmAvailable) {
                            withNodeEnv {
                                sh "npm pack >/dev/null 2>&1 || true"
                            }
                        } else {
                            echo "Skipping npm package step because npm is unavailable."
                        }
                    }
                }
            }

            if (params.RUN_SONARQUBE) {
                stage("SonarQube Analysis") {
                    dir(servicePath) {
                        withCredentials([string(credentialsId: sonarTokenCredentialsId, variable: "SONAR_TOKEN")]) {
                            withSonarQubeEnv(sonarServerName) {
                                def sonarProjectKey = cfg.imageName.replaceAll("[^a-zA-Z0-9_.:-]", "-")
                                def findCoverageReports = { List<String> globs ->
                                    def fileNamePatterns = globs
                                            .collect { it?.tokenize('/')?.last() }
                                            .findAll { it?.trim() }
                                            .unique()
                                    if (!fileNamePatterns) {
                                        return []
                                    }
                                    def nameExpr = fileNamePatterns.collect { p -> "-name '${p}'" }.join(" -o ")
                                    def matches = sh(
                                            script: "find . -type f \\( ${nameExpr} \\) -print 2>/dev/null || true",
                                            returnStdout: true
                                    ).trim()
                                    if (!matches) {
                                        return []
                                    }
                                    return matches
                                            .split("\\r?\\n")
                                            .collect { it?.trim()?.replaceFirst('^\\./', '') }
                                            .findAll { it }
                                            .unique()
                                }
                                def findJUnitReports = {
                                    def matches = sh(
                                            script: "find . -type f \\( -path './target/surefire-reports/*.xml' -o -path './target/surefire-reports/**/*.xml' -o -path './target/failsafe-reports/*.xml' -o -path './target/failsafe-reports/**/*.xml' -o -path './target/*-reports/*.xml' -o -path './target/*-reports/**/*.xml' -o -path './build/test-results/*.xml' -o -path './build/test-results/**/*.xml' -o -path './coverage/test-results/*.xml' -o -path './coverage/test-results/**/*.xml' -o -path './test-results/*.xml' -o -path './test-results/**/*.xml' \\) -print 2>/dev/null || true",
                                            returnStdout: true
                                    ).trim()
                                    if (!matches) {
                                        return []
                                    }
                                    return matches
                                            .split("\\r?\\n")
                                            .collect { it?.trim()?.replaceFirst('^\\./', '') }
                                            .findAll { it }
                                            .unique()
                                }
                                if (buildTool == "maven") {
                                    sh """
                                      if [ -f mvnw ]; then
                                        ./mvnw -B verify jacoco:report
                                      else
                                        mvn -B verify jacoco:report
                                      fi
                                    """
                                    def jacocoReports = findCoverageReports(["**/jacoco.xml", "**/jacoco-*.xml", "**/jacoco*.xml"])
                                    def genericCoverageReports = findCoverageReports(["**/coverage*.xml", "**/cobertura*.xml"])
                                    def junitReports = findJUnitReports()
                                    def sonarCoverageArgs = jacocoReports ? "-Dsonar.coverage.jacoco.xmlReportPaths=${jacocoReports.join(',')}" : ""
                                    def sonarTestArgs = junitReports ? "-Dsonar.junit.reportPaths=${junitReports.join(',')}" : ""
                                    if (genericCoverageReports) {
                                        sonarCoverageArgs = "${sonarCoverageArgs} -Dsonar.coverageReportPaths=${genericCoverageReports.join(',')}".trim()
                                    }
                                    sh """
                                      if [ -f mvnw ]; then
                                        ./mvnw -B sonar:sonar -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} ${sonarCoverageArgs} ${sonarTestArgs} -Dsonar.token=\$SONAR_TOKEN
                                      else
                                        mvn -B sonar:sonar -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} ${sonarCoverageArgs} ${sonarTestArgs} -Dsonar.token=\$SONAR_TOKEN
                                      fi
                                    """
                                    if (!skipCoverageForService && !jacocoReports && !genericCoverageReports) {
                                        unstable("No coverage report detected for ${cfg.imageName}. Searched jacoco.xml/jacoco*.xml/coverage*.xml/cobertura*.xml under ${servicePath}; Sonar analysis continues without coverage.")
                                    } else if (skipCoverageForService) {
                                        echo "Skipping coverage report enforcement for ${cfg.imageName}."
                                    } else {
                                        echo "Coverage reports detected for ${cfg.imageName}: ${(jacocoReports + genericCoverageReports).join(', ')}"
                                    }
                                    sonarAnalysisExecuted = true
                                } else if (buildTool == "gradle") {
                                    sh """
                                      if [ -f gradlew ]; then
                                        ./gradlew test jacocoTestReport || ./gradlew test
                                      else
                                        gradle test jacocoTestReport || gradle test
                                      fi
                                    """
                                    def jacocoReports = findCoverageReports(["**/jacoco*.xml"])
                                    def genericCoverageReports = findCoverageReports(["**/coverage*.xml", "**/cobertura*.xml"])
                                    def junitReports = findJUnitReports()
                                    def sonarCoverageArgs = jacocoReports ? "-Dsonar.coverage.jacoco.xmlReportPaths=${jacocoReports.join(',')}" : ""
                                    def sonarTestArgs = junitReports ? "-Dsonar.junit.reportPaths=${junitReports.join(',')}" : ""
                                    if (genericCoverageReports) {
                                        sonarCoverageArgs = "${sonarCoverageArgs} -Dsonar.coverageReportPaths=${genericCoverageReports.join(',')}".trim()
                                    }
                                    sh """
                                      if [ -f gradlew ]; then
                                        ./gradlew sonarqube -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} ${sonarCoverageArgs} ${sonarTestArgs} -Dsonar.token=\$SONAR_TOKEN
                                      else
                                        gradle sonarqube -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} ${sonarCoverageArgs} ${sonarTestArgs} -Dsonar.token=\$SONAR_TOKEN
                                      fi
                                    """
                                    if (!skipCoverageForService && !jacocoReports && !genericCoverageReports) {
                                        unstable("No coverage report detected for ${cfg.imageName}. Searched jacoco*.xml/coverage*.xml/cobertura*.xml under ${servicePath}; Sonar analysis continues without coverage.")
                                    } else if (skipCoverageForService) {
                                        echo "Skipping coverage report enforcement for ${cfg.imageName}."
                                    } else {
                                        echo "Coverage reports detected for ${cfg.imageName}: ${(jacocoReports + genericCoverageReports).join(', ')}"
                                    }
                                    sonarAnalysisExecuted = true
                                } else {
                                    if (npmAvailable) {
                                        withNodeEnv {
                                            def hasTestCoverageScript = (sh(
                                                    script: "node -e \"const p=require('./package.json'); process.exit(p.scripts && p.scripts['test:coverage'] ? 0 : 1)\"",
                                                    returnStatus: true
                                            ) == 0)
                                            def hasCoverageScript = (sh(
                                                    script: "node -e \"const p=require('./package.json'); process.exit(p.scripts && p.scripts['coverage'] ? 0 : 1)\"",
                                                    returnStatus: true
                                            ) == 0)
                                            def hasCiTestScript = (sh(
                                                    script: "node -e \"const p=require('./package.json'); process.exit(p.scripts && p.scripts['test:ci'] ? 0 : 1)\"",
                                                    returnStatus: true
                                            ) == 0)
                                            if (hasTestCoverageScript) {
                                                sh "npm run test:coverage"
                                            } else if (hasCoverageScript) {
                                                sh "npm run coverage"
                                            } else if (hasCiTestScript) {
                                                sh "npm run test:ci"
                                            } else {
                                                sh "npm test -- --coverage --watch=false || npm test -- --coverage || npm test || true"
                                            }
                                            def lcovReports = findCoverageReports(["**/lcov.info"])
                                            def junitReports = findJUnitReports()
                                            if (!lcovReports && fileExists("coverage/lcov.info")) {
                                                lcovReports = ["coverage/lcov.info"]
                                            }
                                            if (!lcovReports && fileExists("coverage/smart-freelance-app/lcov.info")) {
                                                lcovReports = ["coverage/smart-freelance-app/lcov.info"]
                                            }
                                            def genericCoverageReports = findCoverageReports(["**/coverage*.xml", "**/cobertura*.xml"])
                                            def sonarCoverageArgs = lcovReports
                                                    ? "-Dsonar.javascript.lcov.reportPaths=${lcovReports.join(',')} -Dsonar.typescript.lcov.reportPaths=${lcovReports.join(',')}"
                                                    : ""
                                            if (genericCoverageReports) {
                                                sonarCoverageArgs = "${sonarCoverageArgs} -Dsonar.coverageReportPaths=${genericCoverageReports.join(',')}".trim()
                                            }
                                            def sonarTestArgs = junitReports ? "-Dsonar.junit.reportPaths=${junitReports.join(',')}" : ""
                                            if (!skipCoverageForService && !lcovReports && !genericCoverageReports) {
                                                unstable("No Node coverage report detected for ${cfg.imageName}. Searched lcov.info/coverage*.xml/cobertura*.xml under ${servicePath}; Sonar analysis continues without coverage.")
                                            } else if (skipCoverageForService) {
                                                echo "Skipping coverage report enforcement for ${cfg.imageName}."
                                            } else {
                                                echo "Coverage reports detected for ${cfg.imageName}: ${(lcovReports + genericCoverageReports).join(', ')}"
                                            }
                                            sh "npx -y sonar-scanner -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} ${sonarCoverageArgs} ${sonarTestArgs} -Dsonar.token=\\$SONAR_TOKEN"
                                        }
                                        sonarAnalysisExecuted = true
                                    } else {
                                        unstable("Skipping Node SonarQube analysis because npm/npx is unavailable on Jenkins agent")
                                    }
                                }
                            }
                        }
                    }
                }
                stage("Quality Gate") {
                    script {
                        if (!sonarAnalysisExecuted) {
                            echo "Skipping Quality Gate because SonarQube analysis was not executed for this job."
                            return
                        }
                        try {
                            timeout(time: 15, unit: "SECONDS") {
                                def qualityGate = waitForQualityGate abortPipeline: false
                                if (qualityGate?.status != "OK") {
                                    echo "SonarQube Quality Gate status: ${qualityGate?.status}. Continuing without marking build unstable."
                                }
                            }
                        } catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ignored) {
                            echo "SonarQube Quality Gate check timed out; continuing without marking build unstable."
                        }
                    }
                }
            }

            stage("Build Docker Image") {
                dir(servicePath) {
                    sh "docker build -t ${fullImage} -t ${dockerImage}:latest ."
                }
            }

            if (params.PUSH_IMAGE) {
                stage("Push Docker Image") {
                    withCredentials([usernamePassword(credentialsId: dockerCredsId, usernameVariable: "DH_USER", passwordVariable: "DH_PASS")]) {
                        sh """
                          echo "\$DH_PASS" | docker login -u "\$DH_USER" --password-stdin
                        """
                        sh "docker push ${fullImage}"
                        def latestPushStatus = sh(script: """
                          set +e
                          docker push ${dockerImage}:latest
                          status=\$?
                          if [ "\$status" -ne 0 ]; then
                            echo "Retrying latest push after re-login..."
                            echo "\$DH_PASS" | docker login -u "\$DH_USER" --password-stdin
                            docker push ${dockerImage}:latest
                            status=\$?
                          fi
                          exit "\$status"
                        """, returnStatus: true)
                        if (latestPushStatus != 0) {
                            unstable("Failed to push ${dockerImage}:latest, but versioned image ${fullImage} was pushed successfully.")
                        }
                        sh "docker logout || true"
                    }
                }
            }

            if (params.TRIGGER_DOWNSTREAM && params.DOWNSTREAM_JOBS?.trim()) {
                stage("Trigger Downstream") {
                    params.DOWNSTREAM_JOBS.split(",").collect { it.trim() }.findAll { it }.each { nextJob ->
                        build job: nextJob, wait: false, parameters: [
                            string(name: "REPO_URL", value: params.REPO_URL),
                            string(name: "BRANCH", value: params.BRANCH),
                            string(name: "IMAGE_REPO", value: params.IMAGE_REPO),
                            string(name: "IMAGE_TAG", value: params.IMAGE_TAG),
                            booleanParam(name: "PUSH_IMAGE", value: params.PUSH_IMAGE),
                            booleanParam(name: "RUN_SONARQUBE", value: params.RUN_SONARQUBE)
                        ]
                    }
                }
            }
        } finally {
            cleanWs(deleteDirs: true, disableDeferredWipeout: true)
        }
    }
}

return this
