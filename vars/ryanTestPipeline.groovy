// vars/evenOrOdd.groovy
def pipeline(String successEmailAddress) {
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

                    azureWebAppPublish azureCredentialsId: env.AZURE_CRED_ID,
                        resourceGroup: env.RES_GROUP,
                        appName: env.WEB_APP,
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

                    azureWebAppPublish azureCredentialsId: env.AZURE_CRED_ID,
                        resourceGroup: env.RES_GROUP,
                        appName: env.WEB_APP,
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
                mail bcc: '', body: "<b>Example</b><br>Project: ${env.JOB_NAME} <br>Build Number: ${env.BUILD_NUMBER} <br> URL de build: ${env.BUILD_URL}", cc: '', charset: 'UTF-8', from: '', mimeType: 'text/html', replyTo: '', subject: "ERROR CI: Project name -> ${env.JOB_NAME}", to: successEmailAddress;
            }

        }
    }
}