#!/usr/bin/env bash
# The backend's enterprise sign-in against real servers: OpenLDAP (plain,
# LDAPS and StartTLS, verified against a CA made here) and Keycloak as a SAML
# 2.0 identity provider (signed and encrypted assertions, Single Logout).
#
# Needs podman or docker. Starts both containers, runs the *E2ETest classes
# of maven/backend with the settings they read, and removes the containers
# again whatever happens. MAVEN_REPO_LOCAL defaults to the checkout's
# maven/.m2-repo; codenameone-backend's dependencies must already be there.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
MAVEN_DIR="$(cd "$HERE/../.." && pwd)"
ENGINE="${CONTAINER_ENGINE:-$(command -v podman || command -v docker)}"
REPO="${MAVEN_REPO_LOCAL:-$MAVEN_DIR/.m2-repo}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cn1-identity-e2e.XXXXXX")"
LDAP_NAME="cn1-identity-e2e-ldap-$$"
KC_NAME="cn1-identity-e2e-keycloak-$$"
LDAP_PORT="${CN1_E2E_LDAP_PORT:-13389}"
LDAPS_PORT="${CN1_E2E_LDAPS_PORT:-13636}"
KC_PORT="${CN1_E2E_KEYCLOAK_PORT:-18080}"

cleanup() {
  "$ENGINE" rm -f "$LDAP_NAME" "$KC_NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# A CA and a server certificate for localhost, so TLS is verified, not skipped.
mkdir -p "$WORK/certs"
openssl req -x509 -newkey rsa:2048 -nodes -days 2 -subj "/CN=cn1 e2e CA" \
  -keyout "$WORK/certs/ca.key" -out "$WORK/certs/ca.pem" 2>/dev/null
openssl req -newkey rsa:2048 -nodes -subj "/CN=localhost" \
  -keyout "$WORK/certs/server.key" -out "$WORK/certs/server.csr" 2>/dev/null
printf 'subjectAltName=DNS:localhost,IP:127.0.0.1\n' > "$WORK/certs/san.ext"
openssl x509 -req -in "$WORK/certs/server.csr" -CA "$WORK/certs/ca.pem" \
  -CAkey "$WORK/certs/ca.key" -CAcreateserial -days 2 -extfile "$WORK/certs/san.ext" \
  -out "$WORK/certs/server.pem" 2>/dev/null
chmod 644 "$WORK/certs/"*

"$ENGINE" run -d --name "$LDAP_NAME" -p "$LDAP_PORT:389" -p "$LDAPS_PORT:636" \
  -v "$WORK/certs:/certs:ro" -v "$HERE:/seed:ro" docker.io/library/alpine:3.20 sh -c '
    apk add --no-cache openldap openldap-back-mdb openldap-clients >/dev/null &&
    mkdir -p /run/openldap /var/lib/openldap/openldap-data &&
    slapadd -f /seed/slapd.conf -l /seed/directory.ldif &&
    exec slapd -h "ldap:/// ldaps:///" -f /seed/slapd.conf -d 0' >/dev/null

# HotSpot dies of SIGILL on its SVE code paths in a Linux VM on recent Apple
# silicon; the flag exists only on aarch64 builds, so only there.
KC_JAVA=""
case "$(uname -m)" in arm64|aarch64) KC_JAVA="-XX:UseSVE=0" ;; esac
"$ENGINE" run -d --name "$KC_NAME" -p "$KC_PORT:8080" -e JAVA_OPTS_APPEND="$KC_JAVA" \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:26.0.7 start-dev >/dev/null

for i in $(seq 1 120); do
  if "$ENGINE" exec "$LDAP_NAME" ldapsearch -x -H ldap://localhost -b dc=corp,dc=example \
      -s base >/dev/null 2>&1; then break; fi
  sleep 1
done
for i in $(seq 1 180); do
  if curl -fs "http://localhost:$KC_PORT/realms/master" >/dev/null 2>&1; then break; fi
  sleep 1
done

export CN1_E2E_LDAP_URL="ldap://localhost:$LDAP_PORT"
export CN1_E2E_LDAPS_URL="ldaps://localhost:$LDAPS_PORT"
export CN1_E2E_LDAP_CA="$WORK/certs/ca.pem"
export CN1_E2E_KEYCLOAK_URL="http://localhost:$KC_PORT"
cd "$MAVEN_DIR"
# Offline by default, for a checkout whose repository is already filled; CI
# sets CN1_E2E_MVN_OFFLINE= because -DskipTests never resolved surefire's
# JUnit provider there.
mvn -B ${CN1_E2E_MVN_OFFLINE--o} -pl backend test -Dtest='*E2ETest' -Dsurefire.failIfNoSpecifiedTests=false \
  -Dmaven.repo.local="$REPO" "$@"
