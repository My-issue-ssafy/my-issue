pipeline {
  agent any

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build Docker Image') {
      steps {
        sh '''
          echo "=== Docker 이미지 빌드 시작 ==="
          docker build -f backend/my-issue/Dockerfile -t my-app:latest backend/my-issue
          echo "=== 빌드 완료 ==="
        '''
      }
    }
  }
}
