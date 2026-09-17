def CodeCoverage() {
    sh 'mvn test'
    jacoco(
        classPattern: '**/target/classes',
        sourcePattern: '**/src/main/java',
        execPattern: '**/target/jacoco.exec'
    )
}

def SmokeTest() {
    sh 'docker run --rm team-skeleton'
    junit 'target/surefire-reports/*.xml'
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
                sh 'mvn -B clean package'
                script {
                    // here we're sanitizing the branch name
                    // github branch names can contain characters that are not valid in docker tags, so we replace them here with hyphens
                    def safeTag = env.BRANCH_NAME.replaceAll('[^a-zA-Z0-9_.-]', '-') 
                    sh "docker build -t team-skeleton:${safeTag} ."
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