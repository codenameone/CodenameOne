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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Native Linux (GTK3/Cairo) counterpart of the Windows cases in
 * {@link CleanTargetIntegrationTest}. Builds the real hellocodenameone screenshot
 * suite through the native Linux port -- translate the app + Kotlin stdlib + core
 * + the Linux port + JavaAPI with the {@code linux} app type, then native
 * CMake/Ninja-build the ELF -- and runs it headless (under Xvfb in CI) capturing
 * the suite over the cn1ss WebSocket. Unlike Windows the build host is the run
 * host, so there is no cross-compile: one Linux runner builds and runs.
 *
 * <p>All cases are gated on a Linux host and on the framework + suite having been
 * built (the CI job installs core + the Linux port + hellocodenameone-common
 * first); they no-op (assumeTrue) elsewhere.</p>
 */
class CleanTargetLinuxIntegrationTest {

    /** Launcher main that drives the hellocodenameone Cn1ssDeviceRunner suite on the Linux port. */
    static String linuxHelloLauncherSource() {
        return "import com.codename1.ui.Display;\n" +
                "import com.codename1.testing.TestReporting;\n" +
                "import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunner;\n" +
                "import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunnerReporter;\n" +
                "public class LinuxHelloMain {\n" +
                "    public static void main(String[] args) {\n" +
                "        Display.init(null);\n" +
                // Force kotlin.Unit into the translation set (reached only via lambda
                // return types otherwise), exactly as the Windows launcher does.
                "        if (kotlin.Unit.INSTANCE == null) { return; }\n" +
                // Install the transcoded SVG/Lottie registry before the first theme so
                // url(*.svg) backgrounds resolve, then load the app theme.
                "        try { com.codename1.generated.svg.SVGRegistry.installGlobal(); } catch (Throwable __svg) { __svg.printStackTrace(); }\n" +
                "        com.codename1.ui.plaf.UIManager.initFirstTheme(\"/theme\");\n" +
                "        TestReporting.setInstance(new Cn1ssDeviceRunnerReporter());\n" +
                "        Cn1ssDeviceRunner.addTest(new com.codenameone.examples.hellocodenameone.tests.KotlinUiTest());\n" +
                // runSuite blocks polling each test off the EDT; the main thread owns
                // the GTK pump (runMainEventLoop) so the EDT is woken to lay out, paint
                // and emit each screenshot over the cn1ss WebSocket.
                "        new Thread(new Runnable() {\n" +
                "            public void run() { new Cn1ssDeviceRunner().runSuite(); }\n" +
                "        }, \"CN1SS-Runner\").start();\n" +
                "        com.codename1.impl.linux.LinuxImplementation.runMainEventLoop();\n" +
                "    }\n" +
                "}\n";
    }

    /**
     * Translates the real hellocodenameone screenshot suite (app + Kotlin stdlib +
     * core + the Linux port + JavaAPI + the port's nativeSources) into a CMake dist
     * with the {@code linux} app type and returns the dist root. Pure Java
     * translation (no native toolchain), the Linux analog of
     * {@code translateHelloSuiteDist}.
     */
    static Path translateHelloSuiteDistLinux() throws Exception {
        Path coreClasses = Paths.get("..", "..", "maven", "core", "target", "classes").normalize().toAbsolutePath();
        Path portClasses = Paths.get("..", "..", "maven", "linux", "target", "classes").normalize().toAbsolutePath();
        Path commonClasses = Paths.get("..", "..", "scripts", "hellocodenameone", "common", "target", "classes")
                .normalize().toAbsolutePath();
        Path adsMockClasses = Paths.get("..", "..", "maven", "cn1-ads-mock", "target", "classes")
                .normalize().toAbsolutePath();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(coreClasses.resolve("com/codename1/ui/Form.class")),
                "codenameone-core must be built (maven/core/target/classes)");
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(portClasses.resolve("com/codename1/impl/linux/LinuxImplementation.class")),
                "LinuxPort must be built (maven/linux/target/classes)");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                Files.exists(commonClasses.resolve("com/codenameone/examples/hellocodenameone/tests/Cn1ssDeviceRunner.class")),
                "hellocodenameone-common must be built (scripts/hellocodenameone/common/target/classes)");

        List<CompilerHelper.CompilerConfig> configs = new ArrayList<>();
        for (String v : new String[] { "17", "21", "25", "11", "1.8" }) {
            configs.addAll(CompilerHelper.getAvailableCompilers(v));
        }
        org.junit.jupiter.api.Assumptions.assumeFalse(configs.isEmpty(), "No JDK available to translate with");
        CompilerHelper.CompilerConfig config = configs.get(0);

        // Stage the Kotlin stdlib (the app is Kotlin) into a translator source root.
        Path kotlinDir = Files.createTempDirectory("linuxhello-kotlin");
        Path m2 = Paths.get(System.getProperty("user.home"), ".m2", "repository", "org", "jetbrains", "kotlin");
        for (String[] ga : new String[][] {
                { "kotlin-stdlib", "1.6.0" }, { "kotlin-stdlib-jdk7", "1.6.0" }, { "kotlin-stdlib-jdk8", "1.6.0" },
                { "kotlin-stdlib-common", "1.6.0" }, { "kotlin-annotations-jvm", "1.6.0" } }) {
            Path jar = m2.resolve(ga[0]).resolve(ga[1]).resolve(ga[0] + "-" + ga[1] + ".jar");
            if (Files.exists(jar)) {
                CleanTargetIntegrationTest.extractJar(jar, kotlinDir);
            }
        }

        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("linuxhello-sources");
        Path classesDir = Files.createTempDirectory("linuxhello-classes");
        Path javaApiDir = Files.createTempDirectory("linuxhello-japi");
        Files.write(sourceDir.resolve("LinuxHelloMain.java"), linuxHelloLauncherSource().getBytes(StandardCharsets.UTF_8));

        CompilerHelper.compileJavaAPI(javaApiDir, config);
        String cp = coreClasses + java.io.File.pathSeparator + portClasses
                + java.io.File.pathSeparator + commonClasses + java.io.File.pathSeparator + kotlinDir
                + java.io.File.pathSeparator + adsMockClasses;
        List<String> appCompile = new ArrayList<>(Arrays.asList(
                "-encoding", "UTF-8",
                "-source", config.targetVersion, "-target", config.targetVersion,
                "-classpath", cp, "-d", classesDir.toString(),
                sourceDir.resolve("LinuxHelloMain.java").toString()));
        assertEquals(0, CompilerHelper.compile(config.jdkHome, appCompile),
                "LinuxHelloMain should compile:\n" + CompilerHelper.getLastErrorLog());

        Path nativeDir = Paths.get("..", "..", "Ports", "LinuxPort", "nativeSources").normalize().toAbsolutePath();
        Path nativeStage = Files.createTempDirectory("linuxhello-native");
        try (java.util.stream.Stream<Path> s = Files.list(nativeDir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isRegularFile(p)) {
                    Files.copy(p, nativeStage.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        // Stage the bundled material icon font as a classpath resource so it is
        // .incbin'd into the ELF and loadTrueTypeFontFromMemory (FontConfig) can
        // resolve its family -- the icon glyphs (checkbox/radio/FAB) render.
        Path resStage = Files.createTempDirectory("linuxhello-res");
        Path font = Paths.get("..", "..", "CodenameOne", "src", "material-design-font.ttf").normalize().toAbsolutePath();
        if (Files.exists(font)) {
            Files.copy(font, resStage.resolve("material-design-font.ttf"), StandardCopyOption.REPLACE_EXISTING);
        }

        Path outputDir = Files.createTempDirectory("linuxhello-out");
        String sources = classesDir + ";" + commonClasses + ";" + kotlinDir + ";" + coreClasses + ";"
                + portClasses + ";" + javaApiDir + ";" + nativeStage + ";" + adsMockClasses + ";" + resStage;
        CleanTargetIntegrationTest.runTranslatorMultiSource(sources, outputDir, "LinuxHelloMain", "linux");

        Path cmakeRoot = outputDir.resolve("dist");
        assertTrue(Files.exists(cmakeRoot.resolve("CMakeLists.txt")), "translator should emit a CMake project");
        return cmakeRoot;
    }

    /**
     * Translates + native-builds the hellocodenameone suite ELF with CMake/Ninja
     * (the C compiler the runner provides; {@code cc} by default). Returns the ELF.
     */
    static Path buildHelloCodenameOneElf() throws Exception {
        Path cmakeRoot = translateHelloSuiteDistLinux();
        Path buildDir = cmakeRoot.resolve("build");
        Files.createDirectories(buildDir);
        String cc = System.getenv().getOrDefault("CN1_CC", "cc");
        List<String> configure = new ArrayList<>(Arrays.asList(
                "cmake", "-S", cmakeRoot.toString(), "-B", buildDir.toString(),
                "-DCMAKE_BUILD_TYPE=Release", "-G", "Ninja",
                "-DCMAKE_C_COMPILER=" + cc));
        CleanTargetIntegrationTest.runCommand(configure, cmakeRoot);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", buildDir.toString()), cmakeRoot);
        Path elf = buildDir.resolve("LinuxHelloMain");
        assertTrue(Files.exists(elf), "native ELF should be produced: " + elf);
        return elf;
    }

    /**
     * First milestone: the full app + Kotlin stdlib + reachable component graph
     * translates and LINKS through the native Linux port (every native method it
     * reaches has a C implementation). Running the suite + capturing the
     * screenshots is the separate case below.
     */
    @Test
    void buildsHelloCodenameOneLinuxNative() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(CompilerHelper.isLinux(),
                "Native Linux build is Linux-only");
        Path elf = buildHelloCodenameOneElf();
        System.out.println("CN1_HELLO_ELF=" + elf.toAbsolutePath() + " (" + (Files.size(elf) / 1024) + "KB)");
        String out = System.getenv("CN1_LINUX_ELF_OUT");
        if (out != null && !out.trim().isEmpty()) {
            Path dest = Paths.get(out.trim());
            if (dest.getParent() != null) { Files.createDirectories(dest.getParent()); }
            Files.copy(elf, dest, StandardCopyOption.REPLACE_EXISTING);
            dest.toFile().setExecutable(true);
            System.out.println("Native Linux suite ELF copied to " + dest.toAbsolutePath());
        }
    }

    /**
     * Runs the suite ELF (built here, or {@code CN1_PREBUILT_EXE} when a job hands
     * over a prebuilt one) and captures every screenshot over the cn1ss WebSocket.
     * The ELF connects to ws://127.0.0.1:8765 (Cn1ssDeviceRunnerHelper default)
     * through the port's POSIX socket layer. CI runs this under Xvfb (the test
     * inherits DISPLAY).
     */
    @Test
    void capturesHelloSuiteOverWebSocketLinux() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(CompilerHelper.isLinux(),
                "Native Linux websocket capture is Linux-only");
        Path elf;
        String prebuilt = System.getenv("CN1_PREBUILT_EXE");
        if (prebuilt != null && !prebuilt.trim().isEmpty()) {
            elf = Paths.get(prebuilt.trim()).toAbsolutePath();
            org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(elf),
                    "CN1_PREBUILT_EXE does not exist: " + elf);
            elf.toFile().setExecutable(true);
            System.out.println("Running prebuilt suite ELF: " + elf);
        } else {
            elf = buildHelloCodenameOneElf();
        }

        // Compile + start the shared cn1ss WebSocket screenshot server.
        List<CompilerHelper.CompilerConfig> configs = new ArrayList<>();
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

        int port = 8765;
        Path outDir = Files.createTempDirectory("cn1ss-linux-out");
        String javaBin = jdk.jdkHome.resolve("bin").resolve("java").toString();
        Process server = new ProcessBuilder(javaBin, "-cp", serverClasses.toString(),
                "Cn1ssScreenshotServer", "--port", String.valueOf(port), "--out", outDir.toString())
                .redirectErrorStream(true).start();
        Process app = null;
        try {
            final CountDownLatch ready = new CountDownLatch(1);
            final StringBuilder serverLog = new StringBuilder();
            Thread sreader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(server.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        synchronized (serverLog) { serverLog.append(line).append('\n'); }
                        if (line.contains("CN1SS_SERVER_PORT")) { ready.countDown(); }
                    }
                } catch (IOException ignore) {
                }
            });
            sreader.setDaemon(true);
            sreader.start();
            assertTrue(ready.await(30, TimeUnit.SECONDS), "cn1ss server should start listening");

            ProcessBuilder appPb = new ProcessBuilder(elf.toAbsolutePath().toString());
            appPb.directory(elf.getParent().toFile());
            appPb.redirectErrorStream(true);
            app = appPb.start();
            final AtomicBoolean finished = new AtomicBoolean(false);
            final Process appF = app;
            Thread areader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(appF.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (line.contains("CN1SS:SUITE:FINISHED")) { finished.set(true); }
                    }
                } catch (IOException ignore) {
                }
            });
            areader.setDaemon(true);
            areader.start();

            // Finish once the bulk of screenshots have landed and none has arrived
            // for a stabilization window (the trailing non-rendering API tests burn
            // their per-test timeout after the last image). 40-minute hard cap.
            int minPngs = 100;
            long stableMs = 150_000L;
            long deadline = System.currentTimeMillis() + 40L * 60 * 1000;
            int pngs = 0, lastPngs = -1;
            long lastChange = System.currentTimeMillis();
            while (System.currentTimeMillis() < deadline) {
                if (finished.get()) { break; }
                pngs = CleanTargetIntegrationTest.countPngFiles(outDir);
                if (pngs != lastPngs) { lastPngs = pngs; lastChange = System.currentTimeMillis(); }
                if (pngs >= minPngs && (System.currentTimeMillis() - lastChange) >= stableMs) { break; }
                Thread.sleep(3000);
            }
            pngs = CleanTargetIntegrationTest.countPngFiles(outDir);
            assertTrue(finished.get() || pngs >= minPngs,
                    "hello suite capture incomplete: pngs=" + pngs + " (need " + minPngs + ")\n" + serverLog);

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

        // Best-effort: also emit a *windowed* demo ELF -- one that builds and shows
        // a Form instead of driving the cn1ss suite -- by splicing the generated
        // launcher and relinking the objects we already compiled (a fast relink, no
        // re-translation). Runs only when CN1_LINUX_DEMO_OUT is set and we built
        // from source (a dist is present to splice). Wrapped so it can never fail
        // the suite test.
        String demoOut = System.getenv("CN1_LINUX_DEMO_OUT");
        if (demoOut != null && !demoOut.trim().isEmpty()
                && (prebuilt == null || prebuilt.trim().isEmpty())) {
            try {
                Path buildDir = elf.getParent();
                Path launcher = buildDir.getParent().resolve("LinuxHelloMain-src").resolve("LinuxHelloMain.c");
                spliceWindowedDemoLauncher(launcher);
                CleanTargetIntegrationTest.runCommand(
                        Arrays.asList("cmake", "--build", buildDir.toString()), buildDir.getParent());
                Path dest = Paths.get(demoOut.trim());
                if (dest.getParent() != null) { Files.createDirectories(dest.getParent()); }
                Files.copy(elf, dest, StandardCopyOption.REPLACE_EXISTING);
                dest.toFile().setExecutable(true);
                System.out.println("CN1_LINUX_DEMO_ELF=" + dest.toAbsolutePath()
                        + " (" + (Files.size(dest) / 1024) + "KB)");
            } catch (Throwable t) {
                System.out.println("Windowed demo build failed (non-fatal): " + t);
            }
        }
    }

    /**
     * Rewrites the generated suite launcher ({@code LinuxHelloMain.c}) into a
     * windowed demo: instead of driving the cn1ss screenshot suite it builds and
     * shows a small Form (title + two labels + a button) and enters the GTK event
     * loop. Lets the same translated dist produce a self-contained binary one can
     * double-click on a Linux desktop. Mirrors the operand-stack / GC-root
     * discipline of the generated {@code main} so the new objects stay reachable.
     */
    static void spliceWindowedDemoLauncher(Path launcherC) throws IOException {
        String s = new String(Files.readAllBytes(launcherC), StandardCharsets.UTF_8);
        // 1) pull in the Form/Label/Button headers the demo body calls (otherwise
        //    __NEW_* is implicitly declared and its pointer return is truncated).
        String incAnchor = "#include \"com_codename1_ui_plaf_UIManager.h\"\n";
        if (s.contains(incAnchor) && !s.contains("com_codename1_ui_Form.h")) {
            s = s.replace(incAnchor, incAnchor
                    + "#include \"com_codename1_ui_Form.h\"\n"
                    + "#include \"com_codename1_ui_Label.h\"\n"
                    + "#include \"com_codename1_ui_Button.h\"\n");
        }
        // 2) replace the suite-construction body (the Reporter..Thread.start span)
        //    with a Form build+show; keep Display.init + theme above it and
        //    runMainEventLoop below it.
        int start = s.indexOf("__NEW_com_codenameone_examples_hellocodenameone_tests_Cn1ssDeviceRunnerReporter");
        int end = s.indexOf("com_codename1_impl_linux_LinuxImplementation_runMainEventLoop__");
        if (start < 0 || end < 0 || end < start) {
            throw new IOException("launcher anchors not found; cannot splice windowed demo");
        }
        start = s.lastIndexOf('\n', start) + 1;     // start of the Reporter line
        end = s.lastIndexOf('\n', end) + 1;         // start of the runMainEventLoop line
        String body =
            "    /* --- windowed demo: build & show a Form (replaces the cn1ss suite) --- */\n" +
            "    PUSH_POINTER(__NEW_com_codename1_ui_Form(threadStateData));\n" +
            "    BC_DUP();\n" +
            "    com_codename1_ui_Form___INIT____(threadStateData, SP[-1].data.o);     SP -= 1;\n" +
            "    virtual_com_codename1_ui_Form_setTitle___java_lang_String(threadStateData, SP[-1].data.o, newStringFromCString(threadStateData, \"Codename One - Native Linux (GTK3 / Cairo)\"));\n" +
            "    PUSH_POINTER(__NEW_com_codename1_ui_Label(threadStateData));\n" +
            "    BC_DUP();\n" +
            "    com_codename1_ui_Label___INIT_____java_lang_String(threadStateData, SP[-1].data.o, newStringFromCString(threadStateData, \"This window is a single, self-contained native ELF.\"));     SP -= 1;\n" +
            "    virtual_com_codename1_ui_Form_addComponent___com_codename1_ui_Component(threadStateData, SP[-2].data.o, SP[-1].data.o);     SP -= 1;\n" +
            "    PUSH_POINTER(__NEW_com_codename1_ui_Label(threadStateData));\n" +
            "    BC_DUP();\n" +
            "    com_codename1_ui_Label___INIT_____java_lang_String(threadStateData, SP[-1].data.o, newStringFromCString(threadStateData, \"2D rendered by Cairo, text by Pango, widgets by GTK3.\"));     SP -= 1;\n" +
            "    virtual_com_codename1_ui_Form_addComponent___com_codename1_ui_Component(threadStateData, SP[-2].data.o, SP[-1].data.o);     SP -= 1;\n" +
            "    PUSH_POINTER(__NEW_com_codename1_ui_Button(threadStateData));\n" +
            "    BC_DUP();\n" +
            "    com_codename1_ui_Button___INIT_____java_lang_String(threadStateData, SP[-1].data.o, newStringFromCString(threadStateData, \"It works!\"));     SP -= 1;\n" +
            "    virtual_com_codename1_ui_Form_addComponent___com_codename1_ui_Component(threadStateData, SP[-2].data.o, SP[-1].data.o);     SP -= 1;\n" +
            "    virtual_com_codename1_ui_Form_show__(threadStateData, SP[-1].data.o);     SP -= 1;\n";
        s = s.substring(0, start) + body + s.substring(end);
        Files.write(launcherC, s.getBytes(StandardCharsets.UTF_8));
    }
}
