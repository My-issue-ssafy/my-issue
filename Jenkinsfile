pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stages('Build Docker Image') {
            steps{
                dir('backend/my-issue'){
                    sh '''
                        echo "=== Docker 이미지 빌드 시작 ==="
                        docker build -t my-app:latest .
                        echo "=== 빌드 완료 ==="
                    '''
                }
            }
        }
    }
}