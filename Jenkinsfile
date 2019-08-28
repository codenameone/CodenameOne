pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        ws(dir: 'cn1build') {
          git(url: 'https://github.com/codenameone/cn1-binaries.git', branch: 'master')
          git(url: 'https://github.com/codenameone/CodenameOne.git', branch: 'master')
          dir(path: 'CodenameOne')
          withAnt(installation: 'ant', jdk: 'JDK 8') {
            sh 'ant release'
          }

          cleanWs()
        }

      }
    }
  }
}