#!/bin/bash
if [[ -z "${GH_TOKEN}" ]]; then
  echo "No GH_TOKEN environment variable found.  Skipping triggered builds"
  exit 0
fi
if [[ -z "${CN1_TRIGGER_REPOS}" ]]; then
  echo "No CN1_TRIGGER_REPOS environment variable found.  Skipping triggered builds"
  exit 0
fi
travis login --github-token=$GH_TOKEN --org
travis token > /dev/null
#travis whoami
mkdir repos
cd repos
for i in $(echo ${CN1_TRIGGER_REPOS} | tr " " "\n")
do
  rm -rf repo
  # process
  git clone https://github.com/${i} repo
  cd repo
  travis restart
  cd ..
  rm -rf repo
done
