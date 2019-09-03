pipeline {
  agent any
  stages {
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
      deleteDir() /* clean up our workspace */
    }
  }
}
