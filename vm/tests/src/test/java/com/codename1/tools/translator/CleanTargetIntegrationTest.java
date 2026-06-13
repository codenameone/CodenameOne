package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CleanTargetIntegrationTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void generatesRunnableHelloWorldUsingCleanTarget(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("clean-target-sources");
        Path classesDir = Files.createTempDirectory("clean-target-classes");
        Path javaApiDir = Files.createTempDirectory("java-api-classes");
        Path javaFile = sourceDir.resolve("HelloWorld.java");
        Files.write(javaFile, helloWorldSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("native_hello.c"), nativeHelloSource().getBytes(StandardCharsets.UTF_8));

        List<String> compileArgs = new java.util.ArrayList<>();

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        if (CompilerHelper.useClasspath(config)) {
             // For CleanTarget, we are compiling java.lang classes.
             // On JDK 9+, rely on the JDK's bootstrap classes but include JavaAPI in classpath
             // so non-replaced classes are found.
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-classpath");
             compileArgs.add(javaApiDir.toString());
        } else {
             compileArgs.add("-source");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-target");
             compileArgs.add(config.targetVersion);
             compileArgs.add("-bootclasspath");
             compileArgs.add(javaApiDir.toString());
             compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(javaFile.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "HelloWorld.java should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Files.copy(sourceDir.resolve("native_hello.c"), classesDir.resolve("native_hello.c"));

        Path outputDir = Files.createTempDirectory("clean-target-output");
        runTranslator(classesDir, outputDir, "HelloCleanApp");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        Path srcRoot = distDir.resolve("HelloCleanApp-src");

        replaceLibraryWithExecutableTarget(cmakeLists, srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        runCommand(configure, distDir);

        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("HelloCleanApp"));
        String output = runCommand(Arrays.asList(executable.toString()), buildDir);

        assertTrue(output.contains("Hello, Clean Target!"),
                "Compiled program should print hello message, actual output was:\n" + output);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void generatesRunnableExecutableForWindowsAppType(CompilerHelper.CompilerConfig config) throws Exception {
        Parser.cleanup();

        Path sourceDir = Files.createTempDirectory("windows-target-sources");
        Path classesDir = Files.createTempDirectory("windows-target-classes");
        Path javaApiDir = Files.createTempDirectory("windows-java-api-classes");
        Path javaFile = sourceDir.resolve("HelloWorld.java");
        Files.write(javaFile, helloWorldSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("native_hello.c"), nativeHelloSource().getBytes(StandardCharsets.UTF_8));

        List<String> compileArgs = new java.util.ArrayList<>();

        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        CompilerHelper.compileJavaAPI(javaApiDir, config);

        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-source");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-target");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-classpath");
            compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-source");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-target");
            compileArgs.add(config.targetVersion);
            compileArgs.add("-bootclasspath");
            compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }

        compileArgs.add("-d");
        compileArgs.add(classesDir.toString());
        compileArgs.add(javaFile.toString());

        int compileResult = CompilerHelper.compile(config.jdkHome, compileArgs);
        assertEquals(0, compileResult, "HelloWorld.java should compile with " + config);

        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.copy(sourceDir.resolve("native_hello.c"), classesDir.resolve("native_hello.c"));

        Path outputDir = Files.createTempDirectory("windows-target-output");
        // The "windows" app type makes the translator emit add_executable() with the
        // platform link libraries directly, so unlike the other clean-target tests we
        // do NOT post-process the CMakeLists with replaceLibraryWithExecutableTarget.
        runTranslator(classesDir, outputDir, "WinCleanApp", "windows");

        Path distDir = outputDir.resolve("dist");
        Path cmakeLists = distDir.resolve("CMakeLists.txt");
        assertTrue(Files.exists(cmakeLists), "Translator should emit a CMake project");

        String cmake = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        assertTrue(cmake.contains("add_executable(${PROJECT_NAME}"),
                "windows app type should emit an executable target, was:\n" + cmake);
        assertFalse(cmake.contains("add_library(${PROJECT_NAME}"),
                "windows app type should not emit a library target, was:\n" + cmake);
        assertTrue(cmake.contains("d2d1") && cmake.contains("dwrite"),
                "windows app type should link the Direct2D/DirectWrite stack, was:\n" + cmake);

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);

        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake",
                "-S", distDir.toString(),
                "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release"));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        runCommand(configure, distDir);

        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path executable = buildDir.resolve(CompilerHelper.executableName("WinCleanApp"));
        String output = runCommand(Arrays.asList(executable.toString()), buildDir);

        assertTrue(output.contains("Hello, Clean Target!"),
                "Compiled program should print hello message, actual output was:\n" + output);
    }

    /**
     * Compiles the WindowsPort native layer (the hand-written Win32 / Direct2D /
     * DirectWrite / WIC / WinHTTP sources) inside a real translated "windows"
     * app-type dist, so the runtime headers (cn1_globals.h and the generated
     * cn1_class_method_index.h) are present. Windows-only: it shells out to
     * clang-cl, which the CI legs / dev VM put on PATH via the MSVC dev
     * environment. This is a compile check (clang-cl /c) -- linking the full app
     * is exercised separately once a CN1 app translation is wired.
     */
    @org.junit.jupiter.api.Test
    void compilesWindowsPortNativeLayer() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(CompilerHelper.isWindows(),
                "WindowsPort native layer is Windows-only");
        java.util.List<CompilerHelper.CompilerConfig> configs = new java.util.ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(configs.isEmpty(), "No JDK available to translate with");
        CompilerHelper.CompilerConfig config = configs.get(0);

        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("winnative-sources");
        Path classesDir = Files.createTempDirectory("winnative-classes");
        Path javaApiDir = Files.createTempDirectory("winnative-japi");
        Path javaFile = sourceDir.resolve("HelloWorld.java");
        Files.write(javaFile, helloWorldSource().getBytes(StandardCharsets.UTF_8));
        Files.write(sourceDir.resolve("native_hello.c"), nativeHelloSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.compileJavaAPI(javaApiDir, config);
        List<String> compileArgs = new java.util.ArrayList<>();
        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-source"); compileArgs.add(config.targetVersion);
            compileArgs.add("-target"); compileArgs.add(config.targetVersion);
            compileArgs.add("-classpath"); compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-source"); compileArgs.add(config.targetVersion);
            compileArgs.add("-target"); compileArgs.add(config.targetVersion);
            compileArgs.add("-bootclasspath"); compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d"); compileArgs.add(classesDir.toString());
        compileArgs.add(javaFile.toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs), "HelloWorld should compile");
        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.copy(sourceDir.resolve("native_hello.c"), classesDir.resolve("native_hello.c"));

        Path outputDir = Files.createTempDirectory("winnative-out");
        runTranslator(classesDir, outputDir, "WinNativeApp", "windows");
        Path srcRoot = outputDir.resolve("dist").resolve("WinNativeApp-src");
        assertTrue(Files.exists(srcRoot.resolve("cn1_globals.h")), "translated runtime header should exist");

        // Drop the WindowsPort native layer into the generated dist and compile
        // each file against the real runtime headers.
        Path nativeDir = Paths.get("..", "..", "Ports", "WindowsPort", "nativeSources").normalize().toAbsolutePath();
        String[] files = {
                "cn1_windows.h", "cn1_windows_comc.h", "cn1_windows_dwrite.h",
                "cn1_windows_window.cpp", "cn1_windows_graphics.cpp", "cn1_windows_text.c",
                "cn1_windows_image.cpp", "cn1_windows_io.c", "cn1_windows_net.c",
                "cn1_windows_dwrite.cpp", "cn1_windows_screenshot.cpp", "cn1_windows_simd.c",
                "cn1_windows_notify.c", "cn1_windows_audiorec.c", "cn1_windows_winrt.cpp",
                "cn1_windows_camera.cpp", "cn1_windows_peer.cpp", "cn1_windows_print.cpp"
        };
        for (String f : files) {
            Files.copy(nativeDir.resolve(f), srcRoot.resolve(f), StandardCopyOption.REPLACE_EXISTING);
        }
        Path objDir = Files.createTempDirectory("winnative-obj");
        List<String> failures = new java.util.ArrayList<>();

        // Probe: does the ParparVM runtime header compile as C++? Direct2D and
        // DirectWrite are C++-only, so the COM layer must be C++ and include
        // cn1_globals.h; this decides whether that is viable.
        Path probe = srcRoot.resolve("cn1_cpp_probe.cpp");
        Files.write(probe, ("extern \"C\" {\n#include \"cn1_globals.h\"\n}\nint cn1_cpp_probe_fn(void) { return 0; }\n")
                .getBytes(StandardCharsets.UTF_8));
        List<String> probeCmd = Arrays.asList("clang-cl", "/c", "/TP", "/std:c++17", "/W3",
                "/D_CRT_SECURE_NO_WARNINGS", "/I", srcRoot.toString(), probe.toString(),
                "/Fo" + objDir.resolve("cn1_cpp_probe.obj"));
        try {
            runCommand(probeCmd, srcRoot);
        } catch (Throwable t) {
            failures.add("===== cn1_globals.h as C++ =====\n" + t.getMessage());
        }
        for (String f : files) {
            if (!f.endsWith(".c") && !f.endsWith(".cpp")) {
                continue;
            }
            List<String> cmd = new java.util.ArrayList<>(Arrays.asList("clang-cl", "/c", "/W3", "/D_CRT_SECURE_NO_WARNINGS"));
            if (f.endsWith(".cpp")) {
                cmd.add("/TP");
                cmd.add("/std:c++17");
            } else {
                cmd.add("/std:c11");
            }
            cmd.add("/I");
            cmd.add(srcRoot.toString());
            cmd.add(srcRoot.resolve(f).toString());
            cmd.add("/Fo" + objDir.resolve(f + ".obj"));
            // Compile every file and collect failures so one run reports them all.
            try {
                runCommand(cmd, srcRoot);
            } catch (Throwable t) {
                failures.add("===== " + f + " =====\n" + t.getMessage());
            }
        }
        assertTrue(failures.isEmpty(), "WindowsPort native compile failures:\n" + String.join("\n", failures));
    }

    /**
     * End-to-end "builder + screenshot" test: compiles a tiny app that drives the
     * WindowsNative bridge, translates it with the "windows" app type together
     * with the WindowsPort native layer, links the executable with clang-cl, runs
     * it headless to render an offscreen Direct2D frame, and verifies the encoded
     * PNG (file present + expected pixels). Windows-only; needs clang-cl / cmake /
     * ninja on PATH via the MSVC dev environment.
     */
    @org.junit.jupiter.api.Test
    void rendersOffscreenToPngWithDirect2D() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(CompilerHelper.isWindows(),
                "Windows port rendering is Windows-only");
        java.util.List<CompilerHelper.CompilerConfig> configs = new java.util.ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(configs.isEmpty(), "No JDK available to translate with");
        CompilerHelper.CompilerConfig config = configs.get(0);

        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("winrender-sources");
        Path classesDir = Files.createTempDirectory("winrender-classes");
        Path javaApiDir = Files.createTempDirectory("winrender-japi");
        Files.write(sourceDir.resolve("WinGfxTest.java"), winGfxTestSource().getBytes(StandardCharsets.UTF_8));
        Path windowsNativeSrc = Paths.get("..", "..", "Ports", "WindowsPort", "src",
                "com", "codename1", "impl", "windows", "WindowsNative.java").normalize().toAbsolutePath();

        CompilerHelper.compileJavaAPI(javaApiDir, config);
        List<String> compileArgs = new java.util.ArrayList<>();
        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-source"); compileArgs.add(config.targetVersion);
            compileArgs.add("-target"); compileArgs.add(config.targetVersion);
            compileArgs.add("-classpath"); compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-source"); compileArgs.add(config.targetVersion);
            compileArgs.add("-target"); compileArgs.add(config.targetVersion);
            compileArgs.add("-bootclasspath"); compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d"); compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("WinGfxTest.java").toString());
        compileArgs.add(windowsNativeSrc.toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs), "WinGfxTest + WindowsNative should compile");
        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        // Bring the native layer into the translation unit so the clean target
        // globs and links it.
        Path nativeDir = Paths.get("..", "..", "Ports", "WindowsPort", "nativeSources").normalize().toAbsolutePath();
        try (java.util.stream.Stream<Path> s = Files.list(nativeDir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isRegularFile(p)) {
                    Files.copy(p, classesDir.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Path outputDir = Files.createTempDirectory("winrender-out");
        runTranslator(classesDir, outputDir, "WinGfxTest", "windows");
        Path distDir = outputDir.resolve("dist");

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release", "-G", "Ninja",
                "-DCMAKE_C_COMPILER=clang-cl", "-DCMAKE_CXX_COMPILER=clang-cl"));
        runCommand(configure, distDir);
        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path exe = buildDir.resolve(CompilerHelper.executableName("WinGfxTest"));
        // The app writes cn1_render.png into its working directory.
        String out = runCommand(Arrays.asList(exe.toString()), buildDir);
        Path png = buildDir.resolve("cn1_render.png");
        assertTrue(out.contains("RENDER_OK"), "render app should report success, output:\n" + out);
        assertTrue(Files.exists(png), "a PNG frame should be written");

        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(png.toFile());
        assertNotNull(img, "PNG should decode");
        assertEquals(400, img.getWidth());
        assertEquals(300, img.getHeight());
        int red = img.getRGB(120, 90) & 0xffffff;   // inside the filled red rect
        int bg = img.getRGB(5, 5) & 0xffffff;        // white background
        assertTrue(((red >> 16) & 0xff) > 180 && (red & 0xff) < 80,
                "expected a red pixel inside the rect, was " + Integer.toHexString(red));
        assertTrue(bg > 0xf0f0f0, "expected white background, was " + Integer.toHexString(bg));
    }

    /**
     * Builds a real Codename One Form app through the native Windows path:
     * compiles a minimal app (Display.init + a Form) against the full
     * codename1-core, translates app + core + WindowsPort + JavaAPI +
     * nativeSources with the "windows" app type, and links the executable with
     * clang-cl. This proves the builder pipeline on an actual Display/Form app
     * (not just the bridge). Windows-only; core/port classes are read from the
     * reactor's target/classes (reachable in the build VM via the shared repo).
     */
    @org.junit.jupiter.api.Test
    void buildsFullFormAppNative() throws Exception {
        Path exe = buildWindowsNativeExe("WinFormApp", winFormAppSource());
        // Always surface the freshly-built exe so it can be launched even if the
        // stable copy below is blocked by a running instance.
        System.out.println("CN1_WINFORM_TEMP_EXE=" + exe.toAbsolutePath());
        // Copy to a stable location so the app can be launched and tried; a
        // running instance locks it, so the copy is best-effort (non-fatal).
        try {
            Path dest = Paths.get(System.getProperty("user.home"), "cn1-winform");
            Files.createDirectories(dest);
            Path destExe = dest.resolve(CompilerHelper.executableName("WinFormApp"));
            Files.copy(exe, destExe, StandardCopyOption.REPLACE_EXISTING);
            Path pdb = exe.resolveSibling("WinFormApp.pdb");
            if (Files.exists(pdb)) {
                Files.copy(pdb, dest.resolve("WinFormApp.pdb"), StandardCopyOption.REPLACE_EXISTING);
            }
            Path theme = exe.resolveSibling("windowsNativeTheme.res");
            if (Files.exists(theme)) {
                Files.copy(theme, dest.resolve("windowsNativeTheme.res"), StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("CN1_WINFORM_EXE=" + destExe.toAbsolutePath());
        } catch (java.io.IOException copyBlocked) {
            System.out.println("CN1_WINFORM_EXE_COPY_SKIPPED=" + copyBlocked.getMessage());
        }
    }

    /**
     * Builds the Form app in headless screenshot mode and runs it: the app
     * renders the UI into an offscreen Direct2D/WIC bitmap (no window) and, once
     * it has painted, writes a PNG and exits. Asserts a non-trivial PNG is
     * produced -- the deterministic capture the Windows CI posts to the PR.
     */
    @org.junit.jupiter.api.Test
    void capturesFormScreenshotHeadless() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(CompilerHelper.isWindows(),
                "Native Windows headless capture is Windows-only");
        Path shot = Files.createTempDirectory("winshot-out").resolve("cn1-windows-native.png");
        // Forward slashes: accepted by the Win32 file APIs and safe to embed in
        // a Java string literal (no backslash escaping).
        String shotPath = shot.toAbsolutePath().toString().replace('\\', '/');
        Path exe = buildWindowsNativeExe("WinShotApp", winShotAppSource(shotPath, 420, 640));
        // The headless app exits itself once the screenshot is written.
        runCommand(Arrays.asList(exe.toAbsolutePath().toString()), exe.getParent());
        assertTrue(Files.exists(shot), "headless run should produce a screenshot PNG: " + shot);
        assertTrue(Files.size(shot) > 1024, "screenshot PNG should be non-trivial, was " + Files.size(shot) + " bytes");
        // Publish for the CI screenshot-comment step (env override) or default cwd.
        String outDir = System.getenv("CN1_SHOT_OUTPUT_DIR");
        Path dest = (outDir != null ? Paths.get(outDir) : Paths.get(System.getProperty("user.home"), "cn1-winform"));
        Files.createDirectories(dest);
        Files.copy(shot, dest.resolve("cn1-windows-native.png"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("CN1_WINFORM_SHOT=" + dest.resolve("cn1-windows-native.png").toAbsolutePath());
    }

    /**
     * Compiles a single-class app against the full codename1-core + WindowsPort,
     * translates app + core + port + JavaAPI + nativeSources with the "windows"
     * app type, and links the executable with clang-cl. Returns the built exe.
     * Skips (assumption) when not on Windows or when core/port are not built.
     */
    static Path buildWindowsNativeExe(String appName, String source) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(CompilerHelper.isWindows(),
                "Native Windows build is Windows-only");
        Path coreClasses = Paths.get("..", "..", "maven", "core", "target", "classes").normalize().toAbsolutePath();
        Path portClasses = Paths.get("..", "..", "maven", "windows", "target", "classes").normalize().toAbsolutePath();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(coreClasses.resolve("com/codename1/ui/Form.class")),
                "codenameone-core must be built (maven/core/target/classes)");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(portClasses.resolve("com/codename1/impl/windows/WindowsImplementation.class")),
                "WindowsPort must be built (maven/windows/target/classes)");

        java.util.List<CompilerHelper.CompilerConfig> configs = new java.util.ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(configs.isEmpty(), "No JDK available to translate with");
        CompilerHelper.CompilerConfig config = configs.get(0);

        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("winapp-sources");
        Path classesDir = Files.createTempDirectory("winapp-classes");
        Path javaApiDir = Files.createTempDirectory("winapp-japi");
        Files.write(sourceDir.resolve(appName + ".java"), source.getBytes(StandardCharsets.UTF_8));

        CompilerHelper.compileJavaAPI(javaApiDir, config);
        // The app is compiled against the full core (CN1 API) + the JDK (java.*).
        List<String> appCompile = new java.util.ArrayList<>(Arrays.asList(
                "-source", config.targetVersion, "-target", config.targetVersion,
                "-classpath", coreClasses + java.io.File.pathSeparator + portClasses,
                "-d", classesDir.toString(), sourceDir.resolve(appName + ".java").toString()));
        assertEquals(0, CompilerHelper.compile(config.jdkHome, appCompile),
                appName + " should compile against core + port:\n" + CompilerHelper.getLastErrorLog());

        // Stage the native layer into a folder the translator copies into srcRoot.
        Path nativeDir = Paths.get("..", "..", "Ports", "WindowsPort", "nativeSources").normalize().toAbsolutePath();
        Path nativeStage = Files.createTempDirectory("winapp-native");
        try (java.util.stream.Stream<Path> s = Files.list(nativeDir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isRegularFile(p)) {
                    Files.copy(p, nativeStage.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Path outputDir = Files.createTempDirectory("winapp-out");
        // Source roots: app classes, full core, WindowsPort classes, JavaAPI, native sources.
        String sources = classesDir + ";" + coreClasses + ";" + portClasses + ";" + javaApiDir + ";" + nativeStage;
        // The "windows" app type binds CodenameOneImplementation to its @Concrete
        // win() target (WindowsImplementation) during translation -- no override.
        runTranslatorMultiSource(sources, outputDir, appName, "windows");

        Path cmakeRoot = outputDir.resolve("dist");
        assertTrue(Files.exists(cmakeRoot.resolve("CMakeLists.txt")), "translator should emit a CMake project");
        Path buildDir = cmakeRoot.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new java.util.ArrayList<>(Arrays.asList(
                "cmake", "-S", cmakeRoot.toString(), "-B", buildDir.toString(),
                // RelWithDebInfo so clang-cl emits a .pdb; a crash address is
                // symbolized via (addr - module base) with llvm-symbolizer.
                "-DCMAKE_BUILD_TYPE=RelWithDebInfo", "-G", "Ninja",
                "-DCMAKE_C_COMPILER=clang-cl", "-DCMAKE_CXX_COMPILER=clang-cl"));
        runCommand(configure, cmakeRoot);
        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), cmakeRoot);
        Path exe = buildDir.resolve(CompilerHelper.executableName(appName));
        assertTrue(Files.exists(exe), "native executable should be produced: " + exe);
        // Ship the port's native material theme next to the exe so the Windows
        // impl can load it (the clean target has no embedded classpath resources).
        Path theme = Paths.get("..", "..", "Themes", "AndroidMaterialTheme.res").normalize().toAbsolutePath();
        if (Files.exists(theme)) {
            Files.copy(theme, exe.resolveSibling("windowsNativeTheme.res"), StandardCopyOption.REPLACE_EXISTING);
        }
        return exe;
    }

    /**
     * Cross-compiles a native Windows {@code .exe} on a non-Windows host (the Linux
     * CI pre-check, or a macOS dev box) using clang-cl + lld-link against a Windows
     * SDK laid out by <a href="https://github.com/Jake-Shadle/xwin">xwin</a>. clang
     * is a cross-compiler, so the binary it emits for {@code x86_64-pc-windows-msvc}
     * is the same regardless of the host -- this validates that the port + a real
     * Form app translate and *link* into a Windows PE off-Windows. It is compile-
     * and-link only: the PE cannot run here (Direct2D/DirectWrite need a real
     * Windows GPU stack), so there is no run/screenshot step.
     *
     * <p>Gated on {@code CN1_XWIN_SYSROOT} pointing at an {@code xwin splat}
     * directory; skipped (assumption) otherwise, and skipped on Windows (the native
     * path is covered by the other tests). {@code CN1_CLANG_CL} overrides the
     * clang-cl path (default {@code clang-cl} on PATH); lld-link must be on PATH.</p>
     */
    @org.junit.jupiter.api.Test
    void crossCompilesWindowsExeWithXwin() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(CompilerHelper.isWindows(),
                "Cross-compile check is for non-Windows hosts; Windows uses the native path");
        String sysroot = System.getenv("CN1_XWIN_SYSROOT");
        org.junit.jupiter.api.Assumptions.assumeTrue(sysroot != null && !sysroot.trim().isEmpty(),
                "Set CN1_XWIN_SYSROOT to an `xwin splat` directory to run the cross-compile check");
        Path sys = Paths.get(sysroot.trim()).toAbsolutePath();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(sys.resolve("crt/include")),
                "CN1_XWIN_SYSROOT must contain crt/include + sdk/include (run `xwin splat`): " + sys);
        String clangCl = System.getenv().getOrDefault("CN1_CLANG_CL", "clang-cl");

        Path coreClasses = Paths.get("..", "..", "maven", "core", "target", "classes").normalize().toAbsolutePath();
        Path portClasses = Paths.get("..", "..", "maven", "windows", "target", "classes").normalize().toAbsolutePath();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(coreClasses.resolve("com/codename1/ui/Form.class")),
                "codenameone-core must be built (maven/core/target/classes)");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(portClasses.resolve("com/codename1/impl/windows/WindowsImplementation.class")),
                "WindowsPort must be built (maven/windows/target/classes)");

        java.util.List<CompilerHelper.CompilerConfig> configs = new java.util.ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(configs.isEmpty(), "No JDK available to translate with");
        CompilerHelper.CompilerConfig config = configs.get(0);

        // --- translate (host-agnostic): compile a real Form app against core+port,
        // stage the native layer, run the translator with the "windows" app type ---
        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("xwin-src");
        Path classesDir = Files.createTempDirectory("xwin-cls");
        Path javaApiDir = Files.createTempDirectory("xwin-japi");
        Files.write(sourceDir.resolve("WinFormApp.java"), winFormAppSource().getBytes(StandardCharsets.UTF_8));
        CompilerHelper.compileJavaAPI(javaApiDir, config);
        assertEquals(0, CompilerHelper.compile(config.jdkHome, Arrays.asList(
                "-source", config.targetVersion, "-target", config.targetVersion,
                "-classpath", coreClasses + java.io.File.pathSeparator + portClasses,
                "-d", classesDir.toString(), sourceDir.resolve("WinFormApp.java").toString())),
                "WinFormApp should compile:\n" + CompilerHelper.getLastErrorLog());
        Path nativeDir = Paths.get("..", "..", "Ports", "WindowsPort", "nativeSources").normalize().toAbsolutePath();
        Path nativeStage = Files.createTempDirectory("xwin-native");
        try (java.util.stream.Stream<Path> s = Files.list(nativeDir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isRegularFile(p)) {
                    Files.copy(p, nativeStage.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Path outputDir = Files.createTempDirectory("xwin-out");
        String sources = classesDir + ";" + coreClasses + ";" + portClasses + ";" + javaApiDir + ";" + nativeStage;
        runTranslatorMultiSource(sources, outputDir, "WinFormApp", "windows");
        Path cmakeRoot = outputDir.resolve("dist");
        assertTrue(Files.exists(cmakeRoot.resolve("CMakeLists.txt")), "translator should emit a CMake project");

        // --- cross-compile that dist to a Windows x64 PE with clang-cl + xwin ---
        Path exe = crossBuildDist(cmakeRoot, sys, clangCl, "WinFormApp");
        System.out.println("CN1_XWIN_EXE=" + exe.toAbsolutePath() + " (" + (Files.size(exe) / 1024) + "KB)");
    }

    /**
     * Cross-compiles a translator-emitted CMake dist to a Windows x64 PE with
     * clang-cl + lld-link + llvm-rc against an xwin-laid-out Windows SDK ({@code
     * sys}). Shared by the link-only smoke test (crossCompilesWindowsExeWithXwin)
     * and the full-suite cross-build (crossBuildsHelloSuiteExe). Returns the
     * produced exe and asserts it is a non-trivial PE. The cmake/clang-cl
     * subprocess inherits this process's environment, so WEBVIEW2_SDK_DIR (when the
     * CI fetched the SDK) flows into the generated CMake and links the
     * BrowserComponent peer; otherwise the browser natives compile as stubs.
     */
    static Path crossBuildDist(Path cmakeRoot, Path sys, String clangCl, String exeName) throws Exception {
        String target = "x86_64-pc-windows-msvc";
        String libArch = "x86_64";
        String inc = String.join(" ",
                "--target=" + target,
                imsvc(sys.resolve("crt/include")),
                imsvc(sys.resolve("sdk/include/ucrt")),
                imsvc(sys.resolve("sdk/include/um")),
                imsvc(sys.resolve("sdk/include/shared")),
                imsvc(sys.resolve("sdk/include/winrt")));
        String linkFlags = String.join(" ",
                "-fuse-ld=lld",
                "/libpath:" + sys.resolve("crt/lib/" + libArch),
                "/libpath:" + sys.resolve("sdk/lib/um/" + libArch),
                "/libpath:" + sys.resolve("sdk/lib/ucrt/" + libArch));
        // The resource compiler (llvm-rc) needs the SDK include path too: on a real
        // Windows host it comes from %INCLUDE% (set by vcvarsall), which we do not
        // have here, so cn1_resources.rc's `#include <windows.h>` fails without it.
        String rcFlags = String.join(" ",
                "-I", sys.resolve("sdk/include/um").toString(),
                "-I", sys.resolve("sdk/include/shared").toString(),
                "-I", sys.resolve("crt/include").toString(),
                "-I", sys.resolve("sdk/include/ucrt").toString());
        String llvmRc = System.getenv().getOrDefault("CN1_LLVM_RC", "llvm-rc");
        Path buildDir = cmakeRoot.resolve("xbuild");
        Files.createDirectories(buildDir);
        runCommand(Arrays.asList("cmake", "-S", cmakeRoot.toString(), "-B", buildDir.toString(),
                "-G", "Ninja",
                "-DCMAKE_SYSTEM_NAME=Windows",
                "-DCMAKE_SYSTEM_PROCESSOR=AMD64",
                // STATIC_LIBRARY so CMake's compiler-detection try_compile does not
                // need a full link before the toolchain flags are fully applied.
                "-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY",
                "-DCMAKE_C_COMPILER=" + clangCl,
                "-DCMAKE_CXX_COMPILER=" + clangCl,
                "-DCMAKE_BUILD_TYPE=Release",
                "-DCMAKE_C_FLAGS=" + inc,
                "-DCMAKE_CXX_FLAGS=" + inc,
                "-DCMAKE_RC_COMPILER=" + llvmRc,
                "-DCMAKE_RC_FLAGS=" + rcFlags,
                "-DCMAKE_EXE_LINKER_FLAGS=" + linkFlags), cmakeRoot);
        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), cmakeRoot);

        Path exe = buildDir.resolve(exeName + ".exe");
        assertTrue(Files.exists(exe), "cross-compiled Windows executable should be produced: " + exe);
        assertTrue(Files.size(exe) > 100_000, "PE should be non-trivial, was " + Files.size(exe) + " bytes");
        byte[] head = new byte[2];
        try (java.io.InputStream in = Files.newInputStream(exe)) {
            assertEquals(2, in.read(head), "should read PE header");
        }
        assertEquals('M', head[0] & 0xff, "exe should start with the PE 'MZ' magic");
        assertEquals('Z', head[1] & 0xff, "exe should start with the PE 'MZ' magic");
        return exe;
    }

    /**
     * Cross-compiles the FULL hellocodenameone screenshot suite (not just a Form
     * app) into a Windows x64 PE on a non-Windows host with clang-cl + xwin, and
     * writes it to CN1_CROSS_EXE_OUT so the CI can upload it as an artifact. The
     * companion Windows job downloads that exe and runs the screenshot suite
     * against it (capturesHelloSuiteOverWebSocket with CN1_PREBUILT_EXE) -- so the
     * binary that renders on Windows is the exact one cross-compiled on Linux,
     * mirroring the real cloud build pipeline (compile on Linux, run on the user's
     * Windows machine). When the CI fetched the WebView2 SDK (WEBVIEW2_SDK_DIR set)
     * the cross-build also compiles + links the BrowserComponent peer, so the run
     * proves a cross-compiled browser actually renders.
     *
     * Gated like crossCompilesWindowsExeWithXwin: a non-Windows host with
     * CN1_XWIN_SYSROOT pointing at an `xwin splat` directory, plus a built
     * core/port/common (translateHelloSuiteDist's assumptions). The CI builds those
     * before invoking this test.
     */
    @org.junit.jupiter.api.Test
    void crossBuildsHelloSuiteExe() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(CompilerHelper.isWindows(),
                "Cross-build is for non-Windows hosts; Windows uses the native path");
        String sysroot = System.getenv("CN1_XWIN_SYSROOT");
        org.junit.jupiter.api.Assumptions.assumeTrue(sysroot != null && !sysroot.trim().isEmpty(),
                "Set CN1_XWIN_SYSROOT to an `xwin splat` directory to run the cross-build");
        Path sys = Paths.get(sysroot.trim()).toAbsolutePath();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(sys.resolve("crt/include")),
                "CN1_XWIN_SYSROOT must contain crt/include + sdk/include (run `xwin splat`): " + sys);
        String clangCl = System.getenv().getOrDefault("CN1_CLANG_CL", "clang-cl");

        Path cmakeRoot = translateHelloSuiteDist();
        Path exe = crossBuildDist(cmakeRoot, sys, clangCl, "WinHelloMain");
        System.out.println("CN1_CROSS_SUITE_EXE=" + exe.toAbsolutePath() + " (" + (Files.size(exe) / 1024) + "KB)");

        // Hand the cross-built exe to the CI (artifact upload + the Windows run).
        String out = System.getenv("CN1_CROSS_EXE_OUT");
        if (out != null && !out.trim().isEmpty()) {
            Path dest = Paths.get(out.trim());
            if (dest.getParent() != null) { Files.createDirectories(dest.getParent()); }
            Files.copy(exe, dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Cross-built suite exe copied to " + dest.toAbsolutePath());
        }
    }

    /** clang-cl system-include flag for an xwin include dir ({@code /imsvc <dir>}). */
    static String imsvc(Path dir) {
        return "/imsvc " + dir;
    }

    /** Minimal extraction of a jar's class tree into a directory (a translator source root). */
    static void extractJar(Path jar, Path dest) throws Exception {
        try (java.util.zip.ZipInputStream zin = new java.util.zip.ZipInputStream(Files.newInputStream(jar))) {
            java.util.zip.ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.isDirectory()) { continue; }
                String name = e.getName();
                if (!name.endsWith(".class")) { continue; }
                Path out = dest.resolve(name);
                Files.createDirectories(out.getParent());
                Files.copy(zin, out, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /** Launcher main that drives the hellocodenameone Cn1ssDeviceRunner screenshot suite on the Windows port. */
    static String winHelloLauncherSource() {
        return "import com.codename1.ui.Display;\n" +
                "import com.codename1.testing.TestReporting;\n" +
                "import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunner;\n" +
                "import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunnerReporter;\n" +
                "public class WinHelloMain {\n" +
                "    static class Holder { int value; }\n" +
                "    static Holder nullHolder() { return null; }\n" +
                "    public static void main(String[] args) {\n" +
                "        Display.init(null);\n" +
                // Deterministic validation for the native fault->exception handler
                // (cn1WinFaultToException). initDisplay (called synchronously by
                // Display.init above) installs the vectored handler, so a null deref
                // here must surface as a catchable NullPointerException with a Java
                // stack trace instead of a hard EXCEPTION_ACCESS_VIOLATION. A getfield
                // on null is used because the clean target does not emit an NPE check
                // for field derefs, so it genuinely faults and exercises the handler.
                // Gated on an env var so it never affects normal/CI screenshot runs.
                "        if (com.codename1.impl.windows.WindowsNative.faultSelfTestEnabled()) {\n" +
                "            try {\n" +
                "                Holder h = nullHolder();\n" +
                "                com.codename1.impl.windows.WindowsNative.nativeLog(\"CN1_FAULT_SELFTEST: pre-deref value=\" + (h.value + 1));\n" +
                "                com.codename1.impl.windows.WindowsNative.nativeLog(\"CN1_FAULT_SELFTEST: NO_EXCEPTION_BAD\");\n" +
                "            } catch (Throwable t) {\n" +
                "                com.codename1.impl.windows.WindowsNative.nativeLog(\"CN1_FAULT_SELFTEST: caught \" + t.getClass().getName() + \": \" + t.getMessage());\n" +
                "                t.printStackTrace();\n" +
                "            }\n" +
                "            return;\n" +
                "        }\n" +
                // KotlinUiTest reaches kotlin.Unit only through its lambdas' return
                // type, which the translator's reachability closure does not follow,
                // so kotlin.Unit is never translated and KotlinUiTest.c fails to
                // compile (kotlin_Unit.h not found). A direct static-field reference
                // forces it into the translation set (the Kotlin app entry point does
                // this implicitly on the other ports).
                "        if (kotlin.Unit.INSTANCE == null) { return; }\n" +
                // Register the transcoded SVG/Lottie images (generated by the
                // TranscoderCli step in CI into com.codename1.generated.svg) so the
                // theme's url(*.svg) backgrounds and Resources.getImage(name) resolve
                // to the real GeneratedSVGImage instead of the 1x1 placeholder. On
                // device this lives in the per-build Stub (IPhoneBuilder /
                // AndroidGradleBuilder emit it); the launcher is that stub here. The
                // direct reference also forces the translator to retain the package.
                // MUST run before initFirstTheme so the Style backgrounds bind while
                // the theme is built.
                "        try { com.codename1.generated.svg.SVGRegistry.installGlobal(); } catch (Throwable __svg) { __svg.printStackTrace(); }\n" +
                // Load the app theme (theme.css compiled to /theme.res). It carries
                // @includeNativeBool:true, so buildTheme installs the Windows native
                // theme (the full material look: styled checkbox/switch/tabs/text
                // field/dialog/FAB + $Dark variants) as the base and layers the
                // app's gradient/SVG/Button UIIDs on top. Without this the suite
                // renders on the bare app theme (no component styling, no dark mode).
                // initFirstTheme also sets the global Resources so getImage() works.
                "        com.codename1.ui.plaf.UIManager.initFirstTheme(\"/theme\");\n" +
                "        TestReporting.setInstance(new Cn1ssDeviceRunnerReporter());\n" +
                // KotlinUiTest is registered as the prepended test exactly as the
                // real Kotlin app entry point does (HelloCodenameOne.kt:
                // Cn1ssDeviceRunner.addTest(KotlinUiTest())); it is not in
                // DEFAULT_TEST_CLASSES, so without this the kotlin screenshot is
                // never produced on the native Windows clean target.
                "        Cn1ssDeviceRunner.addTest(new com.codenameone.examples.hellocodenameone.tests.KotlinUiTest());\n" +
                // runSuite blocks polling each test; it must run off the EDT (it
                // navigates forms via callSerially). The main thread owns the Win32
                // pump (runMainEventLoop) so the EDT is woken to lay out + paint +
                // emit each screenshot over the cn1ss WebSocket.
                "        new Thread(new Runnable() {\n" +
                "            public void run() { new Cn1ssDeviceRunner().runSuite(); }\n" +
                "        }, \"CN1SS-Runner\").start();\n" +
                "        com.codename1.impl.windows.WindowsImplementation.runMainEventLoop();\n" +
                "    }\n" +
                "}\n";
    }

    /**
     * Builds the real hellocodenameone screenshot suite as a native Windows exe:
     * stages hellocodenameone-common (the Kotlin/Java app + all *ScreenshotTest
     * classes) + the Kotlin stdlib + core + the Windows port + JavaAPI, compiles a
     * launcher that runs Cn1ssDeviceRunner, translates it with the "windows" app
     * type and clang-cl-builds it. Returns the exe.
     */
    static Path buildHelloCodenameOneExe() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(CompilerHelper.isWindows(),
                "Native Windows build is Windows-only");
        Path cmakeRoot = translateHelloSuiteDist();
        Path buildDir = cmakeRoot.resolve("build");
        Files.createDirectories(buildDir);
        // Native Windows build with the MSVC clang-cl on PATH (the dev env / CI
        // sets it up). RelWithDebInfo so a native crash address symbolizes.
        runCommand(Arrays.asList("cmake", "-S", cmakeRoot.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=RelWithDebInfo", "-G", "Ninja",
                "-DCMAKE_C_COMPILER=clang-cl", "-DCMAKE_CXX_COMPILER=clang-cl"), cmakeRoot);
        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), cmakeRoot);
        Path exe = buildDir.resolve(CompilerHelper.executableName("WinHelloMain"));
        assertTrue(Files.exists(exe), "native executable should be produced: " + exe);
        // The single executable is fully self-contained: its classpath resources
        // (theme.res, windowsNativeTheme.res, material-design-font.ttf) are embedded
        // in the exe's PE resource section by the windows translator target and read
        // via getResourceAsStream -- nothing is staged next to the exe.
        return exe;
    }

    /**
     * Translates the real hellocodenameone screenshot suite (the Kotlin/Java app +
     * every *ScreenshotTest, the Kotlin stdlib, core, the Windows port, JavaAPI and
     * the port's nativeSources) into a CMake dist with the "windows" app type, and
     * returns the dist root (the CMake project). Host-agnostic -- pure Java
     * translation, no native toolchain -- so it backs both the native Windows build
     * (buildHelloCodenameOneExe) and the Linux xwin cross-build
     * (crossBuildsHelloSuiteExe).
     */
    static Path translateHelloSuiteDist() throws Exception {
        Path coreClasses = Paths.get("..", "..", "maven", "core", "target", "classes").normalize().toAbsolutePath();
        Path portClasses = Paths.get("..", "..", "maven", "windows", "target", "classes").normalize().toAbsolutePath();
        Path commonClasses = Paths.get("..", "..", "scripts", "hellocodenameone", "common", "target", "classes")
                .normalize().toAbsolutePath();
        // cn1-ads-mock is a Maven dependency of the app (used by AdsScreenshotTest),
        // not compiled into common/target/classes, so add its classes as a source
        // root or the translator can't emit MockAdProvider and the C build fails on
        // its missing header. Built by the CI install step alongside core + plugin.
        Path adsMockClasses = Paths.get("..", "..", "maven", "cn1-ads-mock", "target", "classes")
                .normalize().toAbsolutePath();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(coreClasses.resolve("com/codename1/ui/Form.class")),
                "codenameone-core must be built (maven/core/target/classes)");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(portClasses.resolve("com/codename1/impl/windows/WindowsImplementation.class")),
                "WindowsPort must be built (maven/windows/target/classes)");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                Files.exists(commonClasses.resolve("com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.class")),
                "hellocodenameone-common must be built (scripts/hellocodenameone/common/target/classes)");

        java.util.List<CompilerHelper.CompilerConfig> configs = new java.util.ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(configs.isEmpty(), "No JDK available to translate with");
        CompilerHelper.CompilerConfig config = configs.get(0);

        // Stage the Kotlin stdlib (the app is Kotlin) into a translator source root.
        Path kotlinDir = Files.createTempDirectory("winhello-kotlin");
        Path m2 = Paths.get(System.getProperty("user.home"), ".m2", "repository", "org", "jetbrains", "kotlin");
        for (String[] ga : new String[][] {
                { "kotlin-stdlib", "1.6.0" }, { "kotlin-stdlib-jdk7", "1.6.0" }, { "kotlin-stdlib-jdk8", "1.6.0" },
                { "kotlin-stdlib-common", "1.6.0" }, { "kotlin-annotations-jvm", "1.6.0" } }) {
            Path jar = m2.resolve(ga[0]).resolve(ga[1]).resolve(ga[0] + "-" + ga[1] + ".jar");
            if (Files.exists(jar)) {
                extractJar(jar, kotlinDir);
            }
        }

        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("winhello-sources");
        Path classesDir = Files.createTempDirectory("winhello-classes");
        Path javaApiDir = Files.createTempDirectory("winhello-japi");
        Files.write(sourceDir.resolve("WinHelloMain.java"), winHelloLauncherSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.compileJavaAPI(javaApiDir, config);
        String cp = coreClasses + java.io.File.pathSeparator + portClasses
                + java.io.File.pathSeparator + commonClasses + java.io.File.pathSeparator + kotlinDir
                + java.io.File.pathSeparator + adsMockClasses;
        List<String> appCompile = new java.util.ArrayList<>(Arrays.asList(
                "-source", config.targetVersion, "-target", config.targetVersion,
                "-classpath", cp, "-d", classesDir.toString(),
                sourceDir.resolve("WinHelloMain.java").toString()));
        assertEquals(0, CompilerHelper.compile(config.jdkHome, appCompile),
                "WinHelloMain should compile:\n" + CompilerHelper.getLastErrorLog());

        Path nativeDir = Paths.get("..", "..", "Ports", "WindowsPort", "nativeSources").normalize().toAbsolutePath();
        Path nativeStage = Files.createTempDirectory("winhello-native");
        try (java.util.stream.Stream<Path> s = Files.list(nativeDir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isRegularFile(p)) {
                    Files.copy(p, nativeStage.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Path outputDir = Files.createTempDirectory("winhello-out");
        String sources = classesDir + ";" + commonClasses + ";" + kotlinDir + ";" + coreClasses + ";"
                + portClasses + ";" + javaApiDir + ";" + nativeStage + ";" + adsMockClasses;
        // The "windows" app type binds CodenameOneImplementation to its @Concrete
        // win() target (WindowsImplementation) during translation -- no override.
        runTranslatorMultiSource(sources, outputDir, "WinHelloMain", "windows");

        Path cmakeRoot = outputDir.resolve("dist");
        assertTrue(Files.exists(cmakeRoot.resolve("CMakeLists.txt")), "translator should emit a CMake project");
        return cmakeRoot;
    }

    /**
     * Builds the real hellocodenameone screenshot suite through the native Windows
     * port (translate + clang-cl link). This first milestone proves the full app +
     * Kotlin stdlib + reachable component graph translates and LINKS (every native
     * method it reaches has a C implementation); running the suite + capturing the
     * ~122 screenshots is a separate step once it links.
     */
    @org.junit.jupiter.api.Test
    void buildsHelloCodenameOneNative() throws Exception {
        Path exe = buildHelloCodenameOneExe();
        System.out.println("CN1_HELLO_EXE=" + exe.toAbsolutePath());
        java.nio.file.Path dest = Paths.get(System.getProperty("user.home"), "cn1-hello");
        Files.createDirectories(dest);
        try {
            Path d = dest.resolve("WinHelloMain.exe");
            Files.copy(exe, d, StandardCopyOption.REPLACE_EXISTING);
            Path pdb = exe.resolveSibling("WinHelloMain.pdb");
            if (Files.exists(pdb)) {
                Files.copy(pdb, dest.resolve("WinHelloMain.pdb"), StandardCopyOption.REPLACE_EXISTING);
            }
            for (String res : new String[] { "windowsNativeTheme.res", "theme.res", "material-design-font.ttf" }) {
                Path src = exe.resolveSibling(res);
                if (Files.exists(src)) {
                    Files.copy(src, dest.resolve(res), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println("CN1_HELLO_EXE_COPY=" + d.toAbsolutePath());
        } catch (IOException copyBlocked) {
            System.out.println("CN1_HELLO_EXE_COPY_SKIPPED=" + copyBlocked.getMessage());
        }
    }

    static String winFormAppSource() {
        // A small but realistic multi-screen Codename One app: a Toolbar-based
        // scrollable Contacts list (MultiButton rows) that navigates to a detail
        // form (with a toolbar Back command) and pops Dialogs. Exercises the real
        // CN1 navigation / toolbar / scrolling / modal-dialog paths through the
        // native Windows port, not just a single static form.
        return "import com.codename1.ui.Display;\n" +
                "import com.codename1.ui.Form;\n" +
                "import com.codename1.ui.Label;\n" +
                "import com.codename1.ui.Button;\n" +
                "import com.codename1.ui.Command;\n" +
                "import com.codename1.ui.Dialog;\n" +
                "import com.codename1.ui.Toolbar;\n" +
                "import com.codename1.components.MultiButton;\n" +
                "import com.codename1.ui.layouts.BoxLayout;\n" +
                "import com.codename1.ui.events.ActionListener;\n" +
                "import com.codename1.ui.events.ActionEvent;\n" +
                "public class WinFormApp {\n" +
                "    static String[] NAMES = {\"Ada Lovelace\",\"Alan Turing\",\"Grace Hopper\",\"Linus Torvalds\",\"Margaret Hamilton\",\"Dennis Ritchie\",\"Barbara Liskov\",\"Ken Thompson\",\"Donald Knuth\",\"Katherine Johnson\",\"Tim Berners-Lee\",\"Radia Perlman\",\"James Gosling\",\"Anita Borg\"};\n" +
                "    static String[] ROLES = {\"Mathematician, 1843\",\"Computability, 1936\",\"Rear Admiral, COBOL\",\"Linux kernel author\",\"Apollo guidance code\",\"C language, Unix\",\"CLU, data abstraction\",\"Unix co-author\",\"TAOCP and TeX\",\"NASA trajectories\",\"Invented the Web\",\"Spanning tree protocol\",\"Java language\",\"Systers founder\"};\n" +
                "    public static void main(String[] args) {\n" +
                "        Toolbar.setGlobalToolbar(true);\n" +
                "        Display.init(null);\n" +
                "        Display.getInstance().callSerially(new Runnable() {\n" +
                "            public void run() { showMain(); }\n" +
                "        });\n" +
                // Display.init started the EDT (which renders); the main thread now
                // owns the Win32 message loop + input dispatch so the window is
                // responsive and the EDT (asleep on the Display lock) gets woken.
                "        com.codename1.impl.windows.WindowsImplementation.runMainEventLoop();\n" +
                "    }\n" +
                "    static void showMain() {\n" +
                "        Form home = new Form(\"Contacts\", BoxLayout.y());\n" +
                "        home.setScrollableY(true);\n" +
                "        home.getToolbar().addCommandToRightBar(new Command(\"About\") {\n" +
                "            public void actionPerformed(ActionEvent e) {\n" +
                "                Dialog.show(\"About\", \"Codename One running as a real native Windows app -- ParparVM bytecode-to-C with Direct2D / DirectWrite rendering.\", \"Nice\", null);\n" +
                "            }\n" +
                "        });\n" +
                "        for (int i = 0; i < NAMES.length; i++) {\n" +
                "            final int idx = i;\n" +
                "            MultiButton mb = new MultiButton(NAMES[i]);\n" +
                "            mb.setTextLine2(ROLES[i]);\n" +
                "            mb.addActionListener(new ActionListener() {\n" +
                "                public void actionPerformed(ActionEvent e) { showDetail(idx); }\n" +
                "            });\n" +
                "            home.add(mb);\n" +
                "        }\n" +
                "        home.show();\n" +
                "    }\n" +
                "    static void showDetail(final int idx) {\n" +
                "        Form f = new Form(NAMES[idx], BoxLayout.y());\n" +
                "        f.getToolbar().addCommandToLeftBar(new Command(\"Back\") {\n" +
                "            public void actionPerformed(ActionEvent e) { showMain(); }\n" +
                "        });\n" +
                "        Label name = new Label(NAMES[idx]);\n" +
                "        name.getAllStyles().setFgColor(0x1565c0);\n" +
                "        f.add(name);\n" +
                "        f.add(new Label(ROLES[idx]));\n" +
                "        f.add(new Label(\"Reached by tapping a list row;\"));\n" +
                "        f.add(new Label(\"native navigation + toolbar back work.\"));\n" +
                "        Button greet = new Button(\"Say hi\");\n" +
                "        greet.addActionListener(new ActionListener() {\n" +
                "            public void actionPerformed(ActionEvent e) { Dialog.show(\"Hi\", \"Hello from \" + NAMES[idx] + \"!\", \"OK\", null); }\n" +
                "        });\n" +
                "        f.add(greet);\n" +
                "        f.show();\n" +
                "    }\n" +
                "}\n";
    }

    /** Headless variant: enables screenshot capture, then builds the same UI. */
    static String winShotAppSource(String pngPath, int width, int height) {
        return "import com.codename1.ui.Display;\n" +
                "import com.codename1.ui.Form;\n" +
                "import com.codename1.ui.Label;\n" +
                "import com.codename1.ui.Button;\n" +
                "import com.codename1.ui.layouts.BoxLayout;\n" +
                "import com.codename1.impl.windows.WindowsNative;\n" +
                "public class WinShotApp {\n" +
                "    public static void main(String[] args) {\n" +
                "        WindowsNative.enableHeadlessScreenshot(\"" + pngPath + "\", " + width + ", " + height + ");\n" +
                "        Display.init(null);\n" +
                "        Display.getInstance().callSerially(new Runnable() {\n" +
                "            public void run() {\n" +
                "                Form f = new Form(\"CN1 Native\", BoxLayout.y());\n" +
                "                f.add(new Label(\"Hello from the native Windows port!\"));\n" +
                "                f.add(new Button(\"Click me\"));\n" +
                "                f.show();\n" +
                "            }\n" +
                "        });\n" +
                // Park the main thread so the process stays alive; the EDT writes
                // the PNG and exits the process once the UI has painted.
                "        WindowsNative.runHeadlessLoop();\n" +
                "    }\n" +
                "}\n";
    }

    /**
     * Full native-Windows screenshot suite over the cn1ss WebSocket: builds the
     * hellocodenameone exe (the same one scripts/windows/run-hello.bat runs),
     * starts a Cn1ssScreenshotServer on 8765 (the port the device runner defaults
     * to), launches the exe, and waits for the CN1SS:SUITE:FINISHED marker on its
     * stdout before tearing the process down. Asserts the suite finished and that
     * a healthy number of PNGs landed. This is the CI counterpart of run-hello.bat.
     *
     * Skipped (assumeTrue) unless hellocodenameone-common is built; the CI job
     * builds it before invoking this test.
     */
    @org.junit.jupiter.api.Test
    void capturesHelloSuiteOverWebSocket() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(CompilerHelper.isWindows(),
                "Native Windows websocket capture is Windows-only");
        // When CN1_PREBUILT_EXE points at an already-built suite exe -- the Windows
        // job that runs the Linux cross-compiled artifact -- run that one instead of
        // building here. That runner only needs a JDK + the exe (the cn1ss server is
        // Java), not core/port/common or the native toolchain. Otherwise build the
        // suite exe natively on this Windows host.
        Path exe;
        String prebuilt = System.getenv("CN1_PREBUILT_EXE");
        if (prebuilt != null && !prebuilt.trim().isEmpty()) {
            exe = Paths.get(prebuilt.trim()).toAbsolutePath();
            org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(exe),
                    "CN1_PREBUILT_EXE does not exist: " + exe);
            System.out.println("Running prebuilt suite exe (cross-compiled): " + exe);
        } else {
            exe = buildHelloCodenameOneExe();
        }

        // Compile the shared cn1ss screenshot server with an available JDK.
        java.util.List<CompilerHelper.CompilerConfig> configs = new java.util.ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        CompilerHelper.CompilerConfig jdk = configs.get(0);
        Path serverSrc = Paths.get("..", "..", "scripts", "common", "java", "Cn1ssScreenshotServer.java")
                .normalize().toAbsolutePath();
        Path serverClasses = Files.createTempDirectory("cn1ss-server");
        assertEquals(0, CompilerHelper.compile(jdk.jdkHome, Arrays.asList(
                "-d", serverClasses.toString(), "-sourcepath", serverSrc.getParent().toString(),
                serverSrc.toString())), "Cn1ssScreenshotServer should compile");

        // The device runner defaults to ws://127.0.0.1:8765 (Cn1ssDeviceRunnerHelper).
        int port = 8765;
        Path outDir = Files.createTempDirectory("cn1ss-hello-out");
        String javaBin = jdk.jdkHome.resolve("bin").resolve(CompilerHelper.executableName("java")).toString();
        ProcessBuilder serverPb = new ProcessBuilder(javaBin, "-cp", serverClasses.toString(),
                "Cn1ssScreenshotServer", "--port", String.valueOf(port), "--out", outDir.toString());
        serverPb.redirectErrorStream(true);
        Process server = serverPb.start();
        Process app = null;
        try {
            final java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(1);
            final StringBuilder serverLog = new StringBuilder();
            Thread sreader = new Thread(new Runnable() {
                public void run() {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(
                            server.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            synchronized (serverLog) { serverLog.append(line).append('\n'); }
                            if (line.contains("CN1SS_SERVER_PORT")) { ready.countDown(); }
                        }
                    } catch (IOException ignore) {
                    }
                }
            });
            sreader.setDaemon(true);
            sreader.start();
            assertTrue(ready.await(30, java.util.concurrent.TimeUnit.SECONDS),
                    "cn1ss server should start listening");

            // Launch the suite exe and watch its stdout for the end marker. The
            // exe never self-exits (it owns the Win32 message pump), so we kill it
            // once the suite reports done or the deadline passes.
            ProcessBuilder appPb = new ProcessBuilder(exe.toAbsolutePath().toString());
            appPb.directory(exe.getParent().toFile());
            appPb.redirectErrorStream(true);
            app = appPb.start();
            final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean(false);
            final java.util.concurrent.atomic.AtomicInteger finishedTests = new java.util.concurrent.atomic.AtomicInteger(0);
            final java.util.concurrent.atomic.AtomicReference<String> lastLine =
                    new java.util.concurrent.atomic.AtomicReference<String>("");
            // The real shared benchmark emits "CN1SS:STAT:<metric>: <value>" lines
            // (Base64NativePerformanceTest: base64 native/CN1/SIMD + image
            // createMask/applyMask/modifyAlpha/PNG/JPEG, plus the SIMD kernel tally),
            // the same markers iOS/Android surface. Collected here and written to
            // windows-benchmark-stats.txt so the cn1ss report renders them in the PR
            // comment's Benchmark Results table.
            final java.util.List<String> simdStats =
                    java.util.Collections.synchronizedList(new java.util.ArrayList<String>());
            final Process appF = app;
            Thread areader = new Thread(new Runnable() {
                public void run() {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(
                            appF.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            if (line.contains("CN1SS:SUITE:FINISHED")) { finished.set(true); }
                            if (line.contains("suite finished test=")) { finishedTests.incrementAndGet(); }
                            int s = line.indexOf("CN1SS:STAT:");
                            if (s >= 0) { simdStats.add(line.substring(s + "CN1SS:STAT:".length()).trim()); }
                            if (line.contains("CN1SS:") || line.contains("suite ")) { lastLine.set(line); }
                        }
                    } catch (IOException ignore) {
                    }
                }
            });
            areader.setDaemon(true);
            areader.start();

            // Completion signal: all screenshots captured. Under mvn/surefire load
            // the suite runs notably slower than run-hello.bat's detached launch, and
            // the slow trailing API tests (each burning its per-test DONE timeout
            // without emitting an image) come AFTER the last screenshot -- so waiting
            // for SUITE:FINISHED is both slow and flaky. Instead, finish once the
            // bulk has landed AND no new PNG has arrived for a stabilization window
            // (the screenshot-producing tests are done; only non-rendering tests
            // remain). SUITE:FINISHED still short-circuits. 40-minute hard cap.
            int minPngs = 100;
            long stableMs = 150_000L;       // > the slowest single inter-screenshot gap
            long deadline = System.currentTimeMillis() + 40L * 60 * 1000;
            int pngs = 0;
            int lastPngs = -1;
            long lastChange = System.currentTimeMillis();
            while (System.currentTimeMillis() < deadline) {
                if (finished.get()) { break; }
                pngs = countPngFiles(outDir);
                if (pngs != lastPngs) { lastPngs = pngs; lastChange = System.currentTimeMillis(); }
                if (pngs >= minPngs && (System.currentTimeMillis() - lastChange) >= stableMs) { break; }
                Thread.sleep(3000);
            }
            pngs = countPngFiles(outDir);
            assertTrue(finished.get() || pngs >= minPngs,
                    "hello suite capture incomplete: pngs=" + pngs + " (need " + minPngs + ")"
                    + " finishedTests=" + finishedTests.get() + " suiteFinished=" + finished.get()
                    + " lastLine=" + lastLine.get() + "\n" + serverLog);

            String outEnv = System.getenv("CN1_SHOT_OUTPUT_DIR");
            if (outEnv != null) {
                Path dest = Paths.get(outEnv);
                Files.createDirectories(dest);
                try (java.util.stream.Stream<Path> s = Files.list(outDir)) {
                    for (Path p : (Iterable<Path>) s::iterator) {
                        if (p.getFileName().toString().endsWith(".png")) {
                            Files.copy(p, dest.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
                // Persist the SIMD benchmark tally next to the PNGs so the cn1ss
                // comment job can pick it up (--extra-stats) and render it in the
                // PR comment's Benchmark Results table.
                if (!simdStats.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    synchronized (simdStats) {
                        for (String l : simdStats) { sb.append(l).append('\n'); }
                    }
                    Files.write(dest.resolve("windows-benchmark-stats.txt"),
                            sb.toString().getBytes(StandardCharsets.UTF_8));
                    System.out.println("CN1_SIMD_STATS=" + simdStats.size() + " lines");
                }
            }
            System.out.println("CN1_HELLO_SUITE_PNGS=" + pngs);
        } finally {
            if (app != null) { app.destroyForcibly(); }
            server.destroy();
        }
    }

    static int countPngFiles(Path dir) throws IOException {
        int n = 0;
        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (p.getFileName().toString().endsWith(".png")) { n++; }
            }
        }
        return n;
    }

    /** runTranslator variant taking a pre-joined ';'-separated source-root list. */
    static void runTranslatorMultiSource(String sources, Path outputDir, String appName, String appType) throws Exception {
        runTranslatorImpl(sources, outputDir, appName, appType);
    }

    static String winGfxTestSource() {
        return "import com.codename1.impl.windows.WindowsNative;\n" +
                "public class WinGfxTest {\n" +
                "    public static void main(String[] args) {\n" +
                "        long g = WindowsNative.createOffscreenGraphics(400, 300);\n" +
                "        WindowsNative.setColor(g, 0xffffff);\n" +
                "        WindowsNative.fillRect(g, 0, 0, 400, 300);\n" +
                "        WindowsNative.setColor(g, 0xff0000);\n" +
                "        WindowsNative.fillRect(g, 50, 50, 200, 100);\n" +
                "        WindowsNative.setColor(g, 0x000000);\n" +
                "        WindowsNative.drawString(g, \"Hello Direct2D\", 60, 60);\n" +
                // The clean target's String[] args marshalling is unrelated to the
                // port (and currently faulty), so write a fixed file in the cwd.
                "        boolean ok = WindowsNative.saveGraphicsToPng(g, \"cn1_render.png\");\n" +
                "        System.out.println(ok ? \"RENDER_OK\" : \"RENDER_FAIL\");\n" +
                "    }\n" +
                "}\n";
    }

    /**
     * End-to-end Direct3D 11 render check for the portable 3D API on Windows.
     * Translates a tiny app that drives the WindowsNative.gl3d* bridge to render a
     * Phong-lit cube into an offscreen D3D render target, plus Matrix4 + the native
     * layer (cn1_windows_d3d.cpp). On Windows it builds with clang-cl, runs the exe
     * (WARP software D3D is fine when there is no GPU) and verifies the PNG; on
     * other hosts it cross-compiles the exe with clang-cl + xwin so the native D3D
     * layer is at least compile/link checked (set CN1_XWIN_SYSROOT to run that leg).
     */
    @org.junit.jupiter.api.Test
    void rendersOffscreenToPngWithDirect3D() throws Exception {
        java.util.List<CompilerHelper.CompilerConfig> configs = new java.util.ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(configs.isEmpty(), "No JDK available to translate with");
        CompilerHelper.CompilerConfig config = configs.get(0);

        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("win3d-sources");
        Path classesDir = Files.createTempDirectory("win3d-classes");
        Path javaApiDir = Files.createTempDirectory("win3d-japi");
        Files.write(sourceDir.resolve("WinGfx3DTest.java"), winGfx3DTestSource().getBytes(StandardCharsets.UTF_8));
        Path windowsNativeSrc = Paths.get("..", "..", "Ports", "WindowsPort", "src",
                "com", "codename1", "impl", "windows", "WindowsNative.java").normalize().toAbsolutePath();
        Path matrix4Src = Paths.get("..", "..", "CodenameOne", "src",
                "com", "codename1", "gpu", "Matrix4.java").normalize().toAbsolutePath();

        CompilerHelper.compileJavaAPI(javaApiDir, config);
        List<String> compileArgs = new java.util.ArrayList<>();
        compileArgs.add("-source"); compileArgs.add(config.targetVersion);
        compileArgs.add("-target"); compileArgs.add(config.targetVersion);
        if (CompilerHelper.useClasspath(config)) {
            compileArgs.add("-classpath"); compileArgs.add(javaApiDir.toString());
        } else {
            compileArgs.add("-bootclasspath"); compileArgs.add(javaApiDir.toString());
            compileArgs.add("-Xlint:-options");
        }
        compileArgs.add("-d"); compileArgs.add(classesDir.toString());
        compileArgs.add(sourceDir.resolve("WinGfx3DTest.java").toString());
        compileArgs.add(windowsNativeSrc.toString());
        compileArgs.add(matrix4Src.toString());
        assertEquals(0, CompilerHelper.compile(config.jdkHome, compileArgs), "WinGfx3DTest + Matrix4 + WindowsNative should compile");
        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        Path nativeDir = Paths.get("..", "..", "Ports", "WindowsPort", "nativeSources").normalize().toAbsolutePath();
        try (java.util.stream.Stream<Path> s = Files.list(nativeDir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isRegularFile(p)) {
                    Files.copy(p, classesDir.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Path outputDir = Files.createTempDirectory("win3d-out");
        runTranslator(classesDir, outputDir, "WinGfx3DTest", "windows");
        Path distDir = outputDir.resolve("dist");

        if (CompilerHelper.isWindows()) {
            Path buildDir = distDir.resolve("build");
            Files.createDirectories(buildDir);
            runCommand(Arrays.asList("cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                    "-DCMAKE_BUILD_TYPE=Release", "-G", "Ninja",
                    "-DCMAKE_C_COMPILER=clang-cl", "-DCMAKE_CXX_COMPILER=clang-cl"), distDir);
            runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
            Path exe = buildDir.resolve(CompilerHelper.executableName("WinGfx3DTest"));
            String out = runCommand(Arrays.asList(exe.toString()), buildDir);
            Path png = buildDir.resolve("cn1_render.png");
            assertTrue(out.contains("RENDER_OK"), "D3D render app should report success, output:\n" + out);
            assertTrue(Files.exists(png), "a PNG frame should be written");
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(png.toFile());
            assertNotNull(img, "PNG should decode");
            assertEquals(400, img.getWidth());
            assertEquals(300, img.getHeight());
            // The lit blue cube must cover the centre; the corners stay the dark clear color.
            int centre = img.getRGB(200, 150) & 0xffffff;
            int corner = img.getRGB(5, 5) & 0xffffff;
            assertTrue((centre & 0xff) > 0x50, "expected a lit blue cube pixel at the centre, was " + Integer.toHexString(centre));
            assertTrue(corner < 0x202030, "expected the dark clear color in the corner, was " + Integer.toHexString(corner));
            return;
        }

        // Non-Windows: cross-compile the dist (incl. cn1_windows_d3d.cpp) to a
        // Windows PE with clang-cl + xwin, so the native D3D layer is compile/link
        // checked. Running the PE (render verification) happens on Windows.
        String sysroot = System.getenv("CN1_XWIN_SYSROOT");
        org.junit.jupiter.api.Assumptions.assumeTrue(sysroot != null && !sysroot.trim().isEmpty(),
                "Set CN1_XWIN_SYSROOT to an `xwin splat` directory to cross-compile the D3D check off-Windows");
        Path sys = Paths.get(sysroot.trim()).toAbsolutePath();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(sys.resolve("crt/include")),
                "CN1_XWIN_SYSROOT must contain crt/include + sdk/include (run `xwin splat`): " + sys);
        String clangCl = System.getenv().getOrDefault("CN1_CLANG_CL", "clang-cl");
        String llvmRc = System.getenv().getOrDefault("CN1_LLVM_RC", "llvm-rc");
        String inc = String.join(" ", "--target=x86_64-pc-windows-msvc",
                imsvc(sys.resolve("crt/include")), imsvc(sys.resolve("sdk/include/ucrt")),
                imsvc(sys.resolve("sdk/include/um")), imsvc(sys.resolve("sdk/include/shared")),
                imsvc(sys.resolve("sdk/include/winrt")));
        String linkFlags = String.join(" ", "-fuse-ld=lld",
                "/libpath:" + sys.resolve("crt/lib/x86_64"),
                "/libpath:" + sys.resolve("sdk/lib/um/x86_64"),
                "/libpath:" + sys.resolve("sdk/lib/ucrt/x86_64"));
        String rcFlags = String.join(" ", "-I", sys.resolve("sdk/include/um").toString(),
                "-I", sys.resolve("sdk/include/shared").toString(),
                "-I", sys.resolve("crt/include").toString(),
                "-I", sys.resolve("sdk/include/ucrt").toString());
        Path buildDir = distDir.resolve("xbuild");
        Files.createDirectories(buildDir);
        runCommand(Arrays.asList("cmake", "-S", distDir.toString(), "-B", buildDir.toString(),
                "-G", "Ninja", "-DCMAKE_SYSTEM_NAME=Windows", "-DCMAKE_SYSTEM_PROCESSOR=AMD64",
                "-DCMAKE_TRY_COMPILE_TARGET_TYPE=STATIC_LIBRARY",
                "-DCMAKE_C_COMPILER=" + clangCl, "-DCMAKE_CXX_COMPILER=" + clangCl,
                "-DCMAKE_BUILD_TYPE=Release", "-DCMAKE_C_FLAGS=" + inc, "-DCMAKE_CXX_FLAGS=" + inc,
                "-DCMAKE_RC_COMPILER=" + llvmRc, "-DCMAKE_RC_FLAGS=" + rcFlags,
                "-DCMAKE_EXE_LINKER_FLAGS=" + linkFlags), distDir);
        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), distDir);
        Path exe = buildDir.resolve("WinGfx3DTest.exe");
        assertTrue(Files.exists(exe) && Files.size(exe) > 100_000, "cross-compiled D3D PE should be produced: " + exe);
        System.out.println("CN1_GPU_XWIN_EXE=" + exe.toAbsolutePath());
    }

    static String winGfx3DTestSource() {
        return "import com.codename1.impl.windows.WindowsNative;\n" +
                "import com.codename1.gpu.Matrix4;\n" +
                "public class WinGfx3DTest {\n" +
                "    public static void main(String[] args) {\n" +
                "        int W = 400, H = 300;\n" +
                "        long ctx = WindowsNative.gl3dCreateContext();\n" +
                "        if (ctx == 0) { System.out.println(\"RENDER_FAIL_NOCTX\"); return; }\n" +
                "        WindowsNative.gl3dBeginFrame(ctx, W, H);\n" +
                "        WindowsNative.gl3dSetViewport(ctx, 0, 0, W, H);\n" +
                "        float[] verts = {\n" +
                "            -0.8f, -0.8f, 0.8f, 0f, 0f, 1f, 0f, 1f,\n" +
                "            0.8f, -0.8f, 0.8f, 0f, 0f, 1f, 1f, 1f,\n" +
                "            0.8f, 0.8f, 0.8f, 0f, 0f, 1f, 1f, 0f,\n" +
                "            -0.8f, 0.8f, 0.8f, 0f, 0f, 1f, 0f, 0f,\n" +
                "            0.8f, -0.8f, -0.8f, 0f, 0f, -1f, 0f, 1f,\n" +
                "            -0.8f, -0.8f, -0.8f, 0f, 0f, -1f, 1f, 1f,\n" +
                "            -0.8f, 0.8f, -0.8f, 0f, 0f, -1f, 1f, 0f,\n" +
                "            0.8f, 0.8f, -0.8f, 0f, 0f, -1f, 0f, 0f,\n" +
                "            -0.8f, -0.8f, -0.8f, -1f, 0f, 0f, 0f, 1f,\n" +
                "            -0.8f, -0.8f, 0.8f, -1f, 0f, 0f, 1f, 1f,\n" +
                "            -0.8f, 0.8f, 0.8f, -1f, 0f, 0f, 1f, 0f,\n" +
                "            -0.8f, 0.8f, -0.8f, -1f, 0f, 0f, 0f, 0f,\n" +
                "            0.8f, -0.8f, 0.8f, 1f, 0f, 0f, 0f, 1f,\n" +
                "            0.8f, -0.8f, -0.8f, 1f, 0f, 0f, 1f, 1f,\n" +
                "            0.8f, 0.8f, -0.8f, 1f, 0f, 0f, 1f, 0f,\n" +
                "            0.8f, 0.8f, 0.8f, 1f, 0f, 0f, 0f, 0f,\n" +
                "            -0.8f, 0.8f, 0.8f, 0f, 1f, 0f, 0f, 1f,\n" +
                "            0.8f, 0.8f, 0.8f, 0f, 1f, 0f, 1f, 1f,\n" +
                "            0.8f, 0.8f, -0.8f, 0f, 1f, 0f, 1f, 0f,\n" +
                "            -0.8f, 0.8f, -0.8f, 0f, 1f, 0f, 0f, 0f,\n" +
                "            -0.8f, -0.8f, -0.8f, 0f, -1f, 0f, 0f, 1f,\n" +
                "            0.8f, -0.8f, -0.8f, 0f, -1f, 0f, 1f, 1f,\n" +
                "            0.8f, -0.8f, 0.8f, 0f, -1f, 0f, 1f, 0f,\n" +
                "            -0.8f, -0.8f, 0.8f, 0f, -1f, 0f, 0f, 0f\n" +
                "        };\n" +
                "        short[] idx = new short[36];\n" +
                "        for (int face = 0; face < 6; face++) {\n" +
                "            int b = face * 4, o = face * 6;\n" +
                "            idx[o] = (short) b; idx[o+1] = (short) (b+1); idx[o+2] = (short) (b+2);\n" +
                "            idx[o+3] = (short) b; idx[o+4] = (short) (b+2); idx[o+5] = (short) (b+3);\n" +
                "        }\n" +
                "        long vbo = WindowsNative.gl3dCreateFloatBuffer(verts, verts.length);\n" +
                "        long ibo = WindowsNative.gl3dCreateShortBuffer(idx, idx.length);\n" +
                "        String hlsl =\n" +
                "                \"cbuffer CN1Uniforms : register(b0) {\\n\" +\n" +
                "                \"  float4x4 mvp;\\n\" +\n" +
                "                \"  float4x4 model;\\n\" +\n" +
                "                \"  float4x4 normalMatrix;\\n\" +\n" +
                "                \"  float4 color;\\n\" +\n" +
                "                \"  float4 lightDir;\\n\" +\n" +
                "                \"  float4 lightColor;\\n\" +\n" +
                "                \"  float4 ambient;\\n\" +\n" +
                "                \"  float4 eye;\\n\" +\n" +
                "                \"  float shininess;\\n\" +\n" +
                "                \"};\\n\" +\n" +
                "                \"struct VSInput {\\n\" +\n" +
                "                \"  float3 position : POSITION;\\n\" +\n" +
                "                \"  float3 normal : NORMAL;\\n\" +\n" +
                "                \"  float2 texcoord : TEXCOORD0;\\n\" +\n" +
                "                \"};\\n\" +\n" +
                "                \"struct VSOutput {\\n\" +\n" +
                "                \"  float4 position : SV_Position;\\n\" +\n" +
                "                \"  float3 worldNormal : TEXCOORD1;\\n\" +\n" +
                "                \"  float3 worldPos : TEXCOORD2;\\n\" +\n" +
                "                \"};\\n\" +\n" +
                "                \"VSOutput cn1_vertex_main(VSInput input) {\\n\" +\n" +
                "                \"  VSOutput output;\\n\" +\n" +
                "                \"  float4 clip = mul(mvp, float4(input.position, 1.0));\\n\" +\n" +
                "                \"  clip.z = (clip.z + clip.w) * 0.5;\\n\" +\n" +
                "                \"  output.position = clip;\\n\" +\n" +
                "                \"  output.worldNormal = mul(normalMatrix, float4(input.normal, 0.0)).xyz;\\n\" +\n" +
                "                \"  output.worldPos = mul(model, float4(input.position, 1.0)).xyz;\\n\" +\n" +
                "                \"  return output;\\n\" +\n" +
                "                \"}\\n\" +\n" +
                "                \"float4 cn1_fragment_main(VSOutput input) : SV_Target {\\n\" +\n" +
                "                \"  float4 base = color;\\n\" +\n" +
                "                \"  float3 n = normalize(input.worldNormal);\\n\" +\n" +
                "                \"  float3 l = normalize(-lightDir.xyz);\\n\" +\n" +
                "                \"  float ndotl = max(dot(n, l), 0.0);\\n\" +\n" +
                "                \"  float3 lighting = ambient.xyz + lightColor.xyz * ndotl;\\n\" +\n" +
                "                \"  float3 rgb = base.rgb * lighting;\\n\" +\n" +
                "                \"  if (ndotl > 0.0) {\\n\" +\n" +
                "                \"    float3 v = normalize(eye.xyz - input.worldPos);\\n\" +\n" +
                "                \"    float3 h = normalize(l + v);\\n\" +\n" +
                "                \"    float spec = pow(max(dot(n, h), 0.0), shininess);\\n\" +\n" +
                "                \"    rgb += lightColor.xyz * spec;\\n\" +\n" +
                "                \"  }\\n\" +\n" +
                "                \"  return float4(rgb, base.a);\\n\" +\n" +
                "                \"}\\n\";\n" +
                "        long pipe = WindowsNative.gl3dGetOrCreatePipeline(ctx, \"cube\", hlsl, 0, 1, 1, 1);\n" +
                "        if (pipe == 0) { System.out.println(\"RENDER_FAIL_PIPE\"); return; }\n" +
                "        float[] proj = Matrix4.perspective((float) Math.toRadians(45), (float) W / H, 0.1f, 100f);\n" +
                "        float[] view = Matrix4.lookAt(2.6f, 2.1f, 3.4f, 0f, 0f, 0f, 0f, 1f, 0f);\n" +
                "        float[] vp = Matrix4.identity(); Matrix4.multiply(proj, view, vp);\n" +
                "        float[] model = Matrix4.rotation((float) Math.toRadians(25), 0.35f, 1f, 0.12f);\n" +
                "        float[] mvp = Matrix4.identity(); Matrix4.multiply(vp, model, mvp);\n" +
                "        float[] nm = Matrix4.normalMatrix(model);\n" +
                "        float[] u = new float[72];\n" +
                "        int p = 0;\n" +
                "        for (int i = 0; i < 16; i++) u[p++] = mvp[i];\n" +
                "        for (int i = 0; i < 16; i++) u[p++] = model[i];\n" +
                "        for (int i = 0; i < 16; i++) u[p++] = nm[i];\n" +
                "        u[p++] = 0x33/255f; u[p++] = 0x66/255f; u[p++] = 0xff/255f; u[p++] = 1f;\n" +
                "        u[p++] = -0.4f; u[p++] = -1f; u[p++] = -0.55f; u[p++] = 0f;\n" +
                "        u[p++] = 1f; u[p++] = 1f; u[p++] = 1f; u[p++] = 1f;\n" +
                "        u[p++] = 0.25f; u[p++] = 0.25f; u[p++] = 0.25f; u[p++] = 1f;\n" +
                "        u[p++] = 2.6f; u[p++] = 2.1f; u[p++] = 3.4f; u[p++] = 1f;\n" +
                "        u[p++] = 24f;\n" +
                "        WindowsNative.gl3dClear(ctx, 0xff101018, true, true);\n" +
                "        WindowsNative.gl3dDrawIndexed(ctx, pipe, vbo, 32, ibo, 36, 3, u, 72, 0L, 0, 0);\n" +
                "        boolean ok = WindowsNative.gl3dCaptureToFile(ctx, \"cn1_render.png\");\n" +
                "        System.out.println(ok ? \"RENDER_OK\" : \"RENDER_FAIL\");\n" +
                "    }\n" +
                "}\n";
    }

    static void runTranslator(Path classesDir, Path outputDir, String appName) throws Exception {
        runTranslator(classesDir, outputDir, appName, "ios");
    }

    static void runTranslator(Path classesDir, Path outputDir, String appName, String appType) throws Exception {
        runTranslatorImpl(classesDir.toString(), outputDir, appName, appType);
    }

    static void runTranslatorImpl(String sources, Path outputDir, String appName, String appType) throws Exception {
        Path translatorResources = Paths.get("..", "ByteCodeTranslator", "src").normalize().toAbsolutePath();
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        URL[] systemUrls;
        if (systemLoader instanceof URLClassLoader) {
            systemUrls = ((URLClassLoader) systemLoader).getURLs();
        } else {
             // For Java 9+, we need to get the classpath from the system property
             String[] paths = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
             systemUrls = new URL[paths.length];
             for (int i=0; i<paths.length; i++) {
                 systemUrls[i] = Paths.get(paths[i]).toUri().toURL();
             }
        }

        URL[] urls = Arrays.copyOf(systemUrls, systemUrls.length + 1);
        urls[systemUrls.length] = translatorResources.toUri().toURL();
        URLClassLoader loader = new URLClassLoader(urls, null);

        ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            assertNotNull(loader.getResource("cn1_globals.h"), "Translator resources should be on the classpath");
            Class<?> translatorClass = Class.forName("com.codename1.tools.translator.ByteCodeTranslator", true, loader);
            assertEquals(loader, translatorClass.getClassLoader());
            java.lang.reflect.Field verboseField = translatorClass.getField("verbose");
            boolean originalVerbose = verboseField.getBoolean(null);
            verboseField.setBoolean(null, false);
            Method main = translatorClass.getMethod("main", String[].class);
            String[] args = new String[]{
                    "clean",
                    sources,
                    outputDir.toString(),
                    appName,
                    "com.example.hello",
                    "Hello App",
                    "1.0",
                    appType,
                    "none"
            };
            try {
                main.invoke(null, (Object) args);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw new RuntimeException(cause);
            } finally {
                verboseField.setBoolean(null, originalVerbose);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
            try {
                loader.close();
            } catch (IOException ignore) {
            }
        }
    }

    static void replaceLibraryWithExecutableTarget(Path cmakeLists, String sourceDirName) throws IOException {
        String content = new String(Files.readAllBytes(cmakeLists), StandardCharsets.UTF_8);
        // The math functions live in libc/libm on POSIX, but are part of the CRT
        // under MSVC, where there is no separate libm to link against.
        String linkLine = CompilerHelper.isWindows()
                ? ""
                : "\ntarget_link_libraries(${PROJECT_NAME} m)";
        String replacement = content.replace(
                "add_library(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})",
                "add_executable(${PROJECT_NAME} ${TRANSLATOR_SOURCES} ${TRANSLATOR_HEADERS})" + linkLine
        );
        Files.write(cmakeLists, replacement.getBytes(StandardCharsets.UTF_8));
    }

    static String runCommand(List<String> command, Path workingDir) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        int exit = process.waitFor();
        assertEquals(0, exit, "Command failed: " + String.join(" ", command) + "\nOutput:\n" + output);
        return output;
    }

    static String helloWorldSource() {
        return "public class HelloWorld {\n" +
                "    private static native void nativeHello();\n" +
                "    public static void main(String[] args) {\n" +
                "        nativeHello();\n" +
                "    }\n" +
                "}\n";
    }

    static String nativeHelloSource() {
        return "#include \"cn1_globals.h\"\n" +
                "#include <stdio.h>\n" +
                "void HelloWorld_nativeHello__(CODENAME_ONE_THREAD_STATE) {\n" +
                "    printf(\"Hello, Clean Target!\\n\");\n" +
                "}\n";
    }

}
