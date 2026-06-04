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
                "cn1_windows_dwrite.cpp", "cn1_windows_screenshot.cpp"
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
        // Bind the abstract platform impl to WindowsImplementation (its @Concrete
        // is baked to the iOS impl, which we don't translate here).
        String prevConcrete = System.getProperty("cn1.concreteImplementation");
        System.setProperty("cn1.concreteImplementation", "com.codename1.impl.windows.WindowsImplementation");
        try {
            runTranslatorMultiSource(sources, outputDir, appName, "windows");
        } finally {
            if (prevConcrete == null) {
                System.clearProperty("cn1.concreteImplementation");
            } else {
                System.setProperty("cn1.concreteImplementation", prevConcrete);
            }
        }

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
                // The transcoded SVG/Lottie images are pure-Java GeneratedSVGImage
                // classes; the generated SVGRegistry maps each source file name
                // (star.svg, lottie_spinner.json, ...) into the global image table
                // so Resources.getImage(name) -- and the theme's url(*.svg)
                // backgrounds -- resolve to the real animation instead of the 1x1
                // placeholder the no-cef CSS compiler stored. On device this call
                // lives in the per-build Stub (like IPhoneBuilder/AndroidGradleBuilder
                // emit); the launcher is that stub here. The direct reference also
                // forces the translator to retain the generated package (a
                // Class.forName lookup would let it be dead-code-eliminated). It
                // MUST run before initFirstTheme so the theme's SVG backgrounds bind
                // to the generated images while the Style objects are built.
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
        Path coreClasses = Paths.get("..", "..", "maven", "core", "target", "classes").normalize().toAbsolutePath();
        Path portClasses = Paths.get("..", "..", "maven", "windows", "target", "classes").normalize().toAbsolutePath();
        Path commonClasses = Paths.get("..", "..", "scripts", "hellocodenameone", "common", "target", "classes")
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
                + java.io.File.pathSeparator + commonClasses + java.io.File.pathSeparator + kotlinDir;
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
                + portClasses + ";" + javaApiDir + ";" + nativeStage;
        String prevConcrete = System.getProperty("cn1.concreteImplementation");
        System.setProperty("cn1.concreteImplementation", "com.codename1.impl.windows.WindowsImplementation");
        try {
            runTranslatorMultiSource(sources, outputDir, "WinHelloMain", "windows");
        } finally {
            if (prevConcrete == null) {
                System.clearProperty("cn1.concreteImplementation");
            } else {
                System.setProperty("cn1.concreteImplementation", prevConcrete);
            }
        }

        Path cmakeRoot = outputDir.resolve("dist");
        assertTrue(Files.exists(cmakeRoot.resolve("CMakeLists.txt")), "translator should emit a CMake project");
        Path buildDir = cmakeRoot.resolve("build");
        Files.createDirectories(buildDir);
        runCommand(Arrays.asList("cmake", "-S", cmakeRoot.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=RelWithDebInfo", "-G", "Ninja",
                "-DCMAKE_C_COMPILER=clang-cl", "-DCMAKE_CXX_COMPILER=clang-cl"), cmakeRoot);
        runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), cmakeRoot);
        Path exe = buildDir.resolve(CompilerHelper.executableName("WinHelloMain"));
        assertTrue(Files.exists(exe), "native executable should be produced: " + exe);
        Path stagedTheme = exe.resolveSibling("windowsNativeTheme.res");
        Path stagedAppTheme = exe.resolveSibling("theme.res");

        // The Windows native theme = the full material theme. The port has no
        // built-in native look, so this is what gives every component its
        // styling (checkbox, switch, tabs, text field, multi-button, dialog
        // buttons, FAB, sheet, ...) and -- crucially -- the $Dark<UIID> variants
        // that make the *_dark screenshot tiles actually render dark. It's the
        // same theme Android ships. installNativeTheme() loads it; the app theme
        // below layers over it via @includeNativeBool.
        Path materialTheme = Paths.get("..", "..", "Themes", "AndroidMaterialTheme.res").normalize().toAbsolutePath();
        if (Files.exists(materialTheme)) {
            Files.copy(materialTheme, stagedTheme, StandardCopyOption.REPLACE_EXISTING);
        }

        // The app theme = the project's own theme.css compiled headless with the
        // no-cef CSS compiler. It holds the CSS-gradient tiles + the SVG/Lottie
        // url() background UIIDs + the Button font, and is loaded over the native
        // theme by the launcher (UIManager.initFirstTheme("/theme")). If the
        // css-compiler jar isn't built or the compile fails the app theme is
        // simply absent -- the suite still runs on the material native look.
        try {
            Path cssDir = Paths.get("..", "..", "maven", "css-compiler", "target").normalize().toAbsolutePath();
            Path cssJar = null;
            if (Files.isDirectory(cssDir)) {
                try (java.util.stream.Stream<Path> s = Files.list(cssDir)) {
                    cssJar = s.filter(p -> p.getFileName().toString().endsWith("-jar-with-dependencies.jar"))
                            .findFirst().orElse(null);
                }
            }
            Path themeCss = Paths.get("..", "..", "scripts", "hellocodenameone", "common", "src", "main", "css", "theme.css")
                    .normalize().toAbsolutePath();
            if (cssJar != null && Files.exists(themeCss)) {
                String javaBin = Paths.get(System.getProperty("java.home"), "bin",
                        CompilerHelper.executableName("java")).toString();
                ProcessBuilder pb = new ProcessBuilder(javaBin, "-jar", cssJar.toString(),
                        "-input", themeCss.toString(), "-output", stagedAppTheme.toString());
                pb.inheritIO();
                pb.start().waitFor();
            }
        } catch (Exception themeCompileFailed) {
            // Non-fatal: the app theme is an overlay; the material native theme
            // still drives the component look and dark mode.
        }

        // The material icon font. The DirectWrite loader resolves bundled
        // TrueType fonts by reading the ttf from the executable directory
        // (cn1_windows_text.c loadTrueTypeFont), so FontImage glyphs -- the
        // checkbox check, switch thumb, FAB "+", ImageViewer arrows, toast icon
        // -- only render when this file sits next to the exe.
        Path materialFont = Paths.get("..", "..", "CodenameOne", "src", "material-design-font.ttf")
                .normalize().toAbsolutePath();
        if (Files.exists(materialFont)) {
            Files.copy(materialFont, exe.resolveSibling("material-design-font.ttf"), StandardCopyOption.REPLACE_EXISTING);
        }
        return exe;
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
        Path exe = buildHelloCodenameOneExe();

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
            final Process appF = app;
            Thread areader = new Thread(new Runnable() {
                public void run() {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(
                            appF.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            if (line.contains("CN1SS:SUITE:FINISHED")) { finished.set(true); }
                            if (line.contains("suite finished test=")) { finishedTests.incrementAndGet(); }
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
            }
            System.out.println("CN1_HELLO_SUITE_PNGS=" + pngs);
        } finally {
            if (app != null) { app.destroyForcibly(); }
            server.destroy();
        }
    }

    private static int countPngFiles(Path dir) throws IOException {
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
