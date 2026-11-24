#!/bin/sh
# Codename One deployment template -- see ../../references/deployment.md.
# Replace myapp with the project's name and app.example.com with the domain. The binary
# is named after the backend module's artifactId: backend/target/myapp-backend here.
# Moves current to the release before the one it points at, and deletes the
# release that failed, so nothing that sorts by name picks it again.
ssh deploy@app.example.com '
  cd /opt/myapp
  BAD=$(readlink current)
  PREVIOUS=$(ls -1d releases/* | sed "s#^#/opt/myapp/#" | grep -B1 -x "$BAD" | head -1)
  [ "$PREVIOUS" != "$BAD" ] && ln -sfn "$PREVIOUS" current &&
  sudo systemctl restart myapp && rm -rf "$BAD"'
