pipeline {
  agent any
  stages {
    stage('Checkout Stage 1') {
      parallel {
        stage('Checkout cn1-binaries') {
          steps {
            git(url: 'https://github.com/codenameone/cn1-binaries.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout codenameone-skins') {
          steps {
            git(url: 'https://github.com/codenameone/codenameone-skins.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout codenameone-demos') {
          steps {
            git(url: 'https://github.com/codenameone/codenameone-demos.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout KitchenSink') {
          steps {
            git(url: 'https://github.com/codenameone/KitchenSink.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout CameraDemo') {
          steps {
            git(url: 'https://github.com/codenameone/CameraDemo.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout SQLSample') {
          steps {
            git(url: 'https://github.com/codenameone/SQLSample.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout Chrome') {
          steps {
            git(url: 'https://github.com/codenameone/Chrome.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout PheonixUI') {
          steps {
            git(url: 'https://github.com/codenameone/PheonixUI.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout CleanModernUIKit') {
          steps {
            git(url: 'https://github.com/codenameone/CleanModernUIKit.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout DeviceTester') {
          steps {
            git(url: 'https://github.com/codenameone/DeviceTester.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout MaterialScreensUIKit') {
          steps {
            git(url: 'https://github.com/codenameone/MaterialScreensUIKit.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout PsdToAppTutorial') {
          steps {
            git(url: 'https://github.com/codenameone/PsdToAppTutorial.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout SwiftnotesCN1') {
          steps {
            git(url: 'https://github.com/codenameone/SwiftnotesCN1.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout UpdateCodenameOne') {
          steps {
            git(url: 'https://github.com/codenameone/UpdateCodenameOne.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout DrSbaitso') {
          steps {
            git(url: 'https://github.com/codenameone/DrSbaitso.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout PropertyCross') {
          steps {
            git(url: 'https://github.com/codenameone/PropertyCross.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout SocialBoo') {
          steps {
            git(url: 'https://github.com/codenameone/SocialBoo.git', credentialsId: 'githubcodenameone')
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
            git(url: 'https://github.com/codenameone/BuildClient.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout NBPlugin') {
          steps {
            git(url: 'https://github.com/codenameone/NBPlugin.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout GUIBuilder') {
          steps {
            git(url: 'https://github.com/codenameone/GUIBuilder.git', credentialsId: 'githubcodenameone')
          }
        }
        stage('Checkout CodenameOneSettings') {
          steps {
            git(url: 'https://github.com/codenameone/CodenameOneSettings.git', credentialsId: 'githubcodenameone')
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
