#!/bin/bash
set -e
sh update_version.sh $1
mvn deploy