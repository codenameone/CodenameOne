pipeline {
  agent any
  stages {
    stage('Checkout') {
      parallel {
        stage('Checkout cn1-binaries') {
          steps {
            git 'https://github.com/codenameone/cn1-binaries.git'
          }
        }
        stage('Checkout codenameone-skins') {
          steps {
            git 'https://github.com/codenameone/codenameone-skins.git'
          }
        }
        stage('Checkout codenameone-demos') {
          steps {
            git 'https://github.com/codenameone/codenameone-demos.git'
          }
        }
        stage('Checkout KitchenSink') {
          steps {
            git 'https://github.com/codenameone/KitchenSink.git'
          }
        }
        stage('Checkout CameraDemo') {
          steps {
            git 'https://github.com/codenameone/CameraDemo.git'
          }
        }
        stage('Checkout SQLSample') {
          steps {
            git 'https://github.com/codenameone/SQLSample.git'
          }
        }
        stage('Checkout Chrome') {
          steps {
            git 'https://github.com/codenameone/Chrome.git'
          }
        }
        stage('Checkout PheonixUI') {
          steps {
            git 'https://github.com/codenameone/PheonixUI.git'
          }
        }
        stage('Checkout CleanModernUIKit') {
          steps {
            git 'https://github.com/codenameone/CleanModernUIKit.git'
          }
        }
        stage('Checkout DeviceTester') {
          steps {
            git 'https://github.com/codenameone/DeviceTester.git'
          }
        }
        stage('Checkout MaterialScreensUIKit') {
          steps {
            git 'https://github.com/codenameone/MaterialScreensUIKit.git'
          }
        }
        stage('Checkout PsdToAppTutorial') {
          steps {
            git 'https://github.com/codenameone/PsdToAppTutorial.git'
          }
        }
        stage('Checkout SwiftnotesCN1') {
          steps {
            git 'https://github.com/codenameone/SwiftnotesCN1.git'
          }
        }
        stage('Checkout UpdateCodenameOne') {
          steps {
            git 'https://github.com/codenameone/UpdateCodenameOne.git'
          }
        }
        stage('Checkout DrSbaitso') {
          steps {
            git 'https://github.com/codenameone/DrSbaitso.git'
          }
        }
        stage('Checkout PropertyCross') {
          steps {
            git 'https://github.com/codenameone/PropertyCross.git'
          }
        }
        stage('Checkout SocialBoo') {
          steps {
            git 'https://github.com/codenameone/SocialBoo.git'
          }
        }
        stage('Checkout BuildClient') {
          steps {
            sh 'mkdir CodenameOne && cd CodenameOne'
            git 'https://github.com/codenameone/BuildClient.git'
          }
        }
        stage('Checkout NBPlugin') {
          steps {
            sh 'mkdir CodenameOne && cd CodenameOne'
            git 'https://github.com/codenameone/NBPlugin.git'
          }
        }
        stage('Checkout GUIBuilder') {
          steps {
            sh 'mkdir CodenameOne && cd CodenameOne'
            git 'https://github.com/codenameone/GUIBuilder.git'
          }
        }
        stage('Checkout CodenameOneSettings') {
          steps {
            sh 'mkdir CodenameOne && cd CodenameOne'
            git 'https://github.com/codenameone/CodenameOneSettings.git'
            sh 'cd .. && ls'
          }
        }
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
