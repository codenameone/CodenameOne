#!/bin/sh
# Links the generated C in /src into /out/$CN1_OUT_NAME.
#
# The same script serves both builder images; what differs is CN1_LINK_MODE.
#
#   static  (musl/Alpine)  a binary with no libc at all, which is what runs in a
#                          scratch or distroless image and what makes the
#                          container's size the binary's size.
#   dynamic (glibc/Debian) linked against the distribution's libc and OpenSSL, for
#                          a base image that already carries them and patches them
#                          on its own schedule.
#
# -fwrapv -fno-strict-aliasing -fno-builtin-fmod(f) are MANDATORY for ParparVM's
# generated C (Java wrapping arithmetic; clang -O3 provably miscompiles without
# them). -static-pie is deliberately NOT used for the musl build: musl's static
# PIE and the crash handler's stack introspection disagree about the load base.
set -e
cd /src
OUT_NAME="${CN1_OUT_NAME:-bootstrap}"
COMMON="-O3 -w -fwrapv -fno-strict-aliasing -fno-builtin-fmod -fno-builtin-fmodf"

# CN1_LINK_DEBUG=1 keeps the symbol table and frame pointers so a debugger can
# name what it finds. Without it every backtrace from a deployed binary is a list
# of hex addresses, which is exactly as useful as no backtrace at all.
STRIP="-Wl,--strip-all"
if [ -n "${CN1_LINK_DEBUG:-}" ]; then
    STRIP=""
    COMMON="$COMMON -g -fno-omit-frame-pointer"
fi

# The virtual-thread switch is assembly, so the .S files compile alongside the C.
# Globbing only *.c compiles the C half and fails at link with "undefined symbol:
# cn1VirtualThreadSwitch", which names the symbol but not the reason.
ASM_SOURCES=""
for f in *.S; do
    [ -e "$f" ] && ASM_SOURCES="$ASM_SOURCES $f"
done

if [ "${CN1_LINK_MODE:-static}" = "static" ]; then
    # shellcheck disable=SC2086
    clang $COMMON -static -fuse-ld=lld -I. -I/opt/curlstatic/include \
        ${CN1_EXTRA_CFLAGS} *.c $ASM_SOURCES \
        -L/opt/curlstatic/lib -lcurl -lnghttp2 -lssl -lcrypto -lz -lm -lpthread \
        $STRIP \
        -o "/out/$OUT_NAME"
else
    # shellcheck disable=SC2086
    clang $COMMON -I. ${CN1_EXTRA_CFLAGS} *.c $ASM_SOURCES \
        -lcurl -lnghttp2 -lssl -lcrypto -lz -lm -lpthread \
        $STRIP \
        -o "/out/$OUT_NAME"
fi

echo "arch: $(uname -m)  libc: ${CN1_LINK_MODE:-static}"
ls -l "/out/$OUT_NAME"
# Proof rather than intent: a "static" build that quietly picked up a shared libc
# would run here and fail in a scratch image, which is the worst place to find out.
if [ "${CN1_LINK_MODE:-static}" = "static" ]; then
    if command -v ldd >/dev/null 2>&1 && ldd "/out/$OUT_NAME" 2>&1 | grep -q "=>"; then
        echo "the static build has dynamic dependencies:"
        ldd "/out/$OUT_NAME"
        exit 1
    fi
fi
