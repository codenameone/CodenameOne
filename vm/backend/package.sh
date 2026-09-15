#!/bin/bash
# Builds one backend program for every Linux deployment target.
#
#   package.sh <MainSimpleClassName> <mainPackage> [target...]
#
# Targets are <libc>-<arch>: musl-x86_64, musl-arm64, glibc-x86_64, glibc-arm64.
# With none named it builds all four. Output lands in target/dist/.
#
# The translation runs ONCE. ParparVM emits portable C, so what differs between
# targets is only the compile -- which is why this is four clang invocations over
# one source tree rather than four builds.
#
# Why both libcs:
#   musl   a fully static binary: no libc, no OpenSSL, nothing. It runs in a
#          scratch or distroless image, so the container is the binary and there
#          is no base image to patch. This is the microservice shape.
#   glibc  linked against the distribution's libc and OpenSSL, for an
#          organisation whose base image already carries them and patches them on
#          its own schedule.
#
# Cross-architecture builds go through qemu (podman/docker emulate the other
# arch), which works and is slow. On a build machine, prefer a native runner per
# architecture and name one target.
#
# Environment knobs:
#   CN1_BACKEND_DEMO      demo source dir (default demo/petserver)
#   CN1_BACKEND_ENGINE    podman or docker (default: whichever is on PATH)
#   CN1_BACKEND_SQLITE=0  leave the SQLite engine out
#   CN1_BACKEND_HTTPS=0   leave TLS and outbound HTTP out
set -e
cd "$(dirname "$0")"
MAIN="${1:?usage: package.sh <MainSimpleClassName> <mainPackage> [target...]}"; shift
PKG="${1:?usage: package.sh <MainSimpleClassName> <mainPackage> [target...]}"; shift
TARGETS="$*"
if [ -z "$TARGETS" ]; then
    TARGETS="musl-x86_64 musl-arm64 glibc-x86_64 glibc-arm64"
fi

ENGINE="${CN1_BACKEND_ENGINE:-}"
if [ -z "$ENGINE" ]; then
    for candidate in podman docker; do
        if command -v "$candidate" >/dev/null 2>&1; then ENGINE="$candidate"; break; fi
    done
fi
[ -n "$ENGINE" ] || { echo "no container engine found; install podman or docker"; exit 1; }

SRC="$(pwd)/target/csrc-$MAIN"
DIST="$(pwd)/target/dist"
mkdir -p "$DIST"

# One translation for every target.
CN1_BACKEND_SRC_OUT="$SRC" ./build.sh "$MAIN" "$PKG" unused
# build.sh derives the -D flags that go with the switches it was given and leaves
# them beside the sources; the container link runs in its own process and would
# otherwise link a source tree it has not been told about.
DERIVED_CFLAGS=""
if [ -f "$SRC/cn1-cflags.txt" ]; then
    DERIVED_CFLAGS="$(cat "$SRC/cn1-cflags.txt")"
    rm -f "$SRC/cn1-cflags.txt"
fi

lower() { echo "$1" | tr 'A-Z' 'a-z'; }

for target in $TARGETS; do
    libc="${target%%-*}"
    arch="${target#*-}"
    case "$libc" in
        musl|glibc) ;;
        *) echo "unknown libc in target '$target' (expected musl or glibc)"; exit 1 ;;
    esac
    case "$arch" in
        x86_64) platform="linux/amd64" ;;
        arm64)  platform="linux/arm64" ;;
        *) echo "unknown architecture in target '$target' (expected x86_64 or arm64)"; exit 1 ;;
    esac

    image="cn1-backend-$libc-$arch"
    echo "==> building the $libc/$arch builder image"
    "$ENGINE" build --platform "$platform" -t "$image" \
        -f "docker/Containerfile.$libc" docker

    out_name="$(lower "$MAIN")-linux-$libc-$arch"
    # Removed first: a failed link would otherwise leave the PREVIOUS binary in
    # place, and a stale artifact that looks fresh is worse than no artifact.
    rm -f "$DIST/$out_name"
    echo "==> linking $out_name"
    "$ENGINE" run --rm --platform "$platform" \
        -v "$SRC:/src:ro,Z" -v "$DIST:/out:Z" \
        -e "CN1_OUT_NAME=$out_name" \
        -e "CN1_EXTRA_CFLAGS=$DERIVED_CFLAGS $CN1_BACKEND_CFLAGS" \
        -e "CN1_LINK_DEBUG=${CN1_LINK_DEBUG:-}" \
        "$image"
done

echo
echo "built:"
ls -l "$DIST" | tail -n +2 | sed 's/^/  /'
