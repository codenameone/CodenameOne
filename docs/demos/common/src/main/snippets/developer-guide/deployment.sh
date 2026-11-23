// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::deploy-build-on-linux[]
# Ubuntu or Debian, on the same release as the server (see below)
sudo apt-get install -y openjdk-17-jdk-headless clang \
    libssl-dev libnghttp2-dev libcurl4-openssl-dev
./mvnw -pl backend -Dcodename1.platform=backend cn1:backend-package   # app with backend
./mvnw cn1:backend-package                                           # backend-only project
// end::deploy-build-on-linux[]

// tag::deploy-build-in-container[]
# From the project root on a Mac or Windows machine. --platform picks the
# server's CPU: linux/amd64 for most VPS plans, linux/arm64 for Graviton or
# Ampere machines. A different CPU from yours runs emulated and is slow.
podman run --rm --platform linux/amd64 -v "$PWD":/src -w /src ubuntu:24.04 bash -c '
  apt-get update && apt-get install -y openjdk-17-jdk-headless clang curl unzip \
      libssl-dev libnghttp2-dev libcurl4-openssl-dev &&
  ./mvnw -B -Dmaven.repo.local=/src/.m2-linux \
      -pl backend -Dcodename1.platform=backend cn1:backend-package'
// end::deploy-build-in-container[]

// tag::deploy-webapp[]
./mvnw -pl backend -Dcodename1.platform=backend cn1:backend-webapp   # stages backend/target/webapp
// end::deploy-webapp[]

// tag::deploy-server-setup[]
# Once, as root on a fresh Ubuntu server
apt-get update && apt-get upgrade -y
apt-get install -y libssl3t64 libnghttp2-14 libcurl4t64 caddy
useradd --system --no-create-home --shell /usr/sbin/nologin myapp   # runs the server
useradd --create-home --shell /bin/bash deploy                      # copies releases
usermod -aG systemd-journal deploy                                  # may read the logs
mkdir -p /home/deploy/.ssh /opt/myapp/releases /etc/myapp
cp ~/.ssh/authorized_keys /home/deploy/.ssh/ && chown -R deploy:deploy /home/deploy/.ssh
chown -R deploy:deploy /opt/myapp
echo 'deploy ALL=(root) NOPASSWD: /usr/bin/systemctl restart myapp' > /etc/sudoers.d/myapp
ufw allow OpenSSH && ufw allow 80,443/tcp && ufw --force enable
// end::deploy-server-setup[]

// tag::deploy-ship[]
#!/bin/sh
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
// end::deploy-ship[]

// tag::deploy-rollback[]
# Moves current to the release before the one it points at, and deletes the
# release that failed, so nothing that sorts by name picks it again.
ssh deploy@app.example.com '
  cd /opt/myapp
  BAD=$(readlink current)
  PREVIOUS=$(ls -1d releases/* | sed "s#^#/opt/myapp/#" | grep -B1 -x "$BAD" | head -1)
  [ "$PREVIOUS" != "$BAD" ] && ln -sfn "$PREVIOUS" current &&
  sudo systemctl restart myapp && rm -rf "$BAD"'
// end::deploy-rollback[]

// tag::deploy-check[]
journalctl -u myapp -f                     # the server's output
curl -fsS https://app.example.com/manage/health/readiness
// end::deploy-check[]
