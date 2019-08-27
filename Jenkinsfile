pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        ws(dir: 'cn1build') {
          git(url: 'https://github.com/codenameone/CodenameOne.git', branch: 'master')
          withAnt(installation: 'ant', jdk: 'JDK 8') {
            sh 'ant jar'
          }

          cleanWs()
        }

      }
    }
  }
}