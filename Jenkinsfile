#!groovy

/*
 * Licensed to CloudNetService under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
                junit testResults: './server/target/surefire-reports/*.xml', allowEmptyResults: true
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package'
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: './launcher/target/CloudNet-UpdateServer.jar'
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
