#!/bin/sh
# Codename One deployment template -- see ../../references/deployment.md.
# Replace myapp with the project's name and app.example.com with the domain. The binary
# is named after the backend module's artifactId: backend/target/myapp-backend here.
# Packages the backend for Linux in a container, from macOS or Windows.
# Usage: build-linux.sh [amd64|arm64]   (default amd64; match the server's CPU)
# A different CPU from this machine's runs emulated and can take most of an hour.
# A backend-only project: drop "-pl backend -Dcodename1.platform=backend".
set -eu
ARCH=${1:-amd64}
ENGINE=$(command -v podman || command -v docker)
"$ENGINE" run --rm --platform linux/$ARCH -v "$PWD":/src -w /src ubuntu:24.04 bash -c '
  apt-get update && apt-get install -y openjdk-17-jdk-headless clang curl unzip \
      libssl-dev libnghttp2-dev libcurl4-openssl-dev &&
  ./mvnw -B -Dmaven.repo.local=/src/.m2-linux \
      -pl backend -Dcodename1.platform=backend cn1:backend-package'
file backend/target/myapp-backend 2>/dev/null || true
