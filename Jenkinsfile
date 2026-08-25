@Library('trading-platform-tests@latest') _
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
                echo "Starting Library Smoke Test"
            }
        }
        stage('Parallel') {
            parallel {           
                stage('Code-Coverage') {
                    steps {
                        echo "Starting Library Code Coverage"
                    }
                }
                
                stage('Static-Analysis') {
                    steps {
                        echo "Starting Library Static Analysis"
                    }
                }
            }
        }
    }
}