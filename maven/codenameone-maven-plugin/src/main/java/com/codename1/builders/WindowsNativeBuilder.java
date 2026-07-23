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
import java.io.IOException;
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

        // The JDK class set the translator emits as java_lang_*.c/.h /
        // java_util_*.c/.h is shipped as a separate classpath resource inside
        // the codenameone-parparvm bundle. Unzipping it into classesDir adds
        // those classes to the translator source roots; without this step the
        // translator emits cn1_globals.c referencing java_lang_Class.h but
        // never produces the header (iOS uses the same wire-up).
        try {
            unzip(getResourceAsStream("/parparvm-java-api.jar"), classesDir, classesDir, classesDir);
        } catch (Exception ex) {
            throw new BuildException("Failed to load JavaAPI.jar", ex);
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
        // First-class Bluetooth: only when the app references
        // com.codename1.bluetooth do we compile the real (libcn1ble-backed)
        // path of cn1_windows_ble.c and ship cn1ble.dll next to the exe --
        // the usage-gated approach the iOS builder uses for CoreBluetooth.
        final boolean[] usesBluetoothHolder = {false};
        try {
            scanClassesForPermissions(classesDir, new Executor.ClassScanner() {
                @Override
                public void usesClass(String cls) {
                    if (cls != null && cls.startsWith("com/codename1/bluetooth/")) {
                        usesBluetoothHolder[0] = true;
                    }
                }

                @Override
                public void usesClassMethod(String cls, String method) {
                }
            });
        } catch (IOException ex) {
            throw new BuildException("Failed to scan for Bluetooth usage", ex);
        }
        boolean usesBluetooth = usesBluetoothHolder[0];

        List<String> parparCmd = new ArrayList<String>();
        parparCmd.add("java");
        // 2g: the clean target's readNativeFiles loads both .m and .c (clean's
        // extension), so the in-memory native-source set is ~2x what the iOS
        // target loads, and the markDependencies/NativeSymbolIndex pass needs
        // headroom on top of that. Without the JavaAPI in classesDir the
        // translator never reaches that stage at scale; once JavaAPI is added
        // the older 768m cap GC-thrashes.
        parparCmd.add("-Xmx2g");
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

        // Enable the libcn1ble path in cn1_windows_ble.c. Appended after the
        // translator's add_executable so target_compile_definitions resolves
        // ${PROJECT_NAME}. No link line -- the C loads cn1ble.dll at runtime.
        if (usesBluetooth) {
            try {
                java.nio.file.Files.write(cmakeLists.toPath(),
                        ("\n# First-class Bluetooth (com.codename1.bluetooth)\n"
                        + "target_compile_definitions(${PROJECT_NAME} PRIVATE CN1_INCLUDE_BLUETOOTH)\n")
                                .getBytes("UTF-8"),
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException ex) {
                throw new BuildException("Failed to enable CN1_INCLUDE_BLUETOOTH in the CMake project", ex);
            }
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

        // On a Windows host the build runs inside the Visual Studio developer
        // environment (clang-cl + the MSVC CRT/SDK reach the compiler through
        // PATH/INCLUDE/LIB via vcvarsall). On any other host -- e.g. the Linux build
        // cloud -- there is no Visual Studio, so cross-compile with clang-cl +
        // lld-link + llvm-rc against a Windows SDK laid out by `xwin splat` (pointed
        // at by windows.sdkRoot / CN1_XWIN_SYSROOT). clang is a cross-compiler, so
        // the PE is identical to a Windows-host build; this is the same toolchain the
        // windows-cross-compile CI validates. Both x64 and arm64 targets are produced
        // this way (the SDK arch subdir follows windows.arch).
        boolean cross = !is_windows;
        File xwinSysroot = cross ? resolveXwinSysroot(request) : null;

        // windows.msix=true additionally compiles the Windows 11 Widgets Board
        // COM provider (cn1_windows_widgetboard.cpp, gated on CN1_WIDGETBOARD)
        // into the exe and, after signing, packs an MSIX around it -- see
        // buildMsixPackage below. The define is injected through the compile
        // flags the builder already passes to CMake (the least invasive hook;
        // the translator-generated CMakeLists is untouched), so plain builds
        // are unaffected.
        boolean msix = "true".equalsIgnoreCase(request.getArg("windows.msix", "false"));
        File winAppSdk = msix ? resolveWinAppSdkDir() : null;
        String widgetBoardCompileFlags = msix ? widgetBoardCompileFlags(winAppSdk) : "";

        List<String> configure = new ArrayList<String>();
        configure.add("cmake");
        configure.add("-S");
        configure.add(cmakeRoot.getAbsolutePath());
        configure.add("-B");
        configure.add(buildDir.getAbsolutePath());
        configure.add("-DCMAKE_BUILD_TYPE=" + buildType);
        configure.add("-G");
        configure.add("Ninja");
        configure.add("-DCMAKE_C_COMPILER=" + clangClExecutable());
        configure.add("-DCMAKE_CXX_COMPILER=" + clangClExecutable());
        if (cross) {
            addCrossCompileConfigure(configure, triple, arch, xwinSysroot,
                    widgetBoardCompileFlags, msix ? widgetBoardLinkFlags(winAppSdk, arch) : "");
            if (msix) {
                // same C++20 requirement as the Windows-host branch below
                configure.add("-DCMAKE_CXX_STANDARD=20");
            }
        } else {
            configure.add("-DCMAKE_C_FLAGS=--target=" + triple + widgetBoardCompileFlags);
            configure.add("-DCMAKE_CXX_FLAGS=--target=" + triple + widgetBoardCompileFlags);
            if (msix) {
                configure.add("-DCMAKE_EXE_LINKER_FLAGS=" + widgetBoardLinkFlags(winAppSdk, arch).trim());
                // C++/WinRT at C++17 falls back to <experimental/coroutine>, which the
                // modern MSVC STL rejects under clang-cl; the generated CMakeLists only
                // defaults CMAKE_CXX_STANDARD when it is not supplied here.
                configure.add("-DCMAKE_CXX_STANDARD=20");
            }
        }

        try {
            if (!runBuildStep(cmakeRoot, arch, cross, configure)) {
                return false;
            }
            List<String> buildCmd = new ArrayList<String>();
            buildCmd.add("cmake");
            buildCmd.add("--build");
            buildCmd.add(buildDir.getAbsolutePath());
            if (!runBuildStep(cmakeRoot, arch, cross, buildCmd)) {
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
        // Ship cn1ble.dll next to the exe so the runtime LoadLibraryA in
        // cn1_windows_ble.c finds it (it loads by the exe's own directory).
        // The arch-keyed DLL is staged into the native bundle from cn1-binaries
        // by the maven/windows build.
        if (usesBluetooth) {
            File bleLib = new File(nativeSources, "ble/" + arch + "/cn1ble.dll");
            if (bleLib.isFile()) {
                try {
                    copy(bleLib, new File(resultDir, "cn1ble.dll"));
                } catch (Exception ex) {
                    throw new BuildException("Failed to bundle cn1ble.dll", ex);
                }
            } else {
                log("WARNING: com.codename1.bluetooth is used but no cn1ble.dll was "
                        + "bundled for arch " + arch + " (" + bleLib.getAbsolutePath()
                        + "); native Bluetooth will report unavailable at runtime.");
            }
        }
        // Authenticode-sign the exe (osslsigncode) when a code-signing certificate
        // is supplied; otherwise it ships unsigned (which runs, but trips SmartScreen
        // / "Unknown publisher").
        signWindowsExecutable(windowsExecutable, request);
        // windows.msix=true: wrap the signed exe in an MSIX package that also
        // declares the Windows 11 Widgets Board provider (surfaces.json kinds).
        buildMsixPackage(request, resDir, arch);
        log("Native Windows executable: " + windowsExecutable.getAbsolutePath() + " (" + arch + ")");
        return true;
    }

    /**
     * Authenticode-signs the produced {@code .exe} with {@code osslsigncode} (which
     * signs Windows PE files on any OS, so it works in the Linux build cloud). No-op
     * unless a code-signing certificate is provided -- the binary then ships unsigned,
     * which runs but shows "Unknown publisher" in UAC and trips SmartScreen on
     * download. Configuration (all optional except a certificate):
     *
     * <ul>
     *   <li>{@code windows.signing.pkcs12} -- path to a PKCS#12 (.pfx/.p12) cert+key;
     *       falls back to the build request's uploaded certificate.</li>
     *   <li>{@code windows.signing.password} -- the PKCS#12 password (falls back to
     *       the request's certificate password).</li>
     *   <li>{@code windows.signing.timestampUrl} -- RFC&nbsp;3161 timestamp server
     *       (default {@code http://timestamp.digicert.com}; empty disables it).</li>
     *   <li>{@code windows.signing.digest} -- digest algorithm (default {@code sha256}).</li>
     *   <li>{@code windows.signing.name} / {@code windows.signing.url} -- the
     *       description + URL embedded in the signature (default the app display name).</li>
     *   <li>{@code windows.signing=false} -- force-skip even if a certificate is present.</li>
     * </ul>
     *
     * <p>Hardware-backed / cloud keys (the post-2023 CA/B requirement -- Azure Trusted
     * Signing, DigiCert KeyLocker, ...) are reached by pointing the above at a
     * PKCS#11-fronted credential per the signing service's docs; the command shape is
     * the same.</p>
     */
    private void signWindowsExecutable(File exe, BuildRequest request) {
        if ("false".equalsIgnoreCase(request.getArg("windows.signing", "true"))) {
            return;
        }
        File pkcs12;
        String pkcs12Hint = request.getArg("windows.signing.pkcs12", null);
        try {
            if (pkcs12Hint != null && !pkcs12Hint.isEmpty()) {
                pkcs12 = new File(pkcs12Hint);
                if (!pkcs12.isFile()) {
                    throw new BuildException("windows.signing.pkcs12 file not found: " + pkcs12.getAbsolutePath());
                }
            } else if (request.getCertificate() != null && request.getCertificate().length > 0) {
                pkcs12 = File.createTempFile("cn1-winsign", ".p12", getBuildDirectory());
                try (FileOutputStream out = new FileOutputStream(pkcs12)) {
                    out.write(request.getCertificate());
                }
            } else {
                log("Native Windows executable is unsigned (no code-signing certificate provided; set "
                        + "windows.signing.pkcs12 or supply a certificate to Authenticode-sign it).");
                return;
            }
        } catch (java.io.IOException ex) {
            throw new BuildException("Failed to stage the Windows code-signing certificate", ex);
        }

        String password = request.getArg("windows.signing.password", null);
        if (password == null) {
            password = request.getCertificatePassword();
        }
        if (password == null) {
            password = "";
        }
        String digest = request.getArg("windows.signing.digest", "sha256");
        String timestampUrl = request.getArg("windows.signing.timestampUrl", "http://timestamp.digicert.com");
        String name = request.getArg("windows.signing.name", request.getDisplayName());
        String url = request.getArg("windows.signing.url", null);
        String tool = System.getenv("CN1_OSSLSIGNCODE");
        if (tool == null || tool.isEmpty()) {
            tool = "osslsigncode";
        }

        File signed = new File(exe.getParentFile(), exe.getName() + ".signed");
        List<String> cmd = new ArrayList<String>();
        cmd.add(tool);
        cmd.add("sign");
        cmd.add("-pkcs12");
        cmd.add(pkcs12.getAbsolutePath());
        cmd.add("-pass");
        cmd.add(password);
        cmd.add("-h");
        cmd.add(digest);
        if (name != null && !name.isEmpty()) {
            cmd.add("-n");
            cmd.add(name);
        }
        if (url != null && !url.isEmpty()) {
            cmd.add("-i");
            cmd.add(url);
        }
        if (timestampUrl != null && !timestampUrl.isEmpty()) {
            cmd.add("-ts");
            cmd.add(timestampUrl);
        }
        cmd.add("-in");
        cmd.add(exe.getAbsolutePath());
        cmd.add("-out");
        cmd.add(signed.getAbsolutePath());

        try {
            if (!exec(getBuildDirectory(), 600000, cmd.toArray(new String[0])) || !signed.isFile()) {
                throw new BuildException("Authenticode signing failed (osslsigncode). Ensure osslsigncode is on PATH "
                        + "(set CN1_OSSLSIGNCODE to override) and the certificate/password are valid.");
            }
            if (!exe.delete() || !signed.renameTo(exe)) {
                copy(signed, exe);
                signed.delete();
            }
            log("Authenticode-signed " + exe.getName() + (timestampUrl != null && !timestampUrl.isEmpty()
                    ? " (timestamped)" : " (not timestamped)"));
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Authenticode signing failed (osslsigncode)", ex);
        }
    }

    /* --------------------------------------------------------------------------
     * MSIX packaging + Windows 11 Widgets Board (windows.msix=true)
     * --------------------------------------------------------------------------
     * Two distribution channels exist for the native Windows target:
     *
     *   1. The plain signed exe (the default). Users get the floating layered
     *      widget windows (cn1_windows_widgets.cpp / WindowsWidgetBridge) --
     *      always-on-top desktop widgets that work with zero packaging.
     *
     *   2. windows.msix=true: the exe is additionally wrapped in an MSIX
     *      package that declares a Windows 11 Widgets Board provider -- real
     *      widgets in the Win+W board, rendered from Adaptive Cards mapped off
     *      the same persisted surface descriptors. This channel has hard
     *      distribution prerequisites, which is why it is opt-in:
     *        - the MSIX must be signed with a certificate the TARGET machine
     *          trusts (a self-signed cert means dev-sideloading only; store or
     *          EV/organization certs for real distribution);
     *        - the Windows App SDK runtime (WindowsAppRuntime redistributable,
     *          1.5 series) must be installed on the target machine -- the
     *          provider binds it at startup via MddBootstrapInitialize;
     *        - Widgets Board exists on Windows 11 only (older Windows installs
     *          the MSIX fine but shows no widgets).
     *
     * Build-time requirements when windows.msix=true:
     *   - CN1_WINAPPSDK_DIR must point at a Windows App SDK layout with
     *     include/ (C++/WinRT projections + MddBootstrap.h), lib/<x64|arm64>/
     *     or the raw nupkg's lib/win10-<x64|arm64>/
     *     (Microsoft.WindowsAppRuntime.Bootstrap.lib) and optionally
     *     bin/<arch>/ or runtimes/win10-<arch>/native/
     *     Microsoft.WindowsAppRuntime.Bootstrap.dll (copied into the package
     *     next to the exe). Note the Microsoft.WindowsAppSDK NuGet package
     *     ships only winmd metadata, NOT prebuilt C++/WinRT headers: generate
     *     the projections into include/ with cppwinrt.exe (the
     *     Microsoft.Windows.CppWinRT package), e.g.
     *       cppwinrt -in lib/uap10.0/Microsoft.Windows.Widgets.winmd -in local
     *                -output include
     *     -- the same recipe the widgetboard-compile-check CI job uses
     *     (.github/workflows/parparvm-tests-windows.yml). Paths must not
     *     contain spaces (they travel through CMake flag strings).
     *   - `makemsix` (the cross-platform msix-packaging tool) must be on PATH
     *     (CN1_MAKEMSIX overrides), so the Linux build daemon can pack without
     *     a Windows host.
     *
     * Hints: windows.msix.identityName / windows.msix.publisher /
     * windows.msix.version (default 1.0.0.0) fill the package Identity;
     * windows.msix.pfx / windows.msix.password sign the package (falling back
     * to the exe-signing configuration). Widget definitions are derived from
     * the project's surfaces.json kinds; without a surfaces.json the MSIX is
     * produced with no widget extension (plain packaged app).
     * ------------------------------------------------------------------------ */

    /**
     * The COM class id of the widget provider ExeServer. Must match
     * {@code CN1_WIDGET_PROVIDER_CLSID} in
     * {@code Ports/WindowsPort/nativeSources/cn1_windows_widgetboard.cpp}.
     */
    static final String WIDGET_PROVIDER_CLSID = "C0DE4A11-5A2F-4E7B-9C61-7D1B4A0C8E52";

    /**
     * Resolves the Windows App SDK layout used to compile the Widgets Board
     * provider ({@code CN1_WINAPPSDK_DIR}); required when {@code windows.msix=true}.
     */
    private File resolveWinAppSdkDir() {
        String path = System.getenv("CN1_WINAPPSDK_DIR");
        if (path == null || path.isEmpty()) {
            throw new BuildException("windows.msix=true compiles the Windows 11 Widgets Board provider, "
                    + "which needs the Windows App SDK headers and libs. Point the CN1_WINAPPSDK_DIR "
                    + "environment variable at a layout containing include/ (C++/WinRT projections + "
                    + "MddBootstrap.h) and lib/<x64|arm64>/ (or the raw nupkg's lib/win10-<arch>/) "
                    + "Microsoft.WindowsAppRuntime.Bootstrap.lib. Extract the layout from the "
                    + "Microsoft.WindowsAppSDK NuGet package, then generate the C++/WinRT projection "
                    + "headers into include/ with cppwinrt.exe (Microsoft.Windows.CppWinRT package): "
                    + "cppwinrt -in lib/uap10.0/Microsoft.Windows.Widgets.winmd -in local -output include");
        }
        File dir = new File(path);
        if (!new File(dir, "include").isDirectory()) {
            throw new BuildException("CN1_WINAPPSDK_DIR does not look like a Windows App SDK layout "
                    + "(missing include/): " + dir.getAbsolutePath());
        }
        return dir;
    }

    /** clang-cl flags enabling the Widgets Board provider compile (CN1_WIDGETBOARD). */
    private static String widgetBoardCompileFlags(File winAppSdk) {
        return " /DCN1_WIDGETBOARD=1 /I" + new File(winAppSdk, "include").getAbsolutePath();
    }

    /**
     * Linker flags adding the WindowsAppRuntime bootstrap import library.
     * Accepts both the documented lib/&lt;arch&gt;/ layout and the raw NuGet
     * package's lib/win10-&lt;arch&gt;/ layout, so an extracted
     * Microsoft.WindowsAppSDK nupkg works as CN1_WINAPPSDK_DIR without
     * re-arranging directories.
     */
    private static String widgetBoardLinkFlags(File winAppSdk, String arch) {
        String normalized = normalizeArch(arch);
        File libDir = new File(winAppSdk, "lib/" + normalized);
        if (!new File(libDir, "Microsoft.WindowsAppRuntime.Bootstrap.lib").isFile()) {
            File nupkgLayout = new File(winAppSdk, "lib/win10-" + normalized);
            if (new File(nupkgLayout, "Microsoft.WindowsAppRuntime.Bootstrap.lib").isFile()) {
                libDir = nupkgLayout;
            }
        }
        return " /libpath:" + libDir.getAbsolutePath() + " Microsoft.WindowsAppRuntime.Bootstrap.lib";
    }

    /**
     * Packs the built (already signed) exe into an MSIX declaring the COM
     * ExeServer + {@code com.microsoft.windows.widgets} app extension, then
     * signs the package. See the block comment above for the channel
     * requirements. No-op unless {@code windows.msix=true}.
     */
    private void buildMsixPackage(BuildRequest request, File resDir, String arch) {
        if (!"true".equalsIgnoreCase(request.getArg("windows.msix", "false"))) {
            return;
        }
        log("Packaging MSIX (windows.msix=true) with Widgets Board provider");
        try {
            File layout = new File(getBuildDirectory(), "msix-layout");
            File assets = new File(layout, "Assets");
            File publicDir = new File(layout, "Public");
            layout.mkdirs();
            assets.mkdirs();
            publicDir.mkdirs();

            // The manifest's Executable/ComServer reference "app.exe" so the
            // COM activation command line is stable regardless of main class.
            copy(windowsExecutable, new File(layout, "app.exe"));

            // The provider binds the WindowsAppRuntime via MddBootstrapInitialize,
            // which needs Microsoft.WindowsAppRuntime.Bootstrap.dll next to the
            // exe. Copy it out of the SDK layout when present (either the
            // documented bin/<arch>/ layout or the raw nupkg's
            // runtimes/win10-<arch>/native/); otherwise the developer must add
            // it to the package manually.
            File winAppSdk = resolveWinAppSdkDir();
            File bootstrapDll = new File(winAppSdk,
                    "bin/" + normalizeArch(arch) + "/Microsoft.WindowsAppRuntime.Bootstrap.dll");
            if (!bootstrapDll.isFile()) {
                File nupkgDll = new File(winAppSdk, "runtimes/win10-" + normalizeArch(arch)
                        + "/native/Microsoft.WindowsAppRuntime.Bootstrap.dll");
                if (nupkgDll.isFile()) {
                    bootstrapDll = nupkgDll;
                }
            }
            if (bootstrapDll.isFile()) {
                copy(bootstrapDll, new File(layout, "Microsoft.WindowsAppRuntime.Bootstrap.dll"));
            } else {
                log("Warning: " + bootstrapDll.getAbsolutePath() + " not found; add "
                        + "Microsoft.WindowsAppRuntime.Bootstrap.dll to the MSIX next to app.exe "
                        + "or the widget provider will fail to bind the WindowsAppRuntime.");
            }

            // Widget definition icons/screenshots + package logos all reuse the
            // app icon; Windows scales them (pixel-perfect per-scale assets can
            // be a follow-up).
            byte[] icon = request.getIcon();
            if (icon == null || icon.length == 0) {
                throw new BuildException("windows.msix=true needs the app icon to generate the MSIX "
                        + "logo assets, but the build request carries no icon.");
            }
            createFile(new File(assets, "StoreLogo.png"), icon);
            createFile(new File(assets, "Square150x150Logo.png"), icon);
            createFile(new File(assets, "Square44x44Logo.png"), icon);
            createFile(new File(assets, "WidgetScreenshot.png"), icon);
            // uap3:AppExtension requires a public folder; ship a marker file so
            // packers that skip empty directories keep it.
            createFile(new File(publicDir, "readme.txt"),
                    "Codename One widget provider public folder".getBytes(StandardCharsets.UTF_8));

            String widgetDefinitions = buildWidgetDefinitionsXml(resDir);
            String manifest = buildAppxManifest(request, arch, widgetDefinitions);
            createFile(new File(layout, "AppxManifest.xml"), manifest.getBytes(StandardCharsets.UTF_8));

            // Pack with makemsix (the MSIX SDK's cross-platform packer; runs on
            // the Linux daemon hosts). Must be on PATH, CN1_MAKEMSIX overrides.
            File msix = new File(resultDir, request.getMainClass() + ".msix");
            String makemsix = System.getenv("CN1_MAKEMSIX");
            if (makemsix == null || makemsix.isEmpty()) {
                makemsix = "makemsix";
            }
            if (!exec(getBuildDirectory(), 600000, makemsix, "pack",
                    "-d", layout.getAbsolutePath(), "-p", msix.getAbsolutePath()) || !msix.isFile()) {
                throw new BuildException("MSIX packaging failed. Ensure the `makemsix` tool "
                        + "(from the msix-packaging project) is on PATH, or point CN1_MAKEMSIX at it.");
            }
            signMsixPackage(msix, request);
            log("MSIX package: " + msix.getAbsolutePath());
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to build the MSIX package", ex);
        }
    }

    /**
     * Derives the {@code <Definitions>} XML of the widget provider extension
     * from the project's {@code surfaces.json} (the same build-time kinds
     * manifest the iOS/Android builders consume). Returns null when the
     * project has no surfaces.json / no kinds -- the MSIX is then produced
     * without the widgets extension.
     */
    private String buildWidgetDefinitionsXml(File resDir) throws Exception {
        File surfacesJson = new File(resDir, "surfaces.json");
        if (!surfacesJson.isFile()) {
            log("No surfaces.json in the project resources; the MSIX is packaged without "
                    + "Widgets Board definitions (widget kinds must be declared at build time).");
            return null;
        }
        java.util.Map<String, Object> doc;
        try (java.io.InputStreamReader reader = new java.io.InputStreamReader(
                new java.io.FileInputStream(surfacesJson), StandardCharsets.UTF_8)) {
            doc = new com.codename1.builders.util.JSONParser().parseJSON(reader);
        }
        Object kindsObj = doc.get("kinds");
        if (!(kindsObj instanceof List) || ((List) kindsObj).isEmpty()) {
            log("surfaces.json declares no widget kinds; the MSIX is packaged without "
                    + "Widgets Board definitions.");
            return null;
        }
        // indented to sit directly inside <Definitions> in the manifest below
        StringBuilder sb = new StringBuilder();
        for (Object kindObj : (List) kindsObj) {
            if (!(kindObj instanceof java.util.Map)) {
                continue;
            }
            java.util.Map kind = (java.util.Map) kindObj;
            Object idObj = kind.get("id");
            if (!(idObj instanceof String) || !((String) idObj).matches("[a-z][a-z0-9_]*")) {
                throw new BuildException("Invalid widget kind id '" + idObj
                        + "' in surfaces.json; ids must match [a-z][a-z0-9_]*");
            }
            String id = (String) idObj;
            String name = kind.get("name") instanceof String ? (String) kind.get("name") : id;
            String description = kind.get("description") instanceof String
                    ? (String) kind.get("description") : name;
            sb.append("                  <Definition Id=\"").append(escapeXml(id))
              .append("\" DisplayName=\"").append(escapeXml(name))
              .append("\" Description=\"").append(escapeXml(description)).append("\">\n")
              .append("                    <Capabilities>\n")
              .append("                      <Capability><Size Name=\"small\"/></Capability>\n")
              .append("                      <Capability><Size Name=\"medium\"/></Capability>\n")
              .append("                      <Capability><Size Name=\"large\"/></Capability>\n")
              .append("                    </Capabilities>\n")
              .append("                    <ThemeResources>\n")
              .append("                      <Icons>\n")
              .append("                        <Icon Path=\"Assets\\Square44x44Logo.png\"/>\n")
              .append("                      </Icons>\n")
              .append("                      <Screenshots>\n")
              .append("                        <Screenshot Path=\"Assets\\WidgetScreenshot.png\" "
                      + "DisplayAltText=\"").append(escapeXml(name)).append("\"/>\n")
              .append("                      </Screenshots>\n")
              .append("                      <DarkMode/>\n")
              .append("                      <LightMode/>\n")
              .append("                    </ThemeResources>\n")
              .append("                  </Definition>\n");
        }
        return sb.toString();
    }

    /**
     * Generates the AppxManifest.xml: package identity from the
     * {@code windows.msix.*} hints, a full-trust win32 Application around
     * app.exe, the COM ExeServer re-activating the exe with
     * {@code -RegisterProcessAsComServer}, and (when surfaces.json declares
     * kinds) the {@code com.microsoft.windows.widgets} app extension.
     */
    private String buildAppxManifest(BuildRequest request, String arch, String widgetDefinitions) {
        String identityName = request.getArg("windows.msix.identityName", request.getPackageName());
        String publisher = request.getArg("windows.msix.publisher", "CN=" + request.getDisplayName());
        String version = normalizeMsixVersion(request.getArg("windows.msix.version",
                request.getVersion() != null ? request.getVersion() : "1.0.0.0"));
        String displayName = escapeXml(request.getDisplayName());
        String procArch = ARCH_ARM64.equals(normalizeArch(arch)) ? "arm64" : "x64";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
          .append("<Package xmlns=\"http://schemas.microsoft.com/appx/manifest/foundation/windows10\"\n")
          .append("         xmlns:uap=\"http://schemas.microsoft.com/appx/manifest/uap/windows10\"\n")
          .append("         xmlns:uap3=\"http://schemas.microsoft.com/appx/manifest/uap/windows10/3\"\n")
          .append("         xmlns:com=\"http://schemas.microsoft.com/appx/manifest/com/windows10\"\n")
          .append("         xmlns:rescap=\"http://schemas.microsoft.com/appx/manifest/foundation/windows10/restrictedcapabilities\"\n")
          .append("         IgnorableNamespaces=\"uap uap3 com rescap\">\n")
          .append("  <Identity Name=\"").append(escapeXml(identityName))
          .append("\" Publisher=\"").append(escapeXml(publisher))
          .append("\" Version=\"").append(version)
          .append("\" ProcessorArchitecture=\"").append(procArch).append("\"/>\n")
          .append("  <Properties>\n")
          .append("    <DisplayName>").append(displayName).append("</DisplayName>\n")
          .append("    <PublisherDisplayName>").append(escapeXml(request.getVendor() != null
                  ? request.getVendor() : request.getDisplayName())).append("</PublisherDisplayName>\n")
          .append("    <Logo>Assets\\StoreLogo.png</Logo>\n")
          .append("  </Properties>\n")
          // 10.0.22000 == Windows 11: the Widgets Board floor. The package
          // installs there and up; the plain exe remains the pre-11 channel.
          .append("  <Dependencies>\n")
          .append("    <TargetDeviceFamily Name=\"Windows.Desktop\" MinVersion=\"10.0.22000.0\" "
                  + "MaxVersionTested=\"10.0.26100.0\"/>\n")
          .append("  </Dependencies>\n")
          .append("  <Resources>\n")
          .append("    <Resource Language=\"en-us\"/>\n")
          .append("  </Resources>\n")
          .append("  <Applications>\n")
          .append("    <Application Id=\"App\" Executable=\"app.exe\" "
                  + "EntryPoint=\"Windows.FullTrustApplication\">\n")
          .append("      <uap:VisualElements DisplayName=\"").append(displayName)
          .append("\" Description=\"").append(displayName)
          .append("\" BackgroundColor=\"transparent\" "
                  + "Square150x150Logo=\"Assets\\Square150x150Logo.png\" "
                  + "Square44x44Logo=\"Assets\\Square44x44Logo.png\"/>\n");
        if (widgetDefinitions != null) {
            sb.append("      <Extensions>\n")
              // COM out-of-proc server: the Widgets Board activates the widget
              // provider by re-launching this very exe with the
              // -RegisterProcessAsComServer argument (handled in
              // cn1_windows_widgetboard.cpp before the app UI would start).
              .append("        <com:Extension Category=\"windows.comServer\">\n")
              .append("          <com:ComServer>\n")
              .append("            <com:ExeServer Executable=\"app.exe\" "
                      + "Arguments=\"-RegisterProcessAsComServer\" DisplayName=\"")
              .append(displayName).append(" Widget Provider\">\n")
              .append("              <com:Class Id=\"").append(WIDGET_PROVIDER_CLSID)
              .append("\" DisplayName=\"").append(displayName).append(" Widget Provider\"/>\n")
              .append("            </com:ExeServer>\n")
              .append("          </com:ComServer>\n")
              .append("        </com:Extension>\n")
              .append("        <uap3:Extension Category=\"windows.appExtension\">\n")
              .append("          <uap3:AppExtension Name=\"com.microsoft.windows.widgets\" "
                      + "DisplayName=\"").append(displayName)
              .append("\" Id=\"cn1widgets\" PublicFolder=\"Public\">\n")
              .append("            <uap3:Properties>\n")
              .append("              <WidgetProvider>\n")
              .append("                <ProviderIcons>\n")
              .append("                  <Icon Path=\"Assets\\Square44x44Logo.png\"/>\n")
              .append("                </ProviderIcons>\n")
              .append("                <Activation>\n")
              .append("                  <CreateInstance ClassId=\"")
              .append(WIDGET_PROVIDER_CLSID).append("\"/>\n")
              .append("                </Activation>\n")
              .append("                <Definitions>\n")
              .append(widgetDefinitions)
              .append("                </Definitions>\n")
              .append("              </WidgetProvider>\n")
              .append("            </uap3:Properties>\n")
              .append("          </uap3:AppExtension>\n")
              .append("        </uap3:Extension>\n")
              .append("      </Extensions>\n");
        }
        sb.append("    </Application>\n")
          .append("  </Applications>\n")
          .append("  <Capabilities>\n")
          .append("    <rescap:Capability Name=\"runFullTrust\"/>\n")
          .append("  </Capabilities>\n")
          .append("</Package>\n");
        return sb.toString();
    }

    /**
     * Signs the MSIX with osslsigncode (2.6+ supports the APPX/MSIX format),
     * reusing the exe-signing command shape. Prefers windows.msix.pfx /
     * windows.msix.password, falling back to the exe signing configuration;
     * with no certificate at all the package is left unsigned -- installable
     * only after the developer signs it, since Windows refuses unsigned MSIX.
     */
    private void signMsixPackage(File msix, BuildRequest request) {
        File pkcs12 = null;
        String pfxHint = request.getArg("windows.msix.pfx",
                request.getArg("windows.signing.pkcs12", null));
        try {
            if (pfxHint != null && !pfxHint.isEmpty()) {
                pkcs12 = new File(pfxHint);
                if (!pkcs12.isFile()) {
                    throw new BuildException("windows.msix.pfx file not found: " + pkcs12.getAbsolutePath());
                }
            } else if (request.getCertificate() != null && request.getCertificate().length > 0) {
                pkcs12 = File.createTempFile("cn1-msixsign", ".p12", getBuildDirectory());
                try (FileOutputStream out = new FileOutputStream(pkcs12)) {
                    out.write(request.getCertificate());
                }
            }
        } catch (java.io.IOException ex) {
            throw new BuildException("Failed to stage the MSIX signing certificate", ex);
        }
        if (pkcs12 == null) {
            log("MSIX left unsigned (no certificate). Windows refuses to install unsigned MSIX "
                    + "packages: supply windows.msix.pfx/windows.msix.password (a cert the target "
                    + "machine trusts; self-signed works only for dev sideloading with the cert "
                    + "imported) or sign the package yourself before distribution.");
            return;
        }
        String password = request.getArg("windows.msix.password",
                request.getArg("windows.signing.password", request.getCertificatePassword()));
        if (password == null) {
            password = "";
        }
        String tool = System.getenv("CN1_OSSLSIGNCODE");
        if (tool == null || tool.isEmpty()) {
            tool = "osslsigncode";
        }
        File signed = new File(msix.getParentFile(), msix.getName() + ".signed");
        List<String> cmd = new ArrayList<String>();
        cmd.add(tool);
        cmd.add("sign");
        cmd.add("-pkcs12");
        cmd.add(pkcs12.getAbsolutePath());
        cmd.add("-pass");
        cmd.add(password);
        cmd.add("-h");
        cmd.add(request.getArg("windows.signing.digest", "sha256"));
        cmd.add("-in");
        cmd.add(msix.getAbsolutePath());
        cmd.add("-out");
        cmd.add(signed.getAbsolutePath());
        try {
            if (!exec(getBuildDirectory(), 600000, cmd.toArray(new String[0])) || !signed.isFile()) {
                throw new BuildException("MSIX signing failed (osslsigncode; version 2.6+ is needed "
                        + "for the MSIX format). Ensure osslsigncode is on PATH (CN1_OSSLSIGNCODE "
                        + "overrides) and the certificate/password are valid. Note: the certificate "
                        + "Subject must equal the manifest Publisher (windows.msix.publisher).");
            }
            if (!msix.delete() || !signed.renameTo(msix)) {
                copy(signed, msix);
                signed.delete();
            }
            log("Signed " + msix.getName());
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("MSIX signing failed (osslsigncode)", ex);
        }
    }

    /** Pads/truncates a version string to the four numeric parts MSIX requires. */
    static String normalizeMsixVersion(String version) {
        String[] parts = (version == null ? "1.0.0.0" : version).split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                sb.append('.');
            }
            String part = i < parts.length ? parts[i].trim() : "0";
            boolean numeric = part.length() > 0;
            for (int j = 0; j < part.length(); j++) {
                if (!Character.isDigit(part.charAt(j))) {
                    numeric = false;
                    break;
                }
            }
            sb.append(numeric ? part : "0");
        }
        return sb.toString();
    }

    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
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
        // Classpath = app classes + Windows port classes + every JAR
        // on the maven plugin's own classloader (which includes
        // codenameone-core via the plugin's pom dependency on it).
        // System.getProperty("java.class.path") only sees Maven's
        // launcher jar -- not the plugin's resolved deps -- so we
        // have to walk the plugin classloader explicitly. Without
        // core on cp the stub compile fails with "cannot access
        // CodenameOneImplementation" / "package com.codename1.ui
        // does not exist".
        String cp = buildStubCompileClasspath(classesDir, portClasses);
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

    /** The clang-cl executable: {@code CN1_CLANG_CL} if set, else {@code clang-cl} on PATH. */
    private static String clangClExecutable() {
        String c = System.getenv("CN1_CLANG_CL");
        return (c != null && !c.isEmpty()) ? c : "clang-cl";
    }

    /** The llvm-rc executable: {@code CN1_LLVM_RC} if set, else {@code llvm-rc} on PATH. */
    private static String llvmRcExecutable() {
        String c = System.getenv("CN1_LLVM_RC");
        return (c != null && !c.isEmpty()) ? c : "llvm-rc";
    }

    /** The xwin SDK arch subdirectory for a (normalised) target: {@code x86_64} / {@code aarch64}. */
    private static String sdkArchSubdir(String arch) {
        return ARCH_ARM64.equals(normalizeArch(arch)) ? "aarch64" : "x86_64";
    }

    /** The CMake {@code CMAKE_SYSTEM_PROCESSOR} value for a target: {@code AMD64} / {@code ARM64}. */
    private static String cmakeSystemProcessor(String arch) {
        return ARCH_ARM64.equals(normalizeArch(arch)) ? "ARM64" : "AMD64";
    }

    /**
     * Resolves the Windows SDK sysroot used to cross-compile on a non-Windows host:
     * the {@code windows.sdkRoot} build hint, else the {@code CN1_XWIN_SYSROOT}
     * environment variable. Must point at an {@code xwin splat} directory (i.e. one
     * containing {@code crt/include} and {@code sdk/include/um}).
     */
    private File resolveXwinSysroot(BuildRequest request) {
        String hint = request.getArg("windows.sdkRoot", null);
        String path = (hint != null && !hint.isEmpty()) ? hint : System.getenv("CN1_XWIN_SYSROOT");
        if (path == null || path.isEmpty()) {
            throw new BuildException("Building the native Windows target on a non-Windows host (e.g. the Linux build "
                    + "cloud) cross-compiles against a Windows SDK laid out by `xwin splat`. Point the windows.sdkRoot "
                    + "build hint or the CN1_XWIN_SYSROOT environment variable at that directory. See the \"Working "
                    + "with the native Windows port\" developer guide chapter.");
        }
        File root = new File(path);
        if (!new File(root, "crt/include").isDirectory() || !new File(root, "sdk/include/um").isDirectory()) {
            throw new BuildException("windows.sdkRoot / CN1_XWIN_SYSROOT does not look like an `xwin splat` output "
                    + "(missing crt/include or sdk/include/um): " + root.getAbsolutePath());
        }
        return root;
    }

    /**
     * Appends the clang-cl + xwin cross-compile flags to a CMake configure command
     * (mirrors {@code windows-cross-compile.yml} and the {@code crossCompilesWindows-
     * ExeWithXwin} test). {@code CMAKE_SYSTEM_NAME=Windows} makes the generated
     * CMakeLists' {@code if(WIN32)} Direct2D/DirectWrite link set activate; {@code
     * /imsvc} points clang-cl at the SDK headers; {@code lld-link} (via {@code
     * -fuse-ld=lld}) links against the SDK libs; {@code llvm-rc} gets the SDK include
     * the resource compiler would otherwise read from {@code %INCLUDE%} on Windows.
     */
    private void addCrossCompileConfigure(List<String> configure, String triple, String arch, File sys,
            String extraCompileFlags, String extraLinkFlags) {
        String a = sdkArchSubdir(arch);
        String inc = "--target=" + triple
                + " /imsvc " + new File(sys, "crt/include")
                + " /imsvc " + new File(sys, "sdk/include/ucrt")
                + " /imsvc " + new File(sys, "sdk/include/um")
                + " /imsvc " + new File(sys, "sdk/include/shared")
                + " /imsvc " + new File(sys, "sdk/include/winrt")
                + extraCompileFlags;
        String rcFlags = "-I " + new File(sys, "sdk/include/um")
                + " -I " + new File(sys, "sdk/include/shared")
                + " -I " + new File(sys, "crt/include")
                + " -I " + new File(sys, "sdk/include/ucrt");
        String linkFlags = "-fuse-ld=lld"
                + " /libpath:" + new File(sys, "crt/lib/" + a)
                + " /libpath:" + new File(sys, "sdk/lib/um/" + a)
                + " /libpath:" + new File(sys, "sdk/lib/ucrt/" + a)
                + extraLinkFlags;
        configure.add("-DCMAKE_SYSTEM_NAME=Windows");
        configure.add("-DCMAKE_SYSTEM_PROCESSOR=" + cmakeSystemProcessor(arch));
        // STATIC_LIBRARY so CMake's compiler-detection try_compile does not need a
        // full link before the cross flags are fully in effect.
        configure.add("-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY");
        configure.add("-DCMAKE_C_FLAGS=" + inc);
        configure.add("-DCMAKE_CXX_FLAGS=" + inc);
        configure.add("-DCMAKE_RC_COMPILER=" + llvmRcExecutable());
        configure.add("-DCMAKE_RC_FLAGS=" + rcFlags);
        configure.add("-DCMAKE_EXE_LINKER_FLAGS=" + linkFlags);
    }

    /**
     * Runs a CMake configure/build step. A cross build (non-Windows host) runs the
     * command directly -- clang-cl / lld-link / llvm-rc / ninja are on PATH and the
     * SDK reaches the compiler through the explicit flags, so no environment wrapper
     * is needed. A Windows host wraps the command in the Visual Studio developer
     * environment via {@link #runWindowsBuildStep}.
     */
    private boolean runBuildStep(File dir, String targetArch, boolean cross, List<String> command) throws Exception {
        if (cross) {
            return exec(dir, 1800000, command.toArray(new String[0]));
        }
        return runWindowsBuildStep(dir, targetArch, command);
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

    /**
     * Build the classpath used to compile the generated stub .java files.
     * Includes the app classes, the Windows port classes, and every JAR on
     * this maven plugin's own classloader -- the latter is how
     * codenameone-core (which provides the abstract
     * com.codename1.impl.CodenameOneImplementation base class) lands on cp.
     * The plugin declares core as a direct dependency in its pom.xml, so
     * its classloader has it; {@code System.getProperty("java.class.path")}
     * sees only Maven's launcher jar and is useless here.
     */
    private String buildStubCompileClasspath(File classesDir, File portClasses) {
        StringBuilder cp = new StringBuilder();
        cp.append(classesDir.getAbsolutePath())
          .append(File.pathSeparator)
          .append(portClasses.getAbsolutePath());
        ClassLoader cl = getClass().getClassLoader();
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        while (cl != null) {
            if (cl instanceof java.net.URLClassLoader) {
                for (java.net.URL u : ((java.net.URLClassLoader) cl).getURLs()) {
                    if ("file".equalsIgnoreCase(u.getProtocol())) {
                        try {
                            seen.add(new File(u.toURI()).getAbsolutePath());
                        } catch (Throwable ignored) {
                            /* malformed URL -- skip */
                        }
                    }
                }
            }
            cl = cl.getParent();
        }
        for (String p : seen) {
            cp.append(File.pathSeparator).append(p);
        }
        return cp.toString();
    }
}
