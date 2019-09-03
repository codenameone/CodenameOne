pipeline {
  agent any
  options {
    checkoutToSubdirectory('cn1')
  }
  stages {
    stage('Checkout') {
      dir('cn1-binaries') {
          git url: 'https://github.com/codenameone/cn1-binaries.git'
      }
      dir('codenameone-skins') {
          git url: 'https://github.com/codenameone/codenameone-skins.git'
      }
      dir('codenameone-demos') {
          git url: 'https://github.com/codenameone/codenameone-demos.git'
      }
      dir('KitchenSink') {
          git url: 'https://github.com/codenameone/KitchenSink.git'
      }
      dir('CameraDemo') {
          git url: 'https://github.com/codenameone/CameraDemo.git'
      }
      dir('SQLSample') {
          git url: 'https://github.com/codenameone/SQLSample.git'
      }
      dir('Chrome') {
          git url: 'https://github.com/codenameone/Chrome.git'
      }
      dir('PheonixUI') {
          git url: 'https://github.com/codenameone/PheonixUI.git'
      }
      dir('CleanModernUIKit') {
          git url: 'https://github.com/codenameone/CleanModernUIKit.git'
      }
      dir('DeviceTester') {
          git url: 'https://github.com/codenameone/DeviceTester.git'
      }
      dir('MaterialScreensUIKit') {
          git url: 'https://github.com/codenameone/MaterialScreensUIKit.git'
      }
      dir('PsdToAppTutorial') {
          git url: 'https://github.com/codenameone/PsdToAppTutorial.git'
      }
      dir('SwiftnotesCN1') {
          git url: 'https://github.com/codenameone/SwiftnotesCN1.git'
      }
      dir('UpdateCodenameOne') {
          git url: 'https://github.com/codenameone/UpdateCodenameOne.git'
      }
      dir('DrSbaitso') {
          git url: 'https://github.com/codenameone/DrSbaitso.git'
      }
      dir('PropertyCross') {
          git url: 'https://github.com/codenameone/PropertyCross.git'
      }
      dir('SocialBoo') {
          git url: 'https://github.com/codenameone/SocialBoo.git'
      }
      dir('CodenameOne/BuildClient') {
          git url: 'https://github.com/codenameone/BuildClient.git'
      }
      dir('CodenameOne/NBPlugin') {
          git url: 'https://github.com/codenameone/NBPlugin.git'
      }
      dir('CodenameOne/GUIBuilder') {
          git url: 'https://github.com/codenameone/GUIBuilder.git'
      }
      dir('CodenameOne/CodenameOneSettings') {
          git url: 'https://github.com/codenameone/CodenameOneSettings.git'
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
      deleteDir() /* clean up our workspace */
    }
  }
}
