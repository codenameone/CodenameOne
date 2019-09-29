pipeline {
  agent any
  stages {
    stage('Checkout Stage 1') {
      parallel {
        stage('Checkout cn1-binaries') {
          steps {
            dir('cn1-binaries') {
              git(url: 'https://github.com/codenameone/cn1-binaries.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout codenameone-skins') {
          steps {
            dir('codenameone-skins') {
              git(url: 'https://github.com/codenameone/codenameone-skins.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout codenameone-demos') {
          steps {
            dir('codenameone-demos') {
              git(url: 'https://github.com/codenameone/codenameone-demos.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout KitchenSink') {
          steps {
            dir('KitchenSink') {
              git(url: 'https://github.com/codenameone/KitchenSink.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout CameraDemo') {
          steps {
            dir('CameraDemo') {
              git(url: 'https://github.com/codenameone/CameraDemo.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout SQLSample') {
          steps {
            dir('SQLSample') {
              git(url: 'https://github.com/codenameone/SQLSample.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout Chrome') {
          steps {
            dir('Chrome') {
              git(url: 'https://github.com/codenameone/Chrome.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout PheonixUI') {
          steps {
            dir('PheonixUI') {
              git(url: 'https://github.com/codenameone/PheonixUI.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout CleanModernUIKit') {
          steps {
            dir('CleanModernUIKit') {
              git(url: 'https://github.com/codenameone/CleanModernUIKit.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout DeviceTester') {
          steps {
            dir('DeviceTester') {
              git(url: 'https://github.com/codenameone/DeviceTester.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout MaterialScreensUIKit') {
          steps {
            dir('MaterialScreensUIKit') {
              git(url: 'https://github.com/codenameone/MaterialScreensUIKit.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout PsdToAppTutorial') {
          steps {
            dir('PsdToAppTutorial') {
              git(url: 'https://github.com/codenameone/PsdToAppTutorial.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout SwiftnotesCN1') {
          steps {
            dir('SwiftnotesCN1') {
              git(url: 'https://github.com/codenameone/SwiftnotesCN1.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout UpdateCodenameOne') {
          steps {
            dir('UpdateCodenameOne') {
              git(url: 'https://github.com/codenameone/UpdateCodenameOne.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout DrSbaitso') {
          steps {
            dir('DrSbaitso') {
              git(url: 'https://github.com/codenameone/DrSbaitso.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout PropertyCross') {
          steps {
            dir('PropertyCross') {
              git(url: 'https://github.com/codenameone/PropertyCross.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout SocialBoo') {
          steps {
            dir('SocialBoo') {
              git(url: 'https://github.com/codenameone/SocialBoo.git', credentialsId: 'githubcodenameone')
            }
          }
        }
      }
    }

    stage('Checkout Stage 2') {
      steps {
        echo 'Checkout Stage 2 reached'
        sh 'ls && mkdir CodenameOne'
      }
    }

    stage('Checkout Stage 3') {
      parallel {
        stage('Checkout BuildClient') {
          steps {
            dir('CodenameOne/BuildClient') {
              git(url: 'https://github.com/codenameone/BuildClient.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout NBPlugin') {
          steps {
            dir('CodenameOne/NBPlugin') {
              git(url: 'https://github.com/codenameone/NBPlugin.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout GUIBuilder') {
          steps {
            dir('CodenameOne/GUIBuilder') {
              git(url: 'https://github.com/codenameone/GUIBuilder.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout CodenameOneSettings') {
          steps {
            dir('CodenameOne/CodenameOneSettings') {
              git(url: 'https://github.com/codenameone/CodenameOneSettings.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout BuildDaemon') {
          steps {
            dir('CodenameOne/BuildDaemon') {
              git(url: 'https://github.com/codenameone/BuildDaemon.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout BuildDaemonBinaries') {
          steps {
            dir('CodenameOne/BuildDaemonBinaries') {
              git(url: 'https://github.com/codenameone/BuildDaemonBinaries.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout OfflineBuilder') {
          steps {
            dir('CodenameOne/OfflineBuilder') {
              git(url: 'https://github.com/codenameone/OfflineBuilder.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout Eclipse') {
          steps {
            dir('CodenameOne/Eclipse') {
              git(url: 'https://github.com/codenameone/Eclipse.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout Shooter') {
          steps {
            dir('CodenameOne/Shooter') {
              git(url: 'https://github.com/codenameone/Shooter.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout JavaCompatibility') {
          steps {
            dir('CodenameOne/JavaCompatibility') {
              git(url: 'https://github.com/codenameone/JavaCompatibility.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout BuildDaemonDependencies') {
          steps {
            dir('CodenameOne/BuildDaemonDependencies') {
              git(url: 'https://github.com/codenameone/BuildDaemonDependencies.git', credentialsId: 'githubcodenameone')
            }
          }
        }
        stage('Checkout IntelliJGradle') {
          steps {
            dir('CodenameOne/IntelliJCodenameOneSupport') {
              git(url: 'https://github.com/codenameone/IntelliJGradle.git', credentialsId: 'githubcodenameone')
            }
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
