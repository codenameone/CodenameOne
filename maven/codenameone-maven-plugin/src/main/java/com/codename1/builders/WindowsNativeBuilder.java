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
import java.io.InputStream;
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
 *
 * @author Codename One
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

    // Native-peer component code generation. Peer components are not wired on the
    // Windows port yet; these provide the minimal, well-formed code the native
    // interface generator expects and are revisited when native peers land.
    @Override
    protected String getDeviceIdCode() {
        return "\"\"";
    }

    @Override
    protected String generatePeerComponentCreationCode(String methodCallString) {
        return "PeerComponent.create(" + methodCallString + ")";
    }

    @Override
    protected String convertPeerComponentToNative(String param) {
        return param + ".getNativePeer()";
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
        parparCmd.add("windows");
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
        List<String> configure = new ArrayList<String>();
        configure.add("cmake");
        configure.add("-S");
        configure.add(cmakeRoot.getAbsolutePath());
        configure.add("-B");
        configure.add(buildDir.getAbsolutePath());
        configure.add("-DCMAKE_BUILD_TYPE=Release");
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
