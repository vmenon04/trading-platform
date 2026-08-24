pipeline {
    agent any
    parameters {
        gitParameter(name: 'GIT_BRANCH', type: 'PT_BRANCH', defaultValue: 'main', selectedValue: 'DEFAULT', description: 'Branch to build')
    }
    tools {
        maven 'Maven3'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scmGit(branches: [[name: "${params.GIT_BRANCH}"]])
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