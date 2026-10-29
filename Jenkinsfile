
pipeline {
    agent any

    environment {
        IMAGE_NAME = 'jenkins-cicd-demo'
        CONTAINER_NAME = 'jenkins-cicd-demo-container'
        STABLE_IMAGE = 'jenkins-cicd-demo:stable'
        NEW_IMAGE = "jenkins-cicd-demo:build-${BUILD_NUMBER}"

        JIRA_URL = 'https://vithiqai-team.atlassian.net'
        JIRA_ISSUE = "${params.JIRA_ISSUE_KEY}"

        SLACK_CHANNEL = 'C0BUJDDTNBF'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code from GitHub'
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                echo 'Running Maven build and unit tests'
                bat 'call mvn clean package'
            }
        }

        stage('Verify JAR') {
            steps {
                echo 'Checking generated JAR file'
                bat 'dir target\\*.jar'
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image'
                bat 'docker build -t %NEW_IMAGE% .'
            }
        }

        stage('Prepare Stable Image') {
            steps {
                echo 'Checking for previous stable image'
                bat '''
                    docker image inspect %STABLE_IMAGE% >nul 2>&1
                    if %ERRORLEVEL% EQU 0 (
                        echo Previous stable image exists
                    ) else (
                        echo No previous stable image exists - first deployment
                    )
                    exit /b 0
                '''
            }
        }

        stage('Remove Old Container') {
            steps {
                echo 'Removing currently running container'
                bat '''
                    docker rm -f %CONTAINER_NAME% >nul 2>&1
                    exit /b 0
                '''
            }
        }

        stage('Run New Container') {
            steps {
                echo 'Starting new Docker container'
                bat 'docker run -d --name %CONTAINER_NAME% -p 8081:8080 %NEW_IMAGE%'
            }
        }

        stage('Health Check') {
            steps {
                script {
                    echo 'Checking application health'

                    def healthResult = bat(
                        script: 'curl -f http://localhost:8081/health',
                        returnStatus: true
                    )

                    if (healthResult != 0) {
                        echo 'New deployment health check failed. Starting rollback.'

                        bat '''
                            docker rm -f %CONTAINER_NAME% >nul 2>&1
                            exit /b 0
                        '''

                        def stableExists = bat(
                            script: 'docker image inspect %STABLE_IMAGE% >nul 2>&1',
                            returnStatus: true
                        )

                        if (stableExists != 0) {
                            error('Deployment failed and no stable image is available for rollback.')
                        }

                        bat 'docker run -d --name %CONTAINER_NAME% -p 8081:8080 %STABLE_IMAGE%'

                        def rollbackHealthResult = bat(
                            script: 'curl -f http://localhost:8081/health',
                            returnStatus: true
                        )

                        if (rollbackHealthResult != 0) {
                            bat '''
                                docker rm -f %CONTAINER_NAME% >nul 2>&1
                                exit /b 0
                            '''
                            error('Rollback failed. No healthy version is running.')
                        }

                        echo 'Rollback completed. Previous stable version is running.'
                        error('Deployment failed. Automatic rollback restored the previous version.')
                    } else {
                        echo 'New deployment health check passed.'
                        echo 'Marking new image as stable.'
                        bat 'docker tag %NEW_IMAGE% %STABLE_IMAGE%'
                        echo 'New deployment is now the stable version.'
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully.'

            // Slack notification
            script {
                try {
                    slackSend(
                        channel: env.SLACK_CHANNEL,
                        color: 'good',
                        message: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n" +
                                 "Deployment and health check passed.\n" +
                                 "Jira: ${env.JIRA_ISSUE}\n" +
                                 "Build: ${env.BUILD_URL}"
                    )
                } catch (err) {
                    echo "Slack success notification failed: ${err}"
                }
            }

            // Jira comment
            script {
                try {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'jira-api-jenkins',
                            usernameVariable: 'JIRA_USER',
                            passwordVariable: 'JIRA_TOKEN'
                        )
                    ]) {
                        bat '''
                            powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $body=@{body=@{type='doc';version=1;content=@(@{type='paragraph';content=@(@{type='text';text='Jenkins pipeline SUCCESS. Build #%BUILD_NUMBER% completed successfully. Docker deployment and health check passed.'})})}} | ConvertTo-Json -Depth 10 -Compress; $pair=[Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($env:JIRA_USER + ':' + $env:JIRA_TOKEN)); $headers=@{Authorization=('Basic ' + $pair); 'Content-Type'='application/json'}; Invoke-RestMethod -Uri '%JIRA_URL%/rest/api/3/issue/%JIRA_ISSUE%/comment' -Method Post -Headers $headers -Body $body"
                        '''
                    }
                    echo 'Jira success comment added.'
                } catch (err) {
                    echo "Jira success comment failed: ${err}"
                }
            }
        }

        failure {
            echo 'Pipeline failed. Check the Jenkins console output.'

            // Slack failure notification
            script {
                try {
                    slackSend(
                        channel: env.SLACK_CHANNEL,
                        color: 'danger',
                        message: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n" +
                                 "Please check the Jenkins console.\n" +
                                 "Jira: ${env.JIRA_ISSUE}\n" +
                                 "Build: ${env.BUILD_URL}"
                    )
                } catch (err) {
                    echo "Slack failure notification failed: ${err}"
                }
            }

            // Jira comment
            script {
                try {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'jira-api-jenkins',
                            usernameVariable: 'JIRA_USER',
                            passwordVariable: 'JIRA_TOKEN'
                        )
                    ]) {
                        bat '''
                            powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $body=@{body=@{type='doc';version=1;content=@(@{type='paragraph';content=@(@{type='text';text='Jenkins pipeline FAILED. Build #%BUILD_NUMBER% failed. Please check the Jenkins console log.'})})}} | ConvertTo-Json -Depth 10 -Compress; $pair=[Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($env:JIRA_USER + ':' + $env:JIRA_TOKEN)); $headers=@{Authorization=('Basic ' + $pair); 'Content-Type'='application/json'}; Invoke-RestMethod -Uri '%JIRA_URL%/rest/api/3/issue/%JIRA_ISSUE%/comment' -Method Post -Headers $headers -Body $body"
                        '''
                    }
                    echo 'Jira failure comment added.'
                } catch (err) {
                    echo "Jira failure comment failed: ${err}"
                }
            }
        }

        always {
            echo "Pipeline finished with result: ${currentBuild.currentResult}"
        }
    }
}
