pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds() }

  environment {
    IMAGE_REPO = 'xioz19/my-issue-py'
    TAG = 'manual'
  }

  triggers {
    gitlab(
      triggerOnPush: true,
      branchFilterType: 'NameBasedFilter',
      includeBranchesSpec: 'dev/data',
      targetBranchRegex: 'dev/data'
    )
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.TAG = sh(returnStdout: true, script: "bash -lc 'git rev-parse --short=7 HEAD'").trim()
          echo "COMMIT_SHA=${env.TAG}"
        }
      }
    }

    stage('Docker Build & Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-cred', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh '''
            bash -Eeuo pipefail -c '
              cd ai/fastapi
              docker build -t ${IMAGE_REPO}:${TAG} -t ${IMAGE_REPO}:latest .
              echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
              docker push ${IMAGE_REPO}:${TAG}
              docker push ${IMAGE_REPO}:latest
              docker logout || true
            '
          '''
        }
      }
    }

    stage('Deploy (Blue/Green)') {
      steps {
        withCredentials([
          sshUserPrivateKey(credentialsId: 'ec2-ssh-key-pem', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER'),
          string(credentialsId: 'NGINX_HOST', variable: 'NGINX_HOST'),
          string(credentialsId: 'DATABASE_URL', variable: 'DATABASE_URL')
        ]) {
          sh '''
            bash -Eeuo pipefail -c '
              echo "🚀 Deploy Python ${IMAGE_REPO}:${TAG}"
              scp -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -i "$SSH_KEY" scripts/deploy_python.sh "$SSH_USER@$NGINX_HOST:~/deploy_python.sh"
              ssh -o IdentitiesOnly=yes -o StrictHostKeyChecking=no -i "$SSH_KEY" "$SSH_USER@$NGINX_HOST" "chmod +x ~/deploy_python.sh && sudo -E ~/deploy_python.sh ${TAG}"
            '
          '''
        }
      }
    }
  }

  post {
    success { echo "✅ Deployed ${IMAGE_REPO}:${TAG}" }
    always  { sh "bash -lc 'docker image prune -f || true'" }
  }
}