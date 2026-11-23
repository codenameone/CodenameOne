#!/bin/sh
# Codename One deployment template -- see ../../references/deployment.md.
# Replace myapp with the project's name and app.example.com with the domain. The binary
# is named after the backend module's artifactId: backend/target/myapp-backend here.
# deploy.sh -- run from the project root after packaging
set -eu
HOST=deploy@app.example.com
RELEASE=/opt/myapp/releases/$(date +%Y%m%d%H%M%S)
ssh "$HOST" "mkdir -p $RELEASE"
scp backend/target/myapp-backend "$HOST:$RELEASE/myapp"
scp backend/application.properties "$HOST:$RELEASE/"
[ -d backend/target/webapp ] && scp -r backend/target/webapp "$HOST:$RELEASE/"
ssh "$HOST" "ln -sfn $RELEASE /opt/myapp/current &&
             sudo systemctl restart myapp &&
             sleep 2 && curl -fsS https://app.example.com/manage/health/readiness"
