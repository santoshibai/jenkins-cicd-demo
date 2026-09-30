pipeline {

    agent any

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
                bat 'docker build -t jenkins-cicd-demo:1.0 .'
            }
        }

        stage('Remove Old Container') {
            steps {
                echo 'Removing old container if it exists'
                bat 'docker rm -f jenkins-cicd-demo-container >nul 2>&1 || exit /b 0'
            }
        }

        stage('Run New Container') {
            steps {
                echo 'Starting new Docker container'
                bat 'docker run --name jenkins-cicd-demo-container jenkins-cicd-demo:1.0'
            }
        }

        stage('Verify Deployment') {
            steps {
                echo 'Checking Docker container status'
                bat 'docker ps -a'
            }
        }
    }

    post {

        success {
            echo '========================================'
            echo 'PIPELINE COMPLETED SUCCESSFULLY'
            echo '========================================'
        }

        failure {
            echo '========================================'
            echo 'PIPELINE FAILED'
            echo '========================================'
        }
    }
}