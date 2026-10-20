#!/bin/bash
# Outbound HTTP/2 and mutual TLS through WebClient, on every runtime.
#
#   tests/web-mtls-h2.sh [javase] [musl] [glibc]     (default: all three)
#
# Starts the fixture servers (tests/mtls/servers.py: an HTTP/1.1-only and an
# HTTP/2-only HTTPS server, both requiring a client certificate from a fixture
# CA) in a container, then runs demo/mtlscheck against them:
#
#   javase  on this machine's JVM, through run-javase.sh
#   musl    the fully static binary package.sh builds, inside the fixture container
#   glibc   the dynamically linked one, likewise -- the fixture image is the
#           glibc builder's own Debian release, so its libcurl is the one linked
#
# Every arm must print MTLSCHECK OK, and no arm's output may contain a password
# or a line of key material. Native builds are for this machine's architecture.
#
# Needs podman or docker, JDK_8_HOME (build.sh) and a JDK for run-javase.sh.
set -e
cd "$(dirname "$0")/.."
ARMS="${*:-javase musl glibc}"
ENGINE="${CN1_BACKEND_ENGINE:-}"
if [ -z "$ENGINE" ]; then
    for candidate in podman docker; do
        if command -v "$candidate" >/dev/null 2>&1; then ENGINE="$candidate"; break; fi
    done
fi
[ -n "$ENGINE" ] || { echo "no container engine found; install podman or docker"; exit 1; }
case "$(uname -m)" in
    arm64|aarch64) ARCH=arm64 ;;
    *) ARCH=x86_64 ;;
esac
H1_PORT="${CN1_MTLS_H1_PORT:-18443}"
H2_PORT="${CN1_MTLS_H2_PORT:-18444}"
WORK="$(pwd)/target/mtls"
rm -rf "$WORK"
mkdir -p "$WORK/creds"
IMAGE=cn1-backend-mtls-fixture
NAME="cn1-mtls-fixture-$$"

"$ENGINE" build -q -t "$IMAGE" -f - . >/dev/null <<'IMG'
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
        python3 python3-h2 python3-cryptography libcurl4 libnghttp2-14 ca-certificates \
 && rm -rf /var/lib/apt/lists/*
IMG

"$ENGINE" run --rm -v "$(pwd)/tests/mtls:/fixture:ro,Z" -v "$WORK/creds:/creds:Z" \
    "$IMAGE" python3 /fixture/credentials.py /creds

cleanup() { "$ENGINE" rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT
"$ENGINE" run -d --name "$NAME" -p "$H1_PORT:$H1_PORT" -p "$H2_PORT:$H2_PORT" \
    -v "$(pwd)/tests/mtls:/fixture:ro,Z" -v "$WORK/creds:/creds:ro,Z" -v "$WORK:/work:Z" \
    "$IMAGE" python3 /fixture/servers.py --dir /creds --h1 "$H1_PORT" --h2 "$H2_PORT" >/dev/null
sleep 2

SECRETS_FOUND=0
check_output() {
    local arm="$1" log="$2"
    cat "$log"
    # No secret in what the program printed: the passwords, and any line of
    # base64 out of a key file.
    if grep -q -e "p12-secret" -e "pem-secret" -e "PRIVATE KEY" "$log" \
       || grep -F -q -f <(grep -v -- "-----" "$WORK/creds/alice.key" | head -3) "$log"; then
        echo "$arm: SECRET MATERIAL IN OUTPUT"
        SECRETS_FOUND=1
    fi
    grep -q "MTLSCHECK OK" "$log"
}

FAILED=""
for arm in $ARMS; do
    echo "==> $arm"
    case "$arm" in
        javase)
            # The JDK run-javase.sh runs, so the trust store is one it reads.
            JHOME="${CN1_BACKEND_JAVA:-${JAVA17_HOME:-}}"
            KEYTOOL=keytool
            if [ -n "$JHOME" ]; then KEYTOOL="$JHOME/bin/keytool"; fi
            rm -f "$WORK/trust.jks"
            "$KEYTOOL" -importcert -noprompt -alias ca -file "$WORK/creds/ca.crt" \
                -keystore "$WORK/trust.jks" -storepass changeit >/dev/null 2>&1
            if CN1_BACKEND_DEMO=demo/mtlscheck CN1_MTLS_ARM=javase \
               CN1_MTLS_H1="https://localhost:$H1_PORT" CN1_MTLS_H2="https://localhost:$H2_PORT" \
               CN1_MTLS_DIR="$WORK/creds" \
               JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=$WORK/trust.jks -Djavax.net.ssl.trustStorePassword=changeit" \
               ./run-javase.sh com.demo.MtlsCheck > "$WORK/javase.log" 2>&1 \
               && check_output javase "$WORK/javase.log"; then :; else
                check_output javase "$WORK/javase.log" || true
                FAILED="$FAILED javase"
            fi
            ;;
        musl|glibc)
            CN1_BACKEND_DEMO=demo/mtlscheck ./package.sh MtlsCheck com.demo "$arm-$ARCH"
            cp "target/dist/mtlscheck-linux-$arm-$ARCH" "$WORK/"
            if "$ENGINE" exec -e CN1_MTLS_ARM=native -e CURL_CA_BUNDLE=/creds/ca.crt \
                   -e CN1_MTLS_H1="https://localhost:$H1_PORT" \
                   -e CN1_MTLS_H2="https://localhost:$H2_PORT" -e CN1_MTLS_DIR=/creds \
                   "$NAME" "/work/mtlscheck-linux-$arm-$ARCH" > "$WORK/$arm.log" 2>&1 \
               && check_output "$arm" "$WORK/$arm.log"; then :; else
                check_output "$arm" "$WORK/$arm.log" || true
                FAILED="$FAILED $arm"
            fi
            ;;
        *) echo "unknown arm $arm"; exit 2 ;;
    esac
done

if [ -n "$FAILED" ] || [ "$SECRETS_FOUND" != 0 ]; then
    echo "WEB MTLS/H2 FAILED:$FAILED"
    exit 1
fi
echo "WEB MTLS/H2 OK ($ARMS)"
