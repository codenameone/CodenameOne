#!/bin/sh
# Codename One deployment template -- see ../../references/deployment.md.
# Replace myapp with the project's name and app.example.com with the domain. The binary
# is named after the backend module's artifactId: backend/target/myapp-backend here.
set -eu
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
