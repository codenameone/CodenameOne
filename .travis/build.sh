#!/bin/bash

# set -e so that this script will exit if any of the commands fail
set -e

if [ -z "${CN1USER}" ] || [ -z "${CN1PASS}" ]; then
  if [ -n "${CN1_RUNTESTS_ANDROID_EMULATOR}" ] || [ -n "${CN1_RUNTESTS_IOS_SIMULATOR}" ]; then
    echo "Running tests on android or iOS requires the CN1USER and CN1PASS environment variables to be set to your Codename One username and password"
    echo "NOTE: Running tests on iOS and Android requires an enterprise account or higher, since they rely on automated build support"
    exit 1
  fi
fi

if [ "${CN1_PLATFORM}" == "android" ]; then
  echo "Installing Node 6"

  # Need to load NVM command first
  # https://github.com/BanzaiMan/travis_production_test/blob/9c02aef/.travis.yml
  # https://github.com/travis-ci/travis-ci/issues/5999#issuecomment-217201571
  source ~/.nvm/nvm.sh
  nvm install 6
  echo `which node`
  android list targets

  echo "Creating AVD..."
  if [ "${API}" -eq "15" ]; then
    echo no | android create avd --force -n test -t android-15 --abi google_apis/armeabi-v7a
  elif [ "${API}" -eq "16" ]; then
    echo no | android create avd --force -n test -t android-16 --abi armeabi-v7a
  elif [ "${API}" -eq "17" ]; then
    echo no | android create avd --force -n test -t android-17 --abi google_apis/armeabi-v7a
  elif [ "${API}" -eq "18" ]; then
    echo no | android create avd --force -n test -t android-18 --abi google_apis/armeabi-v7a
  elif [ "${API}" -eq "19" ]; then
    echo no | android create avd --force -n test -t android-19 --abi armeabi-v7a
  elif [ "${API}" -eq "21" ]; then
    echo no | android create avd --force -n test -t android-21 --abi armeabi-v7a
  elif [ "${API}" -eq "22" ]; then
    echo no | android create avd --force -n test -t android-22 --abi armeabi-v7a
  fi

  echo "Starting Android Emulator..."
  emulator -avd test -no-window &
  EMULATOR_PID=$!

  # Travis will hang after script completion if we don't kill
  # the emulator
  function stop_emulator() {
    kill $EMULATOR_PID
  }
  trap stop_emulator EXIT
fi
if [ "${CN1_PLATFORM}" == "ios" ]; then
  echo "Installing Ant..."
  # Install ANT and Maven.  They are missing from iOS machines
  curl -L http://archive.apache.org/dist/ant/binaries/apache-ant-1.9.6-bin.tar.gz -o apache-ant-1.9.6-bin.tar.gz
  tar xfz apache-ant-1.9.6-bin.tar.gz --directory ../
  export PATH=`pwd`/../apache-ant-1.9.6/bin:$PATH

  echo "Installing Maven"
  curl -L https://archive.apache.org/dist/maven/maven-3/3.2.3/binaries/apache-maven-3.2.3-bin.tar.gz -o apache-maven-3.2.3-bin.tar.gz
  tar xvfz apache-maven-3.2.3-bin.tar.gz --directory ../
  export PATH=`pwd`/../apache-maven-3.2.3/bin:$PATH
fi

if [ "${CN1_PLATFORM}" == "android" ]; then
  echo "We are in android"
fi

# SET UP ENVIRONMENT
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
PROJECT_DIR=`pwd`
if [ "${CN1_PLATFORM}" == "ios" ]; then
  # On OS X we need to set JAVA_HOME for maven to work properly
  export JAVA_HOME=$(/usr/libexec/java_home)
fi

# Run Tests Against JavaSE
if [[ -n ${CN1_RUNTESTS_JAVASE} ]]; then
  ant test-javase
elif [[ -n ${CN1_RUNTESTS_ANDROID_EMULATOR} ]]; then
  echo "Waiting for Emulator..."
  bash .travis/android-waiting-for-emulator.sh
  adb shell settings put global window_animation_scale 0 &
  adb shell settings put global transition_animation_scale 0 &
  adb shell settings put global animator_duration_scale 0 &

  echo "Sleeping for 30 seconds to give emulator a chance to settle in..."
  sleep 30

  echo "Unlocking emulator"
  adb shell input keyevent 82 &

  echo "Running tests with appium in the emulator "
  ant test-android

elif [[ -n ${CN1_RUNTESTS_IOS_SIMULATOR} ]]; then
  echo "Running tests on IOS SIMULATOR"
  echo "Installing appium..."
  mkdir ./node_modules || true
  npm install appium
  ./node_modules/.bin/appium &
  APPIUM_PID=$!

  # Travis will hang after script completion if we don't kill appium
  function stop_appium() {
    kill $APPIUM_PID
  }
  trap stop_appium EXIT
  ant test-ios
fi
function killjobs () {
    JOBS="$(jobs -p)";
    if [ -n "${JOBS}" ]; then
        kill -KILL ${JOBS};
    fi
}
killjobs
exit 0
