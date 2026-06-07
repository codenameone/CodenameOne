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
 * Native Windows desktop/tablet builder. Produces a standalone Win32 executable
 * (Direct2D/DirectWrite rendering, no JVM) from a Codename One app by running
 * ParparVM's "windows" clean C target and compiling the result with the LLVM
 * MSVC toolchain (clang-cl + CMake + Ninja). This is the Windows analog of
 * {@link IPhoneBuilder} for iOS; the JVM-bundled desktop path remains the
 * separate {@code windows-desktop} (javase) target.
 *
 * <p>Build flow (mirrors the iOS pipeline):</p>
 * <ol>
 *   <li>Unzip the app into classes/res/built-in-res.</li>
 *   <li>Extract the bundled WindowsPort layer (translated platform classes +
 *       C/C++ {@code nativeSources}).</li>
 *   <li>Run the ParparVM translator with the {@code windows} app type, which
 *       emits a CMake project that links the Direct2D/DirectWrite/WIC/WinHTTP/
 *       Win32 stack and globs the port's native sources.</li>
 *   <li>Configure + build that CMake project with clang-cl/Ninja for the
 *       selected target architecture, inside the matching Visual Studio
 *       developer environment.</li>
 *   <li>Collect the {@code .exe} into the result directory.</li>
 * </ol>
 *
 * <p><b>Architecture.</b> {@code windows.arch} selects the target: {@code x64}
 * (x86-64, the default) or {@code arm64}. The toolchain runs natively when the
 * host matches the target and cross-compiles otherwise (the Visual Studio
 * cross-build environment, e.g. {@code arm64_x64}). clang-cl is pointed at the
 * target with an explicit triple via {@link #targetTriple(String)}.</p>
 *
 * <p>The native compile/link runs only on Windows (it needs the MSVC SDK +
 * clang-cl); it is exercised by the Windows CI legs / build VM. The pure build
 * orchestration (arch resolution, translator invocation, CMake argument
 * assembly) is platform independent and unit tested.</p>
 */
public class WindowsNativeBuilder extends Executor {
    /** Supported target architectures for {@code windows.arch}. */
    public static final String ARCH_X64 = "x64";
    public static final String ARCH_ARM64 = "arm64";

    private File resultDir;
    private File windowsExecutable;

    /** The produced native executable, or {@code null} if the build failed. */
    public File getWindowsExecutable() {
        return windowsExecutable;
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
    // long[] holding the native widget handle (HWND / native peer pointer),
    // exactly as the iOS builder does. This is the form the translated native
    // peer code and PeerComponent.create(Object) understand.
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
     * Normalises a {@code windows.arch} hint to one of {@link #ARCH_X64} /
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

    /** clang-cl/LLVM target triple for the given (normalised) architecture. */
    public static String targetTriple(String arch) {
        if (ARCH_ARM64.equals(normalizeArch(arch))) {
            return "aarch64-pc-windows-msvc";
        }
        return "x86_64-pc-windows-msvc";
    }

    /**
     * The {@code vcvarsall.bat} architecture argument for a host -&gt; target
     * pair. When host and target match it is just the target ({@code x64} /
     * {@code arm64}); otherwise it is the cross form ({@code host_target}, e.g.
     * {@code arm64_x64}).
     */
    public static String vcvarsArchArg(String hostArch, String targetArch) {
        String host = normalizeArch(hostArch);
        String target = normalizeArch(targetArch);
        if (host.equals(target)) {
            return target;
        }
        return host + "_" + target;
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
        String arch = normalizeArch(request.getArg("windows.arch", ARCH_X64));
        log("Building native Windows app for arch=" + arch + " (triple " + targetTriple(arch) + ")");

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

        // ParparVM translator + the WindowsPort native layer (translated platform
        // classes and the C/C++ nativeSources) are bundled with the build.
        File parparVMCompilerJar;
        File portClasses;
        File nativeSources;
        try {
            parparVMCompilerJar = getResourceAsFile("/parparvm-compiler.jar", ".jar");
            File portDir = new File(tmpFile, "windowsPort");
            portClasses = new File(portDir, "classes");
            nativeSources = new File(portDir, "nativeSources");
            portClasses.mkdirs();
            nativeSources.mkdirs();
            // Provided on the plugin classpath by the codenameone-windows 'bundle'
            // artifact (WindowsPort.jar = platform classes, nativewindows.jar =
            // C/C++ sources), the same mechanism parparvm-compiler.jar uses.
            extractJarResource("/WindowsPort.jar", portClasses);
            extractJarResource("/nativewindows.jar", nativeSources);
        } catch (Exception ex) {
            throw new BuildException("Failed to stage the WindowsPort native layer. The codenameone "
                    + "maven plugin must provide the codenameone-windows 'bundle' artifact "
                    + "(WindowsPort.jar + nativewindows.jar) on its classpath.", ex);
        }

        // Native interface binding + app bootstrap. Scan the app classes for
        // @NativeInterface implementors and generate, for each, an XxxStub bridge
        // (the NativeLookup target) plus an XxxImplCodenameOne carrying the native
        // methods the translator emits as C functions for the app's C++ to define;
        // and generate a <MainClass>Stub whose main() registers those natives and
        // boots the Lifecycle app windowed. The clean target auto-detects the sole
        // main() class -- a CN1 Lifecycle has none, so the bootstrap stub is it.
        // All generated stubs are compiled into classesDir (a translator source
        // root), so the executable links the app, the port and the native bindings.
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

        // The "windows" app type makes the translator emit a standalone
        // executable CMake project (add_executable + the Win32 link set) and
        // copy the port's nativeSources (incl. the C++ COM layer) into srcRoot.
        List<String> parparCmd = new ArrayList<String>();
        parparCmd.add("java");
        parparCmd.add("-Xmx768m");
        parparCmd.add("-jar");
        parparCmd.add(parparVMCompilerJar.getAbsolutePath());
        // Output type: the portable "clean" C target. The "windows" app type
        // (passed below) specializes it -- an executable CMake project with the
        // Win32 link set -- and binds CodenameOneImplementation to its @Concrete
        // win() target (WindowsImplementation) during translation.
        parparCmd.add("clean");
        // The translator takes ';'-separated source roots: app classes, the
        // WindowsPort platform classes, resources, built-in resources, and the
        // native sources (copied verbatim into the generated srcRoot).
        parparCmd.add(join(";", classesDir, portClasses, resDir, buildinRes, nativeSources));
        parparCmd.add(translatedOut.getAbsolutePath());
        parparCmd.add(request.getMainClass());
        parparCmd.add(request.getPackageName());
        parparCmd.add(request.getDisplayName());
        parparCmd.add(version);
        parparCmd.add("windows"); // project type
        parparCmd.add("none");    // additional native frameworks (none on Windows)
        try {
            if (!exec(tmpFile, 600000, parparCmd.toArray(new String[0]))) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failure while running the ParparVM translator (windows target)", ex);
        }

        File cmakeRoot = new File(translatedOut, "dist");
        File cmakeLists = new File(cmakeRoot, "CMakeLists.txt");
        if (!cmakeLists.exists()) {
            throw new BuildException("Translator did not emit a CMake project at " + cmakeLists.getAbsolutePath());
        }

        File buildDir = new File(cmakeRoot, "build");
        buildDir.mkdirs();

        String triple = targetTriple(arch);
        // Optimized + stripped Release by default (smallest self-contained exe, no
        // PDB). windows.debug=true keeps the symbols (RelWithDebInfo -> /Zi+/DEBUG,
        // a PDB next to the exe) so a native crash address can be symbolized during
        // development. Optimizations are on in both configurations.
        boolean debugSymbols = "true".equalsIgnoreCase(request.getArg("windows.debug", "false"));
        String buildType = debugSymbols ? "RelWithDebInfo" : "Release";
        List<String> configure = new ArrayList<String>();
        configure.add("cmake");
        configure.add("-S");
        configure.add(cmakeRoot.getAbsolutePath());
        configure.add("-B");
        configure.add(buildDir.getAbsolutePath());
        configure.add("-DCMAKE_BUILD_TYPE=" + buildType);
        configure.add("-G");
        configure.add("Ninja");
        configure.add("-DCMAKE_C_COMPILER=clang-cl");
        configure.add("-DCMAKE_CXX_COMPILER=clang-cl");
        configure.add("-DCMAKE_C_FLAGS=--target=" + triple);
        configure.add("-DCMAKE_CXX_FLAGS=--target=" + triple);

        try {
            if (!runWindowsBuildStep(cmakeRoot, arch, configure)) {
                return false;
            }
            List<String> buildCmd = new ArrayList<String>();
            buildCmd.add("cmake");
            buildCmd.add("--build");
            buildCmd.add(buildDir.getAbsolutePath());
            if (!runWindowsBuildStep(cmakeRoot, arch, buildCmd)) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Native Windows build (clang-cl/Ninja) failed", ex);
        }

        File exe = new File(buildDir, request.getMainClass() + ".exe");
        if (!exe.exists()) {
            throw new BuildException("Expected executable not produced: " + exe.getAbsolutePath());
        }
        resultDir = new File(tmpFile, "result");
        resultDir.mkdirs();
        windowsExecutable = new File(resultDir, request.getMainClass() + ".exe");
        try {
            copy(exe, windowsExecutable);
        } catch (Exception ex) {
            throw new BuildException("Failed to collect the built executable", ex);
        }
        log("Native Windows executable: " + windowsExecutable.getAbsolutePath() + " (" + arch + ")");
        return true;
    }

    /**
     * Scans the app classes for native interfaces and generates the binding +
     * bootstrap stubs into a temp source tree, then compiles them into
     * {@code classesDir} so the translator picks them up.
     *
     * <p>Three kinds of class are generated:</p>
     * <ul>
     *   <li>{@code XxxStub} (by {@link #registerNativeImplementationsAndCreateStubs})
     *       -- the class registered with {@code NativeLookup}; it implements the
     *       app's {@code Xxx} interface and delegates to an {@code XxxImplCodenameOne}
     *       instance, wrapping/unwrapping {@link com.codename1.ui.PeerComponent}s as
     *       a {@code long[]} handle.</li>
     *   <li>{@code XxxImplCodenameOne} -- declares the actual {@code native} methods.
     *       The translator emits a C function per method (mangled name) that the
     *       app's own {@code nativeSources} C/C++ defines; this is how a native
     *       interface "resolves to actual C++ native code" on the clean target.</li>
     *   <li>{@code <MainClass>Stub} -- the executable entry point: its {@code main()}
     *       runs the generated {@code NativeLookup.register(...)} calls and boots the
     *       Lifecycle app on a window (Display.init + init/start on the EDT +
     *       runMainEventLoop).</li>
     * </ul>
     */
    private void generateNativeInterfaceAndBootstrapStubs(BuildRequest request, File classesDir, File portClasses)
            throws Exception {
        File stubSource = new File(getBuildDirectory(), "stub");
        stubSource.mkdirs();

        // Scan classesDir for native interfaces; generates the XxxStub bridges and
        // returns the NativeLookup.register(...) source to weave into main().
        ClassLoader scanLoader = new URLClassLoader(
                new URL[]{ classesDir.toURI().toURL(), portClasses.toURI().toURL() },
                getClass().getClassLoader());
        String registerNatives = registerNativeImplementationsAndCreateStubs(scanLoader, stubSource, classesDir);

        // For every native interface, emit the XxxImplCodenameOne with the native
        // method declarations (PeerComponent <-> long handle substitution).
        Class[] natives = getNativeInterfaces();
        if (natives != null) {
            for (Class currentNative : natives) {
                writeNativeImplCodenameOne(stubSource, currentNative);
            }
        }

        writeBootstrapStub(request, classesDir, stubSource, registerNatives);

        // Compile every generated .java into classesDir (already a translator
        // source root) against the app classes + the Windows port classes.
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
     * interface method. {@link com.codename1.ui.PeerComponent} returns become
     * {@code long} (the native handle the {@code XxxStub} wraps via {@code new
     * long[]{...}}) and PeerComponent parameters become {@code long}. The translator
     * emits the matching C functions which the app's {@code nativeSources} define.
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
     * other type maps to its canonical name (arrays keep their {@code []} form).
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
     * app windowed (the same sequence the iOS Stub and the port's own test
     * launchers use). The clean target auto-selects this as the C {@code main}
     * because it is the only class carrying a {@code main(String[])} method.
     */
    private void writeBootstrapStub(BuildRequest request, File classesDir, File stubSource, String registerNatives)
            throws Exception {
        String pkg = request.getPackageName();
        String main = request.getMainClass();
        File pkgDir = new File(stubSource, pkg.replace('.', File.separatorChar));
        pkgDir.mkdirs();

        // If the build-time SVG transcoder produced a registry, install it before
        // the first theme is built so url(*.svg) backgrounds resolve to the real
        // transcoded images (mirrors the iOS/Android stub weaving).
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
        src.append("        com.codename1.impl.windows.WindowsImplementation.runMainEventLoop();\n");
        src.append("    }\n");
        src.append("}\n");
        try (OutputStream out = new FileOutputStream(new File(pkgDir, main + "Stub.java"))) {
            out.write(src.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Picks a {@code -source}/{@code -target} pair the running javac accepts for the
     * generated stubs (JDK 9+ dropped 1.6; fall back to 8). Mirrors the iOS builder.
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
            debug("Failed to resolve javac version for Windows stub compile: " + ex.getMessage());
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

    /**
     * Runs a build step inside the Visual Studio developer environment for the
     * target architecture (so clang-cl, the MSVC CRT and the Windows SDK are on
     * PATH/INCLUDE/LIB), locating {@code vcvarsall.bat} via {@code vswhere}.
     */
    private boolean runWindowsBuildStep(File dir, String targetArch, List<String> command) throws Exception {
        File vcvars = findVcvarsall();
        String vcArch = vcvarsArchArg(detectHostArch(), targetArch);
        StringBuilder line = new StringBuilder();
        if (vcvars != null) {
            line.append("call \"").append(vcvars.getAbsolutePath()).append("\" ").append(vcArch).append(" && ");
        }
        for (int i = 0; i < command.size(); i++) {
            if (i > 0) {
                line.append(' ');
            }
            String token = command.get(i);
            line.append(token.indexOf(' ') >= 0 ? ("\"" + token + "\"") : token);
        }
        return exec(dir, 1800000, "cmd", "/c", line.toString());
    }

    /** Locates {@code vcvarsall.bat} via {@code vswhere}; null if not found. */
    private File findVcvarsall() {
        File vswhere = new File(System.getenv("ProgramFiles(x86)") == null ? "C:\\Program Files (x86)"
                : System.getenv("ProgramFiles(x86)"),
                "Microsoft Visual Studio\\Installer\\vswhere.exe");
        if (!vswhere.exists()) {
            return null;
        }
        try {
            File out = File.createTempFile("vswhere", ".txt");
            out.deleteOnExit();
            // -find prints the matching path(s) to stdout; capture via cmd redirect.
            exec(getBuildDirectory(), 60000, "cmd", "/c",
                    "\"" + vswhere.getAbsolutePath() + "\" -latest -products * "
                            + "-find VC\\Auxiliary\\Build\\vcvarsall.bat > \"" + out.getAbsolutePath() + "\"");
            for (String l : readLines(out)) {
                String t = l.trim();
                if (t.toLowerCase().endsWith("vcvarsall.bat") && new File(t).exists()) {
                    return new File(t);
                }
            }
        } catch (Exception ex) {
            log("vswhere lookup failed: " + ex.getMessage());
        }
        return null;
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

    private static List<String> readLines(File f) throws Exception {
        List<String> lines = new ArrayList<String>();
        java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
        try {
            String l;
            while ((l = r.readLine()) != null) {
                lines.add(l);
            }
        } finally {
            r.close();
        }
        return lines;
    }
}
