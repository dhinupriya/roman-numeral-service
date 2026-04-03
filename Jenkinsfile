// ============================================================================
// Jenkins Declarative Pipeline (Bonus Artifact)
//
// Primary CI/CD is GitHub Actions (.github/workflows/ci.yml).
// This Jenkinsfile demonstrates enterprise CI/CD familiarity.
//
// Stages: Checkout → Build → Test → Coverage → Docker Build → Docker Push
// Quality gate: JaCoCo ≥ 80% line coverage
// ============================================================================

pipeline {
    agent any

    tools {
        jdk 'jdk-21'
    }

    environment {
        DOCKER_IMAGE = "roman-numeral-service:${BUILD_NUMBER}"
    }

    stages {
        stage('Build') {
            steps {
                sh './mvnw clean compile -B'
            }
        }

        stage('Test') {
            steps {
                sh './mvnw test -B'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Coverage') {
            steps {
                sh './mvnw verify -B -DskipTests'
                jacoco execPattern: 'target/jacoco.exec',
                       minimumLineCoverage: '80'
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE} ."
            }
        }

        stage('Docker Push') {
            when {
                expression { env.DOCKER_REGISTRY != null }
            }
            steps {
                withCredentials([usernamePassword(
                        credentialsId: 'docker-registry',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                        docker tag ${DOCKER_IMAGE} \${DOCKER_REGISTRY}/${DOCKER_IMAGE}
                        docker push \${DOCKER_REGISTRY}/${DOCKER_IMAGE}
                    """
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/surefire-reports/**', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
            cleanWs()
        }
        failure {
            echo 'Pipeline failed — check test results and coverage reports.'
        }
    }
}
