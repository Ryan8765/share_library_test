/**
    successEmailAddress - email address to send to
    REPOSITORY_BRANCH - The branch of the current deployment (sent via webhook to Jenkins from beanstalk)
    AZURE_CRED_ID - Credential ID for Azure (stored in Jenkins securely)
    RES_GROUP - The resource group for the web app (obtained from Azure)
    WEB_APP - The name of the web app in Azure.
*/
def pipeline(String successEmailAddress, String REPOSITORY_BRANCH, String AZURE_CRED_ID, String RES_GROUP, String WEB_APP, String JOB_NAME, String BUILD_NUMBER, String BUILD_URL ) {
    pipeline {
        agent any
        stages {

            stage('Install Dependenciess') {
                steps {
                    bat 'npm install'
                }
            }

            stage('Build the Project') {
                steps {
                    bat 'npm run build'
                }
            }

            stage('Deploy to Azure Prod') {
                when {
                    environment name: 'REPOSITORY_BRANCH', value: 'master'
                    beforeAgent true
                }
                steps {

                    timeout(time: 5, unit: 'DAYS') {
                        input message: 'Approve Deployment?'
                    }

                    azureWebAppPublish azureCredentialsId: AZURE_CRED_ID,
                        resourceGroup: RES_GROUP,
                        appName: WEB_APP,
                        filePath: "**/*.*",
                        sourceDirectory: "build"
                }
            }

            //Always deploys to "dev" slot - engineering must name this "dev" when setting up the development environment.
            stage('Deploy to Azure Development') {
                when {
                    environment name: 'REPOSITORY_BRANCH', value: 'development'
                    beforeAgent true
                }
                steps {

                    azureWebAppPublish azureCredentialsId: AZURE_CRED_ID,
                        resourceGroup: RES_GROUP,
                        appName: WEB_APP,
                        filePath: "**/*.*",
                        sourceDirectory: "build"
                }
            }

        }

        post {
            always {
                archiveArtifacts artifacts: "build/**/*.*", onlyIfSuccessful: true
                cleanWs()
            }

            //On Success - always send an email to the team involved.
            success {
                mail bcc: '', body: "<b>Example</b><br>Project: ${JOB_NAME} <br>Build Number: ${BUILD_NUMBER} <br> URL de build: ${BUILD_URL}", cc: '', charset: 'UTF-8', from: '', mimeType: 'text/html', replyTo: '', subject: "ERROR CI: Project name -> ${JOB_NAME}", to: successEmailAddress;
            }

        }
    }
}