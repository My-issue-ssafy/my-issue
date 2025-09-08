pipeline {
  agent any

  environment { // 전역 환경변수 정의
    IMAGE_REPO = 'xioz19/my-issue' // 빌드/푸시할 Docker 이미지 경로.
    COMMIT_SHA = "${env.GIT_COMMIT?.take(7) ?: 'manual'}" // 이미지에 버전 태그로 붙여서 이력 추적 가능
  }

  options { timestamps() } // Output 로그에 타임스탬프 붙여줌

  stages {
    stage('Checkout') { // GitLab 에서 코드 가져옴
      steps { checkout scm }
      script {
        // 실제 잡에서 보이는 브랜치명 확인용
        echo "BRANCH_NAME=${env.BRANCH_NAME}, GIT_BRANCH=${env.GIT_BRANCH}"
      }
    }

    stage('Docker Build') { // Docker BuildKit 활성화
      steps {
        sh '''
          echo "=== Docker Build (backend/my-issue) ==="
          export DOCKER_BUILDKIT=1
          # 이전 빌드 결과 캐시로 활용
          docker pull ${IMAGE_REPO}:latest || true

          docker build \
            --pull \
            --cache-from=${IMAGE_REPO}:latest \
            -f backend/my-issue/Dockerfile \
            -t ${IMAGE_REPO}:latest \
            -t ${IMAGE_REPO}:${COMMIT_SHA} \
            backend/my-issue
        '''
        // Dockerfile 경로 지정 -> 최신 태그로도 빌드 -> 커밋 SHA 태그로도 빌드
        // Docker 이미지 2개 태그로 빌더 (latest + 커밋 버전)
      }
    }

    stage('Push to Docker Hub') {
      // 원하는 브랜치에서만 푸시 (필요 시 수정)
      when { anyOf { branch 'main'; branch 'dev/server'; branch 'be' } }
      steps {
        withCredentials([usernamePassword( // Jenkins에 등록된 Docker Hub 크리덴셜 사용
          credentialsId: 'dockerhub-cred',
          usernameVariable: 'DOCKER_USER',
          passwordVariable: 'DOCKER_PASS'
        )]) { // Docker Hub 로그인/푸시/로그아웃
          sh '''
            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
            docker push ${IMAGE_REPO}:${COMMIT_SHA}
            docker push ${IMAGE_REPO}:latest
            docker logout || true
          '''
        }
      }
    }
  }

  // 빌드 성공/실패 후 처리
  post {
    success { echo "✅ Pushed: ${IMAGE_REPO}:${COMMIT_SHA} & :latest" }
    always  { sh 'docker image prune -f || true' }
  }
}
