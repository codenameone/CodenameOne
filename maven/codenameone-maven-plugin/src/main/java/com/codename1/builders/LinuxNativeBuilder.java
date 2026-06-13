/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.builders;

import org.apache.tools.ant.BuildException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Native Linux desktop builder. Produces a standalone ELF executable (GTK3 +
 * Cairo/Pango/GdkPixbuf rendering, no JVM) from a Codename One app by running
 * ParparVM's "linux" clean C target and compiling the result with CMake + Ninja.
 * This is the Linux analog of {@link WindowsNativeBuilder}; the JVM-bundled
 * desktop path remains the separate {@code linux-desktop} (javase) target.
 *
 * <p>Build flow (mirrors the Windows pipeline):</p>
 * <ol>
 *   <li>Unzip the app into classes/res/built-in-res.</li>
 *   <li>Extract the bundled LinuxPort layer (translated platform classes + the C
 *       {@code nativeSources}).</li>
 *   <li>Run the ParparVM translator with the {@code linux} app type, which emits a
 *       CMake project that links the GTK3/Cairo/Pango/GdkPixbuf/libcurl stack
 *       (and, when present, GStreamer/WebKitGTK/libsecret/libnotify) via
 *       pkg-config and globs the port's native sources.</li>
 *   <li>Configure + build that CMake project with the selected C compiler for the
 *       target architecture.</li>
 *   <li>Collect the executable into the result directory.</li>
 * </ol>
 *
 * <p><b>musl.</b> The lean-libc goal is met by the compiler the build uses: on a
 * musl host (e.g. Alpine) the native {@code cc}/{@code clang} already links musl,
 * and pkg-config resolves the musl-built GTK stack. To target musl from a glibc
 * host, set {@code linux.toolchain=zig} (or point {@code linux.cc} / {@code
 * CN1_CC} at a {@code zig cc}-style wrapper) -- {@link #targetTriple(String)}
 * selects the {@code *-linux-musl} triple. The GTK stack stays dynamically linked
 * either way (it cannot be statically linked); only the translated runtime + app C
 * is musl-bound.</p>
 *
 * <p><b>Architecture.</b> {@code linux.arch} selects the target: {@code x64}
 * (x86-64, the default) or {@code arm64}.</p>
 *
 * <p>The native compile/link runs only on Linux (it needs the GTK dev stack +
 * pkg-config). The pure build orchestration (arch resolution, translator
 * invocation, CMake argument assembly) is platform independent and unit tested.</p>
 */
public class LinuxNativeBuilder extends Executor {
    /** Supported target architectures for {@code linux.arch}. */
    public static final String ARCH_X64 = "x64";
    public static final String ARCH_ARM64 = "arm64";

    private File resultDir;
    private File linuxExecutable;

    /** The produced native executable, or {@code null} if the build failed. */
    public File getLinuxExecutable() {
        return linuxExecutable;
    }

    /** The directory holding the build output (executable + any side files). */
    public File getResultDir() {
        return resultDir;
    }

    @Override
    protected String getDeviceIdCode() {
        return "\"\"";
    }

    // The generated XxxImplCodenameOne class carries the native methods; a
    // PeerComponent-returning native interface method is bridged through a
    // long[] holding the native widget handle (the GtkWidget* pointer), exactly
    // as the Windows/iOS builders do. This is the form the translated native peer
    // code and PeerComponent.create(Object) understand.
    @Override
    protected String getImplSuffix() {
        return "ImplCodenameOne";
    }

    @Override
    protected String generatePeerComponentCreationCode(String methodCallString) {
        return "PeerComponent.create(new long[] {" + methodCallString + "})";
    }

    @Override
    protected String convertPeerComponentToNative(String param) {
        return "((long[])" + param + ".getNativePeer())[0]";
    }

    /**
     * Normalises a {@code linux.arch} hint to one of {@link #ARCH_X64} /
     * {@link #ARCH_ARM64}. Accepts common synonyms (x86_64, amd64, aarch64).
     */
    public static String normalizeArch(String arch) {
        if (arch == null) {
            return ARCH_X64;
        }
        String a = arch.trim().toLowerCase();
        if (a.equals("arm64") || a.equals("aarch64")) {
            return ARCH_ARM64;
        }
        if (a.equals("x64") || a.equals("x86_64") || a.equals("amd64") || a.equals("x86-64")) {
            return ARCH_X64;
        }
        // Unknown values fall back to x64 rather than failing the build.
        return ARCH_X64;
    }

    /**
     * LLVM/zig target triple for the given (normalised) architecture. Always the
     * {@code *-linux-musl} triple -- this builder produces musl binaries; the GTK
     * stack is dynamically linked on top.
     */
    public static String targetTriple(String arch) {
        if (ARCH_ARM64.equals(normalizeArch(arch))) {
            return "aarch64-linux-musl";
        }
        return "x86_64-linux-musl";
    }

    /** Best-effort detection of the build host architecture. */
    static String detectHostArch() {
        String a = System.getProperty("os.arch", "").toLowerCase();
        if (a.contains("aarch64") || a.contains("arm64")) {
            return ARCH_ARM64;
        }
        return ARCH_X64;
    }

    @Override
    public boolean build(File sourceZip, BuildRequest request) throws BuildException {
        String arch = normalizeArch(request.getArg("linux.arch", ARCH_X64));
        log("Building native Linux app for arch=" + arch + " (triple " + targetTriple(arch) + ")");

        File tmpFile = getBuildDirectory();
        tmpFile.mkdirs();
        File classesDir = new File(tmpFile, "classes");
        File resDir = new File(tmpFile, "res");
        File buildinRes = new File(tmpFile, "btres");
        classesDir.mkdirs();
        resDir.mkdirs();
        buildinRes.mkdirs();

        try {
            unzip(sourceZip, classesDir, resDir, resDir, buildinRes);
        } catch (Exception ex) {
            throw new BuildException("Failed to unzip the application sources", ex);
        }

        // ParparVM translator + the LinuxPort native layer (translated platform
        // classes and the C nativeSources) are bundled with the build.
        File parparVMCompilerJar;
        File portClasses;
        File nativeSources;
        try {
            parparVMCompilerJar = getResourceAsFile("/parparvm-compiler.jar", ".jar");
            File portDir = new File(tmpFile, "linuxPort");
            portClasses = new File(portDir, "classes");
            nativeSources = new File(portDir, "nativeSources");
            portClasses.mkdirs();
            nativeSources.mkdirs();
            // Provided on the plugin classpath by the codenameone-linux 'bundle'
            // artifact (LinuxPort.jar = platform classes, nativelinux.jar = C
            // sources), the same mechanism parparvm-compiler.jar uses.
            extractJarResource("/LinuxPort.jar", portClasses);
            extractJarResource("/nativelinux.jar", nativeSources);
        } catch (Exception ex) {
            throw new BuildException("Failed to stage the LinuxPort native layer. The codenameone "
                    + "maven plugin must provide the codenameone-linux 'bundle' artifact "
                    + "(LinuxPort.jar + nativelinux.jar) on its classpath.", ex);
        }

        // Native interface binding + app bootstrap (see WindowsNativeBuilder for the
        // full description of the three generated stub kinds). All generated stubs
        // are compiled into classesDir (a translator source root) so the executable
        // links the app, the port and the native bindings.
        try {
            generateNativeInterfaceAndBootstrapStubs(request, classesDir, portClasses);
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to generate native interface / bootstrap stubs", ex);
        }

        String version = request.getVersion() != null ? request.getVersion() : "1.0";
        File translatedOut = new File(tmpFile, "translated");
        translatedOut.mkdirs();

        // The "linux" app type makes the translator emit a standalone executable
        // CMake project (add_executable + the GTK/Cairo pkg-config link set) and
        // copy the port's nativeSources into srcRoot, binding
        // CodenameOneImplementation to its @Concrete linux() target
        // (LinuxImplementation) during translation.
        List<String> parparCmd = new ArrayList<String>();
        parparCmd.add("java");
        parparCmd.add("-Xmx768m");
        parparCmd.add("-jar");
        parparCmd.add(parparVMCompilerJar.getAbsolutePath());
        parparCmd.add("clean");
        // ';'-separated source roots: app classes, the LinuxPort platform classes,
        // resources, built-in resources, and the native sources (copied verbatim
        // into the generated srcRoot).
        parparCmd.add(join(";", classesDir, portClasses, resDir, buildinRes, nativeSources));
        parparCmd.add(translatedOut.getAbsolutePath());
        parparCmd.add(request.getMainClass());
        parparCmd.add(request.getPackageName());
        parparCmd.add(request.getDisplayName());
        parparCmd.add(version);
        parparCmd.add("linux"); // project type
        parparCmd.add("none");  // additional native frameworks (none on Linux)
        try {
            if (!exec(tmpFile, 600000, parparCmd.toArray(new String[0]))) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failure while running the ParparVM translator (linux target)", ex);
        }

        File cmakeRoot = new File(translatedOut, "dist");
        File cmakeLists = new File(cmakeRoot, "CMakeLists.txt");
        if (!cmakeLists.exists()) {
            throw new BuildException("Translator did not emit a CMake project at " + cmakeLists.getAbsolutePath());
        }

        File buildDir = new File(cmakeRoot, "build");
        buildDir.mkdirs();

        // Optimized + stripped Release by default (smallest self-contained binary).
        // linux.debug=true keeps the symbols (RelWithDebInfo) so a native crash
        // address can be symbolized during development.
        boolean debugSymbols = "true".equalsIgnoreCase(request.getArg("linux.debug", "false"));
        String buildType = debugSymbols ? "RelWithDebInfo" : "Release";

        String cc = resolveCCompiler(request, arch);

        List<String> configure = new ArrayList<String>();
        configure.add("cmake");
        configure.add("-S");
        configure.add(cmakeRoot.getAbsolutePath());
        configure.add("-B");
        configure.add(buildDir.getAbsolutePath());
        configure.add("-DCMAKE_BUILD_TYPE=" + buildType);
        configure.add("-G");
        configure.add("Ninja");
        configure.add("-DCMAKE_C_COMPILER=" + cc);

        try {
            if (!exec(cmakeRoot, 1800000, configure.toArray(new String[0]))) {
                return false;
            }
            List<String> buildCmd = new ArrayList<String>();
            buildCmd.add("cmake");
            buildCmd.add("--build");
            buildCmd.add(buildDir.getAbsolutePath());
            if (!exec(cmakeRoot, 1800000, buildCmd.toArray(new String[0]))) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Native Linux build (CMake/Ninja) failed", ex);
        }

        File built = new File(buildDir, request.getMainClass());
        if (!built.exists()) {
            throw new BuildException("Expected executable not produced: " + built.getAbsolutePath());
        }
        resultDir = new File(tmpFile, "result");
        resultDir.mkdirs();
        linuxExecutable = new File(resultDir, request.getMainClass());
        try {
            copy(built, linuxExecutable);
            linuxExecutable.setExecutable(true);
        } catch (Exception ex) {
            throw new BuildException("Failed to collect the built executable", ex);
        }
        log("Native Linux executable: " + linuxExecutable.getAbsolutePath() + " (" + arch + ")");
        return true;
    }

    /**
     * Resolves the C compiler CMake should use. Precedence:
     * <ol>
     *   <li>{@code linux.cc} build hint, else the {@code CN1_CC} environment
     *       variable -- used verbatim (point it at a {@code zig cc} wrapper, a
     *       {@code musl-gcc}, or any cross {@code cc}).</li>
     *   <li>{@code linux.toolchain=zig}: generate a tiny wrapper that runs
     *       {@code zig cc -target <musl-triple>}, so a glibc host can emit musl
     *       binaries for the selected arch (the {@code zig} executable is taken
     *       from {@code CN1_ZIG}, else {@code zig} on PATH).</li>
     *   <li>Otherwise the host's default {@code cc} -- which on a musl host
     *       (Alpine) already links musl.</li>
     * </ol>
     */
    private String resolveCCompiler(BuildRequest request, String arch) {
        String hint = request.getArg("linux.cc", null);
        if (hint == null || hint.isEmpty()) {
            hint = System.getenv("CN1_CC");
        }
        if (hint != null && !hint.isEmpty()) {
            return hint;
        }
        if ("zig".equalsIgnoreCase(request.getArg("linux.toolchain", "")) || "true".equalsIgnoreCase(request.getArg("linux.musl", "false"))) {
            return writeZigCcWrapper(arch).getAbsolutePath();
        }
        return "cc";
    }

    /**
     * Writes an executable wrapper script that invokes {@code zig cc -target
     * <musl-triple>} for the selected arch, so CMake can treat it as a single C
     * compiler. {@code zig} is a self-contained cross-compiler shipping the musl
     * sysroot, which is the simplest way to produce musl binaries from any host.
     */
    private File writeZigCcWrapper(String arch) {
        String zig = System.getenv("CN1_ZIG");
        if (zig == null || zig.isEmpty()) {
            zig = "zig";
        }
        String triple = targetTriple(arch);
        File wrapper = new File(getBuildDirectory(), "zig-cc-" + normalizeArch(arch));
        try {
            StringBuilder sh = new StringBuilder();
            sh.append("#!/bin/sh\n");
            sh.append("exec \"").append(zig).append("\" cc -target ").append(triple).append(" \"$@\"\n");
            try (OutputStream out = new FileOutputStream(wrapper)) {
                out.write(sh.toString().getBytes(StandardCharsets.UTF_8));
            }
            wrapper.setExecutable(true);
        } catch (Exception ex) {
            throw new BuildException("Failed to write the zig cc wrapper", ex);
        }
        return wrapper;
    }

    /**
     * Scans the app classes for native interfaces and generates the binding +
     * bootstrap stubs into a temp source tree, then compiles them into
     * {@code classesDir} so the translator picks them up. Mirrors
     * {@link WindowsNativeBuilder} exactly except for the bootstrap event loop,
     * which calls {@code LinuxImplementation.runMainEventLoop()}.
     */
    private void generateNativeInterfaceAndBootstrapStubs(BuildRequest request, File classesDir, File portClasses)
            throws Exception {
        File stubSource = new File(getBuildDirectory(), "stub");
        stubSource.mkdirs();

        ClassLoader scanLoader = new URLClassLoader(
                new URL[]{ classesDir.toURI().toURL(), portClasses.toURI().toURL() },
                getClass().getClassLoader());
        String registerNatives = registerNativeImplementationsAndCreateStubs(scanLoader, stubSource, classesDir);

        Class[] natives = getNativeInterfaces();
        if (natives != null) {
            for (Class currentNative : natives) {
                writeNativeImplCodenameOne(stubSource, currentNative);
            }
        }

        writeBootstrapStub(request, classesDir, stubSource, registerNatives);

        String javacPath = System.getProperty("java.home") + "/../bin/javac";
        if (!new File(javacPath).exists()) {
            javacPath = System.getProperty("java.home") + "/bin/javac";
        }
        if (!new File(javacPath).exists()) {
            javacPath = "javac";
        }
        String[] st = stubCompileSourceTarget(javacPath);
        String cp = classesDir.getAbsolutePath() + File.pathSeparator + portClasses.getAbsolutePath();
        if (!execWithFiles(stubSource, stubSource, ".java", javacPath, "-source", st[0], "-target", st[1],
                "-classpath", cp, "-d", classesDir.getAbsolutePath())) {
            throw new BuildException("Failed to compile the generated native interface / bootstrap stubs");
        }
    }

    /**
     * Writes {@code XxxImplCodenameOne}: a class with one {@code native} method per
     * interface method. PeerComponent returns/params are bridged as a {@code long}
     * (the native handle the {@code XxxStub} wraps). The translator emits the
     * matching C functions which the app's {@code nativeSources} define.
     */
    private void writeNativeImplCodenameOne(File stubSource, Class currentNative) throws Exception {
        File folder = new File(stubSource, currentNative.getPackage().getName().replace('.', File.separatorChar));
        folder.mkdirs();
        String simple = currentNative.getSimpleName();
        StringBuilder src = new StringBuilder();
        src.append("package ").append(currentNative.getPackage().getName()).append(";\n\n");
        src.append("public class ").append(simple).append("ImplCodenameOne {\n");
        for (Method m : currentNative.getMethods()) {
            String name = m.getName();
            if (name.equals("hashCode") || name.equals("equals") || name.equals("toString")) {
                continue;
            }
            Class returnType = m.getReturnType();
            src.append("    public native ").append(nativeTypeName(returnType)).append(' ').append(name).append('(');
            Class[] params = m.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    src.append(", ");
                }
                src.append(nativeTypeName(params[i])).append(" param").append(i);
            }
            src.append(");\n");
        }
        src.append("}\n");
        try (OutputStream out = new FileOutputStream(new File(folder, simple + "ImplCodenameOne.java"))) {
            out.write(src.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * The Java type used in the generated native method signature for a given
     * interface type. PeerComponent is bridged as a {@code long} handle; every
     * other type maps to its canonical name.
     */
    private static String nativeTypeName(Class<?> type) {
        if (type.getName().equals("com.codename1.ui.PeerComponent")) {
            return "long";
        }
        return type.getCanonicalName();
    }

    /**
     * Writes {@code <MainClass>Stub.java} -- the executable entry point. Its
     * {@code main()} registers the native implementations and boots the Lifecycle
     * app windowed. The clean target auto-selects this as the C {@code main}
     * because it is the only class carrying a {@code main(String[])} method.
     */
    private void writeBootstrapStub(BuildRequest request, File classesDir, File stubSource, String registerNatives)
            throws Exception {
        String pkg = request.getPackageName();
        String main = request.getMainClass();
        File pkgDir = new File(stubSource, pkg.replace('.', File.separatorChar));
        pkgDir.mkdirs();

        String svgInstall = "";
        if (new File(classesDir, "com/codename1/generated/svg/SVGRegistry.class").isFile()) {
            svgInstall = "        try { com.codename1.generated.svg.SVGRegistry.installGlobal(); }"
                    + " catch (Throwable __svg) { __svg.printStackTrace(); }\n";
        }

        StringBuilder src = new StringBuilder();
        src.append("package ").append(pkg).append(";\n\n");
        src.append("import com.codename1.system.NativeLookup;\n");
        src.append("import com.codename1.ui.Display;\n\n");
        src.append("public final class ").append(main).append("Stub {\n");
        src.append("    private ").append(main).append("Stub() { }\n\n");
        src.append("    public static void main(String[] argv) {\n");
        src.append(registerNatives);
        src.append("        final ").append(main).append(" app = new ").append(main).append("();\n");
        src.append("        Display.init(null);\n");
        src.append(svgInstall);
        src.append("        Display.getInstance().callSerially(new Runnable() {\n");
        src.append("            public void run() {\n");
        src.append("                app.init(app);\n");
        src.append("                app.start();\n");
        src.append("            }\n");
        src.append("        });\n");
        src.append("        com.codename1.impl.linux.LinuxImplementation.runMainEventLoop();\n");
        src.append("    }\n");
        src.append("}\n");
        try (OutputStream out = new FileOutputStream(new File(pkgDir, main + "Stub.java"))) {
            out.write(src.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Picks a {@code -source}/{@code -target} pair the running javac accepts for the
     * generated stubs (JDK 9+ dropped 1.6; fall back to 8). Mirrors the Windows builder.
     */
    private String[] stubCompileSourceTarget(String javacPath) {
        int major = -1;
        try {
            String versionOutput = execString(getBuildDirectory(), javacPath, "-version");
            if (versionOutput != null && versionOutput.trim().length() > 0) {
                String[] parts = versionOutput.trim().split("\\s+");
                major = majorJavaVersion(parts[parts.length - 1]);
            }
        } catch (Exception ex) {
            debug("Failed to resolve javac version for Linux stub compile: " + ex.getMessage());
        }
        if (major < 0) {
            major = majorJavaVersion(System.getProperty("java.version"));
        }
        if (major >= 9) {
            return new String[]{"8", "8"};
        }
        return new String[]{"1.6", "1.6"};
    }

    /** Parses a Java version string ("1.8.0_292", "17.0.1", "21") to its feature number, or -1. */
    private static int majorJavaVersion(String version) {
        if (version == null) {
            return -1;
        }
        String v = version.trim();
        if (v.startsWith("1.")) {
            v = v.substring(2);
        }
        int dot = v.indexOf('.');
        if (dot >= 0) {
            v = v.substring(0, dot);
        }
        int us = v.indexOf('_');
        if (us >= 0) {
            v = v.substring(0, us);
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void extractJarResource(String resource, File destDir) throws Exception {
        InputStream is = getResourceAsStream(resource);
        if (is == null) {
            throw new BuildException("Required bundled resource missing: " + resource);
        }
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is);
        try {
            java.util.zip.ZipEntry entry;
            byte[] buf = new byte[8192];
            String destCanon = destDir.getCanonicalPath() + File.separator;
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destDir, entry.getName());
                // Guard against Zip Slip: reject entries that escape destDir via
                // '..' or absolute paths.
                if (!out.getCanonicalPath().startsWith(destCanon)) {
                    throw new BuildException("Refusing to extract entry outside the target directory (zip slip): "
                            + entry.getName());
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                out.getParentFile().mkdirs();
                java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                try {
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                    }
                } finally {
                    fos.close();
                }
            }
        } finally {
            zis.close();
        }
    }

    private static String join(String sep, File... files) {
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(f.getAbsolutePath());
        }
        return sb.toString();
    }
}
