pipeline {
  agent any
  stages {
    stage('Checkout Stage 1') {
      parallel {
        stage('Checkout cn1-binaries') {
          steps {
            git(url: 'https://github.com/codenameone/cn1-binaries.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout codenameone-skins') {
          steps {
            git(url: 'https://github.com/codenameone/codenameone-skins.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout codenameone-demos') {
          steps {
            git(url: 'https://github.com/codenameone/codenameone-demos.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout KitchenSink') {
          steps {
            git(url: 'https://github.com/codenameone/KitchenSink.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout CameraDemo') {
          steps {
            git(url: 'https://github.com/codenameone/CameraDemo.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout SQLSample') {
          steps {
            git(url: 'https://github.com/codenameone/SQLSample.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout Chrome') {
          steps {
            git(url: 'https://github.com/codenameone/Chrome.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout PheonixUI') {
          steps {
            git(url: 'https://github.com/codenameone/PheonixUI.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout CleanModernUIKit') {
          steps {
            git(url: 'https://github.com/codenameone/CleanModernUIKit.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout DeviceTester') {
          steps {
            git(url: 'https://github.com/codenameone/DeviceTester.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout MaterialScreensUIKit') {
          steps {
            git(url: 'https://github.com/codenameone/MaterialScreensUIKit.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout PsdToAppTutorial') {
          steps {
            git(url: 'https://github.com/codenameone/PsdToAppTutorial.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout SwiftnotesCN1') {
          steps {
            git(url: 'https://github.com/codenameone/SwiftnotesCN1.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout UpdateCodenameOne') {
          steps {
            git(url: 'https://github.com/codenameone/UpdateCodenameOne.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout DrSbaitso') {
          steps {
            git(url: 'https://github.com/codenameone/DrSbaitso.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout PropertyCross') {
          steps {
            git(url: 'https://github.com/codenameone/PropertyCross.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout SocialBoo') {
          steps {
            git(url: 'https://github.com/codenameone/SocialBoo.git', credentialsId: 'codenameone')
          }
        }
      }
    }

    stage('Checkout Stage 2') {
      steps {
        echo 'Checkout Stage 2 reached'
        sh 'ls && mkdir CodenameOne && cd CodenameOne'
      }
    }

    stage('Checkout Stage 3') {
      parallel {
        stage('Checkout BuildClient') {
          steps {
            git(url: 'https://github.com/codenameone/BuildClient.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout NBPlugin') {
          steps {
            git(url: 'https://github.com/codenameone/NBPlugin.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout GUIBuilder') {
          steps {
            git(url: 'https://github.com/codenameone/GUIBuilder.git', credentialsId: 'codenameone')
          }
        }
        stage('Checkout CodenameOneSettings') {
          steps {
            git(url: 'https://github.com/codenameone/CodenameOneSettings.git', credentialsId: 'codenameone')
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
