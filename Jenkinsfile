pipeline {
  agent any
  stages {
    stage('Checkout') {
      steps {
        git 'https://github.com/codenameone/cn1-binaries.git'
      }
    }
    stage('Build') {
      steps {
        sh '/usr/local/share/build-cn1.sh'
      }
    }
    stage('Deliver') {
      steps {
        sh '/usr/local/share/upload-jenkins-result.sh'
      }
    }
  }
  post {
    always {
      echo 'Running cleanup'
      deleteDir()

    }

  }
  options {
    checkoutToSubdirectory('cn1')
  }
}
