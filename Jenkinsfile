// @Library('trading-platform-tests@main') _
pipeline {
    agent any
    tools {
        maven 'Maven3'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build Image') {
            steps {
                sh 'mvn -B clean package'
                sh 'docker build -t team-skeleton .'
            }
        }
//         stage('Smoke-Test') {
//             steps {
//                 echo "Starting Library Smoke Test"
//                 SmokeTest()
//             }
//         }
//         stage('Parallel') {
//             parallel {
//                 stage('Code-Coverage') {
//                     steps {
//                         echo "Starting Library Code Coverage"
//                         CodeCoverage()
//                     }
//                 }
//
//                 stage('Static-Analysis') {
//                     steps {
//                         echo "Starting Library Static Analysis"
//                         StaticAnalysis()
//                     }
//                 }
//             }
//         }
    }
}