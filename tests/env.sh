#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
if [ -z $CN1_VERSION]; then
  CN1_VERSION=$(bash $SCRIPTPATH/../maven/print-version.sh)
fi

if [ ! -d $SCRIPTPATH/build ]; then
  mkdir $SCRIPTPATH/build
fi