pipeline {
  agent any
  stages {
    stage('Build') {
      steps {
        ws(dir: 'cn1build') {
          cleanWs()
        }

        git(url: 'https://github.com/codenameone/CodenameOne.git', branch: 'master')
        withAnt(installation: '/home/builder/apache-ant-1.9.14')
      }
    }
  }
}