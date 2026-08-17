#!/usr/bin/env bash
#
# Pushes a Java program to a running Codename One device runtime.
#
#   scripts/cn1-push.sh <MainClass.java|source-dir> [port] [--main <Class>]
#
# Takes a single file or a whole source tree. A real application is a tree of
# packages whose entry point is a Lifecycle subclass rather than a main, and
# running one of those is the point of this runtime, so both shapes work.
#
# Compiles the sources, packages them as a .cn1ip bundle, and sends it to the
# listener on the device. The bundle carries the source as well as the code:
# the runtime refuses to execute anything whose source it cannot show, because
# that is the condition Apple attaches to running downloaded code at all
# (App Store Review Guideline 2.5.2).
#
# The listener binds loopback only. Reaching it means `adb reverse` on Android
# (which needs an authorised device) or the shared loopback of the iOS
# simulator. There is deliberately no discovery protocol: physical access to the
# device is the pairing.
#
# -XDstringConcat=inline is not optional -- see the note where it is used.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE="${1:?usage: cn1-push.sh <MainClass.java|source-dir> [port] [--main <Class>]}"
shift
PORT=18234
MAIN_OVERRIDE=""
while [ $# -gt 0 ]; do
    case "$1" in
        --main) MAIN_OVERRIDE="$2"; shift 2 ;;
        *) PORT="$1"; shift ;;
    esac
done
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

JDK="${JAVA17_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || true)}"
if [ -z "$JDK" ]; then
    JDK="${JAVA_HOME:-}"
fi
if [ -z "$JDK" ]; then
    echo "no JDK found; set JAVA17_HOME" >&2
    exit 1
fi

# `|| true` because a checkout without .m2-local is the normal case, and `ls`
# failing there would end the script under `set -e` before reaching the
# ~/.m2 fallback two lines down.
CORE_JAR="$(ls "$REPO_ROOT"/.m2-local/com/codenameone/codenameone-core/*/codenameone-core-*.jar 2>/dev/null | head -1 || true)"
if [ -z "$CORE_JAR" ]; then
    CORE_JAR="$(ls "$HOME"/.m2/repository/com/codenameone/codenameone-core/*/codenameone-core-*.jar 2>/dev/null | head -1 || true)"
fi
PARPAR_JAR="$(ls "$REPO_ROOT"/.m2-local/com/codenameone/codenameone-parparvm/*/codenameone-parparvm-*.jar 2>/dev/null | grep -v -- '-sources\|-javadoc\|-bundle' | head -1 || true)"
if [ -z "$PARPAR_JAR" ]; then
    echo "codenameone-parparvm not found in .m2-local; build it first:" >&2
    echo "  mvn -pl parparvm install -f maven/pom.xml" >&2
    exit 1
fi
ASM_JARS="$(find "$REPO_ROOT/.m2-local/org/ow2/asm" "$HOME/.m2/repository/org/ow2/asm" -name 'asm*-9.8.jar' 2>/dev/null | tr '\n' ':')"

echo "compiling $SOURCE"
mkdir -p "$WORK/classes"
if [ -d "$SOURCE" ]; then
    SOURCE_ROOT="$SOURCE"
    find "$SOURCE" -name '*.java' > "$WORK/sources.txt"
    if [ ! -s "$WORK/sources.txt" ]; then
        echo "no .java files under $SOURCE" >&2
        exit 1
    fi
else
    # The file itself, not its directory: a directory would sweep in every
    # other program sitting beside it.
    SOURCE_ROOT="$SOURCE"
    echo "$SOURCE" > "$WORK/sources.txt"
fi
# The device has no runtime invokedynamic: ParparVM desugars it at build time
# and a pushed bundle gets no such pass. From JDK 9 javac turns "a" + b into an
# indy against StringConcatFactory, so it has to be compiled the old way.
"$JDK/bin/javac" -g -nowarn -XDstringConcat=inline \
    -cp "$CORE_JAR" -d "$WORK/classes" @"$WORK/sources.txt"

echo "building bundle"
cat > "$WORK/Pack.java" <<'JAVA'
import com.codename1.tools.translator.InterpBundleWriter;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class Pack {
    public static void main(String[] a) throws Exception {
        File classesDir = new File(a[0]);
        String mainClass = a[1];
        File sourceRoot = new File(a[2]);
        File out = new File(a[3]);

        InterpBundleWriter w = new InterpBundleWriter();
        List<File> classes = new ArrayList<File>();
        collect(classesDir, classes);
        for (File f : classes) {
            w.addClassFile(f);
        }
        // Every source in the tree, not just the entry point's: the runtime
        // refuses to run a class whose source it cannot show, and a real
        // application is many files.
        if (sourceRoot.isDirectory()) {
            w.addSourceTree(sourceRoot);
            // Everything that is not source: theme.res, CSS, images. Keyed by
            // path relative to the tree, which is how an application loads them.
            w.addResourceTree(sourceRoot);
        } else {
            // Keyed by the declared package, exactly as addSourceTree does:
            // the reader looks a class's source up as <package>/<SourceFile>,
            // so a lone com.example.Main stored under "Main.java" is a bundle
            // the device refuses as missing its own source.
            String text = new String(Files.readAllBytes(sourceRoot.toPath()), "UTF-8");
            w.addSource(InterpBundleWriter.sourceKey(
                    InterpBundleWriter.packageOf(text), sourceRoot.getName()), text);
        }
        if (mainClass.length() == 0) {
            mainClass = findEntryPoint(classesDir, classes);
            System.out.println("entry point " + mainClass.replace('/', '.'));
        }
        w.setMainClass(mainClass);
        OutputStream os = new FileOutputStream(out);
        try {
            w.write(os);
        } finally {
            os.close();
        }
        System.out.println("bundle " + out.length() + " bytes, " + classes.size() + " classes");
    }

    /// The class to enter: a main(String[]) if there is one, otherwise a
    /// Lifecycle subclass, which is what a real application has.
    private static String findEntryPoint(File root, List<File> classes) throws Exception {
        String lifecycle = null;
        for (File f : classes) {
            org.objectweb.asm.tree.ClassNode cn = new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(Files.readAllBytes(f.toPath()))
                    .accept(cn, org.objectweb.asm.ClassReader.SKIP_CODE);
            for (Object mo : cn.methods) {
                org.objectweb.asm.tree.MethodNode m = (org.objectweb.asm.tree.MethodNode) mo;
                if ("main".equals(m.name) && "([Ljava/lang/String;)V".equals(m.desc)
                        && (m.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0) {
                    return cn.name;
                }
            }
            if ("com/codename1/system/Lifecycle".equals(cn.superName)) {
                lifecycle = cn.name;
            }
        }
        if (lifecycle != null) {
            return lifecycle;
        }
        throw new IllegalStateException(
                "no entry point: expected a main(String[]) or a Lifecycle subclass");
    }

    private static void collect(File dir, List<File> out) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        for (File f : kids) {
            if (f.isDirectory()) collect(f, out);
            else if (f.getName().endsWith(".class")) out.add(f);
        }
    }
}
JAVA
"$JDK/bin/javac" -nowarn -cp "$PARPAR_JAR:$ASM_JARS" -d "$WORK" "$WORK/Pack.java"
"$JDK/bin/java" -cp "$WORK:$PARPAR_JAR:$ASM_JARS" Pack \
    "$WORK/classes" "$MAIN_OVERRIDE" "$SOURCE_ROOT" "$WORK/program.cn1ip"

echo "awaiting the device on 127.0.0.1:$PORT"
cat > "$WORK/Push.java" <<'JAVA'
import java.io.*;
import java.net.*;
import java.nio.file.*;

/**
 * Sends a bundle to a device runtime over loopback.
 *
 * Unauthenticated on purpose, and only over loopback: reaching this listener
 * means `adb reverse` on a USB-authorised device or the iOS simulator's own
 * loopback, so possession of the device is the authentication, and going
 * through a pairing dialog for every push during framework work would be pure
 * friction. The device refuses this protocol on any connection that did not
 * arrive over loopback.
 *
 * A push to a phone over Wi-Fi is a different thing and belongs to a different
 * tool: DevicePush pairs, derives a shared secret and answers a challenge on
 * every connection. This helper deliberately does not, rather than carrying a
 * third copy of the crypto that would drift from the other two.
 */
public final class Push {
    private static final int MAGIC = 0x434E3150;   // "CN1P"
    private static final int V1 = 1;

    public static void main(String[] a) throws Exception {
        byte[] payload = Files.readAllBytes(Paths.get(a[0]));
        send(Integer.parseInt(a[1]), payload);
    }

    /**
     * Waits for the device to dial in, then sends the frame.
     *
     * The desktop listens and the device connects out, not the other way round.
     * A socket the device listens on is unreachable from the host inside the
     * iOS simulator -- the app binds it and reports success while every
     * connection attempt is refused -- and a phone on a real network cannot
     * accept inbound connections at all. The device retries every couple of
     * seconds, so starting this first is all the synchronisation needed.
     */
    private static void send(int port, byte[] payload) throws Exception {
        ServerSocket server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
        server.setSoTimeout(120000);
        Socket s;
        try {
            System.out.println("waiting for the device to connect on 127.0.0.1:" + port);
            s = server.accept();
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("FAILED: the device never connected. Is the app running, "
                    + "and is `adb reverse tcp:" + port + " tcp:" + port + "` set on Android?");
            System.exit(1);
            return;
        } finally {
            server.close();
        }
        try {
            s.setSoTimeout(120000);
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.writeInt(MAGIC);
            out.writeInt(V1);
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
            if (!report(s)) {
                System.exit(1);
            }
        } finally {
            s.close();
        }
    }

    private static boolean report(Socket s) throws IOException {
        DataInputStream in = new DataInputStream(s.getInputStream());
        int ok = in.readByte();
        String message = in.readUTF();
        System.out.println((ok == 1 ? "OK: " : "FAILED: ") + message);
        return ok == 1;
    }
}
JAVA
"$JDK/bin/javac" -nowarn -d "$WORK" "$WORK/Push.java"
"$JDK/bin/java" -cp "$WORK" Push "$WORK/program.cn1ip" "$PORT"
