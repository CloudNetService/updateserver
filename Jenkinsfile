pipeline {
    agent any
    tools {
        maven 'Maven3'
        jdk 'Java11'
    }
    options {
        buildDiscarder logRotator(numToKeepStr: '10')
    }
    stages {
        stage('Clean') {
            steps {
                sh 'mvn clean'
            }
        }
        stage('Compile') {
            steps {
                sh 'mvn compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
                junit testResults: './target/surefire-reports/*.xml', allowEmptyResults: true
            }
        }
        stage('Package') {
            steps {
                sh 'mvn package'
            }
        }
    }
    post {
        always {
            withCredentials([string(credentialsId: 'cloudnet-discord-ci-webhook', variable: 'url')]) {
                discordSend description: 'New build for CloudNet-UpdateServer!', footer: 'New build!', link: env.BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: JOB_NAME, webhookURL: url
            }
        }
    }

}