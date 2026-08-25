pipeline {
    agent any
    parameters {
        gitParameter(name: 'BRANCH_NAME', type: 'PT_BRANCH', branchFilter: 'origin/(.*)', defaultValue: 'main', selectedValue: 'DEFAULT', description: 'Branch to build')
    }
    tools {
        maven 'Maven3'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(branches: [[name: "${params.BRANCH_NAME}"]], userRemoteConfigs: [[url: 'https://github.com/vmenon04/trading-platform.git']])
            }
        }
        stage('Build Image') {
            steps {
                sh 'mvn -B clean package'
                sh 'docker build -t team-skeleton .'
            }
        }
        stage('Smoke Test') {
            steps {
                sh 'docker run --rm team-skeleton'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Parallel') {
            parallel {           
                stage('Code Coverage') {
                    steps {
                        sh 'mvn test'

                        jacoco(
                            classPattern: '**/target/classes',
                            sourcePattern: '**/src/main/java',
                            execPattern: '**/target/jacoco.exec'
                        )
                    }
                }
                
                stage('Static Analysis') {
                    steps {
                        sh 'mvn site' // this runs everything and makes the webpage
                    }
                }
            }
        }
    }
}