pipeline {
  agent any

  environment { // 전역 환경변수 정의
    IMAGE_REPO = 'xioz19/my-issue' // 빌드/푸시할 Docker 이미지 경로.
    COMMIT_SHA = 'manual' // 이미지에 버전 태그로 붙여서 이력 추적 가능
    DB_URL = credentials('my-db-url')  // DB 접속 정보도 Jenkins에 등록된 보안값 사용
    DB_USERNAME = credentials('my-db-username')  // Jenkins에 등록된 보안값
    DB_PASSWORD = credentials('my-db-password') // Jenkins에 등록된 보안값
  }

  options {
    timestamps()
    gitLabConnection('my-issue')
  } // Output 로그에 타임스탬프 붙여줌

  // MR/Push 이벤트 수신
  triggers {
    gitlab (
      triggerOnPush: true,

      // 브랜치 필터
      branchFilterType: 'NameBasedFilter',
      includeBranchesSpec: 'dev/server',  // push 이벤트: 이 브랜치만

      // dev/server 브랜치에 대해서만 빌드 트리거
      targetBranchRegex: 'dev/server'
    )
  }

  stages {
    stage('Checkout') { // GitLab 에서 코드 가져옴
      steps {
        checkout scm
        script {
          // 브랜치/커밋 정보 세팅 및 로깅
          env.COMMIT_SHA = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
          echo "BRANCH_NAME=${env.BRANCH_NAME}, GIT_BRANCH=${env.GIT_BRANCH}, COMMIT_SHA=${env.COMMIT_SHA}"
        }
      }
    }

    stage('Build & Test') {
      steps {
        gitlabCommitStatus(name: 'jenkins-ci') {
          dir('backend/my-issue') {
            sh './gradlew clean build'
          }
        }
      }
    }

    stage('Docker Build') { // Docker BuildKit 활성화
      when {
        expression { env.BRANCH_NAME == 'dev/server' || env.GIT_BRANCH == 'origin/dev/server' }
      }
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

    stage('Deploy') {
      when {
        expression { env.BRANCH_NAME == 'dev/server' || env.GIT_BRANCH == 'origin/dev/server' }
      }
      steps {
        sh '''
          echo "🚀 Start Deploying ${IMAGE_REPO}:${COMMIT_SHA}"

          chmod +x ./scripts/deploy.sh
          ./scripts/deploy.sh ${COMMIT_SHA} 8081
        '''
        }
    }
  }

  // 빌드 성공/실패 후 처리
  post {
    success {
        echo "✅ Pushed: ${IMAGE_REPO}:${COMMIT_SHA} & :latest"
        updateGitlabCommitStatus name: 'jenkins-ci', state: 'success'
    }
    failure {
        updateGitlabCommitStatus name: 'jenkins-ci', state: 'failed'
    }
    always  { sh 'docker image prune -f || true' }
  }
}
