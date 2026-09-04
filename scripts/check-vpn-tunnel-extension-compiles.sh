#!/bin/bash
# The one real compile of the generated packet-tunnel extension.
#
# IOSVpnTunnelExtensionBuilder writes Objective-C that NOTHING in this
# repository builds -- there is no Objective-C compiler in CI here or in the
# CodenameOne repository, and the file is built by Xcode on a machine none of
# our tests run on. That is how a forward-declaration break, which would have
# failed the generated target's first build, sat in the generator unnoticed:
# C99 removed implicit declarations and current clang makes calling an
# undeclared function an error.
#
# So this generates the provider and runs clang over it against the real iOS
# SDK, with -fapplication-extension -- the flag an .appex target compiles with,
# and the reason the extension may not touch UIApplication.
#
# It SKIPS, successfully, where there is no Xcode -- most CI runners here are
# Linux, so this is a check for a developer's Mac; a skip that failed the build
# would only teach people to ignore it.
#
# Usage: ./.github/scripts/check-vpn-tunnel-extension-compiles.sh
set -e

cd "$(dirname "$0")/.."
ROOT="$(pwd)"

if ! command -v xcrun >/dev/null 2>&1; then
    echo "SKIP: no xcrun on this machine, so there is no iOS SDK to compile"
    echo "      against. The generated extension is checked as text by"
    echo "      VpnTunnelExtensionTest; this is the compile half."
    exit 0
fi

SDK="$(xcrun --sdk iphoneos --show-sdk-path 2>/dev/null || true)"
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
    echo "SKIP: xcrun found no iphoneos SDK."
    exit 0
fi

# cn1_globals.h is the ParparVM runtime header the provider includes, and it
# lives in the CodenameOne repository rather than this one. Look where a
# developer's machine actually keeps it, and skip rather than guess.
GLOBALS="$ROOT/vm/ByteCodeTranslator/src"
if [ ! -f "$GLOBALS/cn1_globals.h" ]; then
    echo "cn1_globals.h is not at $GLOBALS -- the ParparVM runtime header the"
    echo "generated provider includes has moved, and this check cannot run."
    exit 1
fi

JAVA_HOME_8="$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)"
JAVAC="javac"
JAVA="java"
if [ -n "$JAVA_HOME_8" ]; then
    JAVAC="$JAVA_HOME_8/bin/javac"
    JAVA="$JAVA_HOME_8/bin/java"
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# The generator on its own. It depends on nothing but java.util, which is why
# it can be compiled out of the tree like this rather than through the whole
# plugin build.
mkdir -p "$WORK/src/com/codename1/util" "$WORK/classes" "$WORK/out"
cp maven/codenameone-maven-plugin/src/main/java/com/codename1/util/IOSVpnTunnelExtensionBuilder.java \
   "$WORK/src/com/codename1/util/"
cat > "$WORK/src/Emit.java" <<'JAVA'
import com.codename1.util.IOSVpnTunnelExtensionBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

/** Writes the generated extension out so clang can be pointed at it. */
public class Emit {
    public static void main(String[] args) throws Exception {
        Map<String, byte[]> files = IOSVpnTunnelExtensionBuilder.buildFileMap(
                "com.example.app", "Demo", "1.0", "1",
                "com.example.app.MyTunnel");
        File out = new File(args[0]);
        out.mkdirs();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            FileOutputStream fo = new FileOutputStream(new File(out, e.getKey()));
            fo.write(e.getValue());
            fo.close();
        }
    }
}
JAVA
"$JAVAC" -nowarn -d "$WORK/classes" \
    "$WORK/src/com/codename1/util/IOSVpnTunnelExtensionBuilder.java" \
    "$WORK/src/Emit.java"
"$JAVA" -cp "$WORK/classes" Emit "$WORK/out"

# The two translated headers the provider includes. They do not exist until a
# project is translated, so they are stubbed with the exact signatures the
# generated call sites use -- which is itself worth checking: a change to
# either side that these do not match is a link error on a device, and this is
# where it becomes a compile error instead.
cat > "$WORK/out/com_codename1_impl_vpn_ExtensionTunnelHost.h" <<'H'
#include "cn1_globals.h"
extern void com_codename1_impl_vpn_ExtensionTunnelHost_begin___java_lang_Object_java_lang_String(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT, JAVA_OBJECT);
extern JAVA_OBJECT com_codename1_impl_vpn_ExtensionTunnelHost_buffer___int_R_byte_1ARRAY(CODENAME_ONE_THREAD_STATE, JAVA_INT);
extern void com_codename1_impl_vpn_ExtensionTunnelHost_received___int(CODENAME_ONE_THREAD_STATE, JAVA_INT);
extern void com_codename1_impl_vpn_ExtensionTunnelHost_end___int(CODENAME_ONE_THREAD_STATE, JAVA_INT);
H
cat > "$WORK/out/com_codename1_impl_ios_IOSExtensionTunnel.h" <<'H'
#include "cn1_globals.h"
extern void com_codename1_impl_ios_IOSExtensionTunnel_install___int(CODENAME_ONE_THREAD_STATE, JAVA_INT);
H
# cn1_globals.h includes the per-project class index, which only a translation
# produces. Empty is right: nothing the provider writes needs a class id.
: > "$WORK/out/cn1_class_method_index.h"

echo "Compiling the generated packet tunnel provider against $(basename "$SDK")"
clang -fsyntax-only -x objective-c -arch arm64 \
      -isysroot "$SDK" \
      -fapplication-extension \
      -I"$GLOBALS" -I"$WORK/out" \
      "$WORK/out/CN1VpnTunnelProvider.m"

echo "OK: the generated extension compiles as an app extension."
