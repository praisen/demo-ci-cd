pipeline {
  agent any

  environment {
    DOCKERHUB_CRED = 'dockerhub'           // Create this in Jenkins Credentials
    DOCKER_IMAGE = "praisen/demo-ci-cd:${BUILD_NUMBER}"
    // If using SonarQube server configured in Jenkins as 'sonarqube':
    SONARQUBE_SERVER = 'sonarqube'         // Manage Jenkins > Configure System
    SONAR_PROJECT_KEY = 'demo'
  }

  options { timestamps() }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Unit Tests') {
  steps { sh 'mvn -B -DskipTests=false clean verify -Ddependency-check.skip=true' }
  post { always { junit 'target/surefire-reports/*.xml' } }
}


    stage('Code Quality: SonarQube') {
      steps {
        withSonarQubeEnv("${SONARQUBE_SERVER}") {
          sh '''
            mvn -B sonar:sonar               -Dsonar.projectKey=${SONAR_PROJECT_KEY}               -Dsonar.host.url=$SONAR_HOST_URL               -Dsonar.token=$SONAR_AUTH_TOKEN
          '''
        }
      }
    }

    stage('Security: Trivy FS Scan') {
  steps {
    sh '''
      # Run Trivy scan and output JSON report into Jenkins workspace
      docker run --rm -v $(pwd):/workspace aquasec/trivy:latest fs \
        --exit-code 0 \
        --severity HIGH,CRITICAL \
        --format json \
        --output /workspace/trivy-report.json \
        /workspace

      # Debug: check file exists in Jenkins workspace
      ls -lh $(pwd)/trivy-report.json
    '''
  }
  post {
    always {
      archiveArtifacts artifacts: 'trivy-report.json', fingerprint: true
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

    stage('Image Scan: Trivy') {
      steps {
        // fails pipeline if HIGH/CRITICAL vulnerabilities found
        sh 'docker run --rm aquasec/trivy:latest image --exit-code 1 --severity HIGH,CRITICAL ${DOCKER_IMAGE}'
      }
    }

    stage('Deploy to Docker') {
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
