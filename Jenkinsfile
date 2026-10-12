
pipeline {
    agent any

    environment {
        IMAGE_NAME    = 'jenkins-cicd-demo'
        CONTAINER_NAME = 'jenkins-cicd-demo-container'
        STABLE_IMAGE  = 'jenkins-cicd-demo:stable'
        NEW_IMAGE     = "jenkins-cicd-demo:build-${BUILD_NUMBER}"

        JIRA_URL   = 'https://vithiqai-team.atlassian.net'
        JIRA_ISSUE = 'SCRUM-8'
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
                bat 'if not exist target\\jenkins-cicd-demo-1.0-SNAPSHOT.jar exit /b 1'
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
                bat 'docker rm -f %CONTAINER_NAME% 2>nul || exit /b 0'
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

                    try {
                        bat '''
                            powershell -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($i=0; $i -lt 10; $i++){ try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8081/health' -TimeoutSec 3; if($r.StatusCode -eq 200){ Write-Output $r.Content; $ok=$true; break } } catch {}; Start-Sleep -Seconds 3 }; if(-not $ok){ Write-Error 'Health check failed'; exit 1 }"
                        '''

                        echo 'NEW DEPLOYMENT HEALTH CHECK PASSED'
                        echo 'Marking new image as stable'

                        bat 'docker tag %NEW_IMAGE% %STABLE_IMAGE%'

                        echo 'New deployment is now the stable version'

                    } catch (Exception deploymentError) {
                        echo "Deployment health check failed: ${deploymentError.getMessage()}"
                        echo 'Starting automatic rollback'

                        bat 'docker rm -f %CONTAINER_NAME% 2>nul || exit /b 0'

                        def stableExists = bat(
                            script: 'docker image inspect %STABLE_IMAGE% >nul 2>&1',
                            returnStatus: true
                        ) == 0

                        if (stableExists) {
                            bat 'docker run -d --name %CONTAINER_NAME% -p 8081:8080 %STABLE_IMAGE%'

                            def rollbackStatus = bat(
                                script: '''
                                    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; for($i=0; $i -lt 10; $i++){ try { $r=Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8081/health' -TimeoutSec 3; if($r.StatusCode -eq 200){ Write-Output $r.Content; $ok=$true; break } } catch {}; Start-Sleep -Seconds 3 }; if(-not $ok){ exit 1 }"
                                ''',
                                returnStatus: true
                            )

                            if (rollbackStatus != 0) {
                                error('Automatic rollback failed health check')
                            }

                            echo 'Rollback successful; previous stable version is healthy'
                        } else {
                            echo 'No previous stable image is available for rollback'
                        }

                        error('Deployment failed; pipeline marked as FAILURE')
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'PIPELINE COMPLETED SUCCESSFULLY - Updating Jira'

            withCredentials([
                usernamePassword(
                    credentialsId: 'jira-api-jenkins',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )
            ]) {
                bat '''
                    powershell -NoProfile -ExecutionPolicy Bypass -Command "$body = @{body=@{type='doc';version=1;content=@(@{type='paragraph';content=@(@{type='text';text='Jenkins pipeline SUCCESS. Build #%BUILD_NUMBER% completed successfully. Docker deployment and health check passed.'})})}} | ConvertTo-Json -Depth 10 -Compress; $pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($env:JIRA_USER + ':' + $env:JIRA_TOKEN)); $headers = @{Authorization=('Basic ' + $pair); 'Content-Type'='application/json'}; Invoke-RestMethod -Uri ($env:JIRA_URL + '/rest/api/3/issue/' + $env:JIRA_ISSUE + '/comment') -Method Post -Headers $headers -Body $body"
                '''
            }

            echo 'Jira updated successfully'

            script {
                try {
                    slackSend(
                        channel: '#food-fusion',
                        message: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER} - Build, deployment and health check passed. Jira: ${env.JIRA_ISSUE}"
                    )
                    echo 'Slack success notification sent'
                } catch (Exception slackError) {
                    echo "Slack notification failed: ${slackError.getMessage()}"
                }
            }
        }

        failure {
            echo 'PIPELINE FAILED - Updating Jira'

            withCredentials([
                usernamePassword(
                    credentialsId: 'jira-api-jenkins',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )
            ]) {
                bat '''
                    powershell -NoProfile -ExecutionPolicy Bypass -Command "$body = @{body=@{type='doc';version=1;content=@(@{type='paragraph';content=@(@{type='text';text='Jenkins pipeline FAILURE. Build #%BUILD_NUMBER% failed. Check Jenkins Console Output for details.'})})}} | ConvertTo-Json -Depth 10 -Compress; $pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($env:JIRA_USER + ':' + $env:JIRA_TOKEN)); $headers = @{Authorization=('Basic ' + $pair); 'Content-Type'='application/json'}; Invoke-RestMethod -Uri ($env:JIRA_URL + '/rest/api/3/issue/' + $env:JIRA_ISSUE + '/comment') -Method Post -Headers $headers -Body $body"
                '''
            }

            echo 'Jira failure status updated'

            script {
                try {
                    slackSend(
                        channel: '#food-fusion',
                        message: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER} - Pipeline failed. Check Jenkins Console Output. Jira: ${env.JIRA_ISSUE}"
                    )
                    echo 'Slack failure notification sent'
                } catch (Exception slackError) {
                    echo "Slack notification failed: ${slackError.getMessage()}"
                }
            }
        }
    }
}
