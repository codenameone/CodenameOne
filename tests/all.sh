#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
source $SCRIPTPATH/env.sh
cd $SCRIPTPATH
bash core.sh
bash signin-demo.sh