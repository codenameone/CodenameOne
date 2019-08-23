pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        ws(dir: 'cn1build') {
          git(url: 'https://github.com/codenameone/CodenameOne.git', branch: 'master')
          withAnt() {
            sh 'ant jar'
          }

          cleanWs()
        }

      }
    }
  }
}