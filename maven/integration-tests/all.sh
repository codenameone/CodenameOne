#!/bin/bash
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
CN1_VERSION=7.0.11-SNAPSHOT
bash cn1app-archetype-test.sh
bash bare-bones-kotlin-test.sh