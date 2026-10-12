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

def ReportingTests() {
    // the reporting tests are Python: install requirements.txt into a virtual environment, then run pytest.
    // runs inside the python:3.14 container (see the Reporting-Tests stage); the venv lives in the workspace
    // because the container runs as the Jenkins user, which can't install packages system-wide.
    sh 'python3 -m venv .venv-reporting'
    sh '.venv-reporting/bin/pip install -q -r requirements.txt'
    try {
        sh 'cd reporting && ../.venv-reporting/bin/python -m pytest -v --junitxml=pytest-results.xml'
    } finally {
        // publish the results even when tests fail, so Jenkins shows which ones
        junit allowEmptyResults: true, testResults: 'reporting/pytest-results.xml'
    }
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

                stage('Reporting-Tests') {
                    // run this stage inside the official Python image, so the agent doesn't need Python installed.
                    // reuseNode keeps it on the same machine and workspace as the other stages.
                    agent {
                        docker {
                            image 'python:3.14'
                            reuseNode true
                        }
                    }
                    steps {
                        echo "Starting Reporting (Python) Tests"
                        script {
                            ReportingTests()
                        }
                    }
                }
            }
        }
    }
}