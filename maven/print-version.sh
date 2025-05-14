#!/bin/bash
#Prints the current project version
#
# Created by Steve Hannah
# Creation Date March 19, 2021
# Usage: bash print-version.sh
# Output Example: 7.0.13-SNAPSHOT
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
cd $SCRIPTPATH
regex='CodenameOne (.*):$'

LINE=$(mvn validate | grep "Reactor Summary")
[[ $LINE =~ $regex ]]

echo "${BASH_REMATCH[1]}"
