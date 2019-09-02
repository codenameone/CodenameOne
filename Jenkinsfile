pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        ws(dir: 'cn1build') {
          sh '/usr/local/share/build-cn1.sh'
        }

      }
    }
  }
}