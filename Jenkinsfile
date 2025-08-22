pipeline {
  agent any

  environment {
    DOCKERHUB_CRED = 'dockerhub'           
    DOCKER_IMAGE = "praisen/demo-ci-cd:${BUILD_NUMBER}"
    SONARQUBE_SERVER = 'sonarqube'         
    SONAR_PROJECT_KEY = 'demo'
  }

  options { timestamps() }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Unit Tests') {
      steps { 
        sh 'mvn -B -DskipTests=false clean verify -Ddependency-check.skip=true' 
      }
      post { 
        always { junit 'target/surefire-reports/*.xml' } 
      }
    }

    stage('Code Quality: SonarQube') {
      steps {
        withSonarQubeEnv("${SONARQUBE_SERVER}") {
          withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_AUTH_TOKEN')]) {
            sh '''
              mvn -B sonar:sonar \
                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                -Dsonar.host.url=$SONAR_HOST_URL \
                -Dsonar.login=$SONAR_AUTH_TOKEN
            '''
          }
        }
      }
    }

    stage('Security: Trivy FS Scan') {
      steps {
        sh '''
          echo "Running Trivy FS scan..."
          docker run --rm -v ${WORKSPACE}:/workspace aquasec/trivy:latest fs \
            --exit-code 0 \
            --severity HIGH,CRITICAL \
            --format json \
            --output /workspace/trivy-fs-report.json \
            /workspace || true
        '''
      }
      post {
        always {
          archiveArtifacts artifacts: 'trivy-fs-report.json', fingerprint: true, allowEmptyArchive: true
        }
      }
    }

    stage('Docker Build & Push') {
      steps {
        sh 'docker build -t ${DOCKER_IMAGE} .'
        withCredentials([usernamePassword(credentialsId: "${DOCKERHUB_CRED}", usernameVariable: 'USER', passwordVariable: 'PASS')]) {
          sh 'echo $PASS | docker login -u $USER --password-stdin'
          sh 'docker push ${DOCKER_IMAGE}'
        }
      }
    }

    stage('Security: Trivy Scan') {
  steps {
    sh '''
      echo "Running Trivy image scan..."
      trivy image --quiet --format json -o trivy-image-report.json ${DOCKER_IMAGE} || true
    '''
  }
  post {
    always {
      script {
        if (fileExists('trivy-image-report.json')) {
          archiveArtifacts artifacts: 'trivy-image-report.json', allowEmptyArchive: true
          echo "Trivy report archived."
        } else {
          echo "⚠️ No Trivy report generated, skipping archive."
        }
      }
    }
  }
}


    stage('Deploy to Docker') {
      when {
        expression { currentBuild.resultIsBetterOrEqualTo('UNSTABLE') }
      }
      steps {
        sh '''
          docker rm -f demo || true
          docker run -d --name demo -p 80:8080 ${DOCKER_IMAGE}
        '''
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
    }
  }
}
