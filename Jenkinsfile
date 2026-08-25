@Library('trading-platform-tests@main') _
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
        stage('Smoke-Test') {
            steps {
                script{
                    echo "Starting Library Smoke Test"
                    Smoke-Test()
                }
            }
        }
        stage('Parallel') {
            parallel {           
                stage('Code-Coverage') {
                    steps {
                        script{
                            echo "Starting Library Code Coverage"
                            Code-Coverage()
                        }
                    }
                }
                
                stage('Static-Analysis') {
                    steps {
                        script{
                            echo "Starting Library Static Analysis"
                            Static-Analysis()
                        }
                    }
                }
            }
        }
    }
}