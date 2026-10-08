pipeline {

    agent any

    environment {
        IMAGE_NAME = 'jenkins-cicd-demo'
        CONTAINER_NAME = 'jenkins-cicd-demo-container'
        STABLE_IMAGE = 'jenkins-cicd-demo:stable'
        NEW_IMAGE = "jenkins-cicd-demo:build-${BUILD_NUMBER}"
        JIRA_URL = 'https://vithiqai-team.atlassian.net'
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

                        echo '========================================'
                        echo 'NEW DEPLOYMENT HEALTH CHECK FAILED'
                        echo 'Starting automatic rollback'
                        echo '========================================'

                        bat '''
                            docker rm -f %CONTAINER_NAME% >nul 2>&1
                            exit /b 0
                        '''

                        bat '''
                            docker image inspect %STABLE_IMAGE% >nul 2>&1
                            if %ERRORLEVEL% EQU 0 (
                                echo Stable image found. Starting rollback.
                            ) else (
                                echo No stable image available for rollback.
                                exit /b 1
                            )
                        '''

                        bat 'docker run -d --name %CONTAINER_NAME% -p 8081:8080 %STABLE_IMAGE%'

                        echo 'Checking health of rolled-back container'

                        def rollbackHealthResult = bat(
                            script: 'curl -f http://localhost:8081/health',
                            returnStatus: true
                        )

                        if (rollbackHealthResult == 0) {

                            echo '========================================'
                            echo 'ROLLBACK COMPLETED SUCCESSFULLY'
                            echo 'Previous stable version is running'
                            echo '========================================'

                        } else {

                            echo '========================================'
                            echo 'ROLLBACK FAILED'
                            echo '========================================'

                            bat '''
                                docker rm -f %CONTAINER_NAME% >nul 2>&1
                                exit /b 0
                            '''

                            error('Rollback failed. No healthy version is running.')
                        }

                        error('Deployment failed. Automatic rollback was triggered.')

                    } else {

                        echo '========================================'
                        echo 'NEW DEPLOYMENT HEALTH CHECK PASSED'
                        echo 'Marking new image as stable'
                        echo '========================================'

                        bat 'docker tag %NEW_IMAGE% %STABLE_IMAGE%'

                        echo 'New deployment is now the stable version'
                    }
                }
            }
        }
    }
    
    post {

    success {
        echo '========================================'
        echo 'PIPELINE COMPLETED SUCCESSFULLY'
        echo 'Updating Jira'
        echo '========================================'

        withCredentials([
            usernamePassword(
                credentialsId: 'jira-api-jenkins',
                usernameVariable: 'JIRA_USER',
                passwordVariable: 'JIRA_TOKEN'
            )
        ]) {

            bat '''
                powershell -NoProfile -Command ^
                "$body = @{body=@{type='doc';version=1;content=@(@{type='paragraph';content=@(@{type='text';text='Jenkins pipeline SUCCESS. Build #%BUILD_NUMBER% completed successfully. Docker deployment and health check passed.'})})}} | ConvertTo-Json -Depth 10; ^
                $pair = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($env:JIRA_USER + ':' + $env:JIRA_TOKEN)); ^
                $headers = @{Authorization='Basic ' + $pair; 'Content-Type'='application/json'}; ^
                Invoke-RestMethod -Uri '%JIRA_URL%/rest/api/3/issue/%JIRA_ISSUE%/comment' -Method Post -Headers $headers -Body $body
            '''

        }

        echo 'Jira updated successfully'
    }

    failure {
        echo '========================================'
        echo 'PIPELINE FAILED'
        echo 'Updating Jira'
        echo '========================================'

        withCredentials([
            usernamePassword(
                credentialsId: 'jira-api-jenkins',
                usernameVariable: 'JIRA_USER',
                passwordVariable: 'JIRA_TOKEN'
            )
        ]) {

            bat '''
                powershell -NoProfile -Command ^
                "$body = @{body=@{type='doc';version=1;content=@(@{type='paragraph';content=@(@{type='text';text='Jenkins pipeline FAILED. Build #%BUILD_NUMBER% failed. Please check the Jenkins console log.'})})}} | ConvertTo-Json -Depth 10; ^
                $pair = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($env:JIRA_USER + ':' + $env:JIRA_TOKEN)); ^
                $headers = @{Authorization='Basic ' + $pair; 'Content-Type'='application/json'}; ^
                Invoke-RestMethod -Uri '%JIRA_URL%/rest/api/3/issue/%JIRA_ISSUE%/comment' -Method Post -Headers $headers -Body $body
            '''

        }

        echo 'Jira updated with pipeline failure'
    }
}
    