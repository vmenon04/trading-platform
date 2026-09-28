def CodeCoverage() {
    sh 'mvn test jacoco:report'
    junit 'target/surefire-reports/*.xml'
    archiveArtifacts artifacts: 'target/site/jacoco/**/*', allowEmptyArchive: false
}

def getSafeTag() {
    return env.BRANCH_NAME.replaceAll('[^a-zA-Z0-9_.-]', '-')
}

def SmokeTest() {
    def safeTag = getSafeTag()
    sh "docker run --rm fintech-five:${safeTag}"
}

def StaticAnalysis() {
    sh 'mvn site'
}

pipeline {
    agent any
    tools {
        maven 'Maven3'
    }
    options {
        // by default, jenkins keeps all build logs, so we set a limit here to avoid excessive storage usage.
        // we set it here to only keep the last 20 build logs
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }
    stages {
        stage('Checkout') {
            steps {
                // multibranch jobs already check out the triggering branch, here we just make it explicit.
                checkout scm
            }
        }
        stage('Build Image') {
            steps {
                // skip compiling and running them while building (tests will be run in the Code-Coverage stage)
                sh 'mvn -B clean package -Dmaven.test.skip=true'
                script {
                    // github branch names can contain characters that are not valid in docker tags.
                    def safeTag = getSafeTag()
                    sh "docker build -t fintech-five:${safeTag} ."
                }
            }
        }
        stage('Smoke-Test') {
            steps {
                echo "Starting Library Smoke Test"
                script {
                    SmokeTest()
                }
            }
        }
        stage('Parallel') {
            parallel {           
                stage('Code-Coverage') {
                    steps {
                        echo "Starting Library Code Coverage"
                        script {
                            CodeCoverage()
                        }
                    }
                }
                
                stage('Static-Analysis') {
                    steps {
                        echo "Starting Library Static Analysis"
                        script {
                            StaticAnalysis()
                        }
                    }
                }
            }
        }
    }
}