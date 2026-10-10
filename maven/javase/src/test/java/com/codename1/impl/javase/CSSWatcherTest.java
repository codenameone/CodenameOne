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
package com.codename1.impl.javase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSSWatcherTest {
    /// The notice a launch without the CSS compiler gets, spelled out rather
    /// than read from the class: the wording is what is being held.
    private static final String UNAVAILABLE_NOTICE = "Live CSS reload is unavailable: this build did not supply "
            + "the CSS compiler (cn1.css.cli.classpath). Launch the simulator through the Codename One Maven or "
            + "Gradle plugin.";

    private static final String[] PROPERTIES = {
        "user.dir", "user.home", "cn1.css.cli.classpath", "cn1.buildDir",
        "codename1.css.compiler.args.input", "codename1.css.compiler.args.output",
        "codename1.css.compiler.args.merge", "cn1.css.reload.noticePrinted",
        "cn1.css.reload.noArgsNoticePrinted"
    };

    private final Map<String, String> savedProperties = new HashMap<String, String>();
    private final PrintStream savedOut = System.out;
    private final PrintStream savedErr = System.err;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    /// Every test starts from a launch that supplied nothing: no compiler
    /// classpath, no compiler arguments, no notice printed yet.
    @BeforeEach
    void isolateLaunchProperties() {
        for (String name : PROPERTIES) {
            savedProperties.put(name, System.getProperty(name));
            if (!name.startsWith("user.")) {
                System.clearProperty(name);
            }
        }
        SimulatorProject.reset();
        // The port announces itself ("Retina Scale: ...") when its class is
        // first initialized; have that happen before any test captures output
        // and asserts on exactly what the watcher printed.
        JavaSEPort.getCWD();
    }

    @AfterEach
    void restoreLaunchProperties() {
        System.setOut(savedOut);
        System.setErr(savedErr);
        for (String name : PROPERTIES) {
            String value = savedProperties.get(name);
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        }
        SimulatorProject.reset();
    }

    private void captureOutput() {
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(err, true));
    }

    private String stdout() {
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private String stderr() {
        return new String(err.toByteArray(), StandardCharsets.UTF_8);
    }

    private static int occurrences(String haystack, String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) {
            count++;
        }
        return count;
    }

    private static File write(File dir, String name, String content) throws IOException {
        File f = new File(dir, name);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    /// The project the archetype generates, launched from its javase module.
    private static File mavenProject(File root) throws IOException {
        write(root, "pom.xml", "<project/>");
        write(root, "common/pom.xml", "<project/>");
        write(root, "common/codenameone_settings.properties", "codename1.cssTheme=true\n");
        write(root, "common/src/main/css/theme.css", "Button { color: red; }\n");
        write(root, "javase/pom.xml", "<project/>");
        return root;
    }

    private static File gradleProject(File root) throws IOException {
        write(root, "settings.gradle.kts", "rootProject.name = \"app\"\n");
        write(root, "build.gradle.kts", "plugins { id(\"com.codenameone\") }\n");
        write(root, "codenameone_settings.properties", "codename1.cssTheme=true\n");
        write(root, "src/main/css/theme.css", "Button { color: red; }\n");
        return root;
    }

    private static void launchFrom(File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
        SimulatorProject.reset();
    }

    private static File canonical(String path) throws IOException {
        return new File(path).getCanonicalFile();
    }

    /// A compiler that has already exited, with whatever it "printed".
    private static final class ExitedProcess extends Process {
        private final int exitCode;
        private final InputStream stdout;
        private final InputStream stderr;

        ExitedProcess(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = new ByteArrayInputStream(stdout.getBytes(StandardCharsets.UTF_8));
            this.stderr = new ByteArrayInputStream(stderr.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return stderr;
        }

        @Override
        public int waitFor() {
            return exitCode;
        }

        @Override
        public int exitValue() {
            return exitCode;
        }

        @Override
        public void destroy() {
        }
    }

    /// A watcher whose compiler is a scripted process and whose clock the test
    /// moves, so the restart bound is exercised without forking a JVM.
    private static class ScriptedCSSWatcher extends CSSWatcher {
        final List<List<String>> commands = new ArrayList<List<String>>();
        private final long millisPerRun;
        private final int stopAtStart;
        private long clock;

        /// @param millisPerRun how long each compiler "lives"
        /// @param stopAtStart the start at which the simulator goes away, or -1
        ScriptedCSSWatcher(long millisPerRun, int stopAtStart) {
            this.millisPerRun = millisPerRun;
            this.stopAtStart = stopAtStart;
        }

        @Override
        Process startChild(List<String> command) {
            commands.add(new ArrayList<String>(command));
            clock += millisPerRun;
            if (commands.size() == stopAtStart) {
                stop();
            }
            return new ExitedProcess(3, "Compiling\n", "Exception in thread \"main\" java.lang.IllegalStateException: "
                    + "bad bundle #" + commands.size() + "\n\tat com.example.Parser.parse(Parser.java:10)\n"
                    + "\t... 3 more\n");
        }

        @Override
        long nowMillis() {
            return clock;
        }
    }

    private static class TrackingCSSWatcher extends CSSWatcher {
        private int stopCount;

        @Override
        public void stop() {
            stopCount++;
            super.stop();
        }
    }

    @Test
    void supportDependsOnTheCompilerClasspathAlone(@TempDir Path tempDir) throws Exception {
        // A home directory whose UpdateStatus.properties reports a recent
        // designer: that file used to switch the watcher on by itself.
        File home = tempDir.resolve("home").toFile();
        write(home, ".codenameone/UpdateStatus.properties", "designer=99\n");
        System.setProperty("user.home", home.getAbsolutePath());
        File project = Files.createDirectories(tempDir.resolve("project")).toFile();
        launchFrom(project);

        assertFalse(CSSWatcher.isSupported(), "a designer install is not a CSS compiler");

        System.setProperty("cn1.css.cli.classpath", "  ");
        assertFalse(CSSWatcher.isSupported(), "a blank classpath supplies nothing");

        // And with no UpdateStatus.properties anywhere, the classpath is enough.
        System.setProperty("user.home", tempDir.resolve("no-such-home").toFile().getAbsolutePath());
        System.setProperty("cn1.css.cli.classpath", "css-cli.jar");
        assertTrue(CSSWatcher.isSupported());
    }

    @Test
    void projectSettingSwitchesTheWatcherOffButCannotSwitchItOn(@TempDir Path tempDir) throws Exception {
        File off = Files.createDirectories(tempDir.resolve("off")).toFile();
        write(off, "codenameone_settings.properties", "csswatcher.enabled=false\n");
        launchFrom(off);
        System.setProperty("cn1.css.cli.classpath", "css-cli.jar");
        assertFalse(CSSWatcher.isSupported(), "csswatcher.enabled=false wins over a supplied compiler");

        File on = Files.createDirectories(tempDir.resolve("on")).toFile();
        write(on, "codenameone_settings.properties", "csswatcher.enabled=TRUE\n");
        launchFrom(on);
        assertTrue(CSSWatcher.isSupported());
        System.clearProperty("cn1.css.cli.classpath");
        assertFalse(CSSWatcher.isSupported(), "csswatcher.enabled=true cannot stand in for the compiler");
    }

    @Test
    void missingCompilerIsReportedOncePerRun(@TempDir Path tempDir) throws Exception {
        File project = Files.createDirectories(tempDir.resolve("project")).toFile();
        write(project, "codenameone_settings.properties", "codename1.cssTheme=true\n");
        write(project, "css/theme.css", "Button { color: red; }\n");
        write(project, "css/dark-theme.css", "Button { color: blue; }\n");
        launchFrom(project);
        captureOutput();

        CSSWatcher.reportIfUnavailable();
        CSSWatcher.reportIfUnavailable();
        // A watcher started anyway, one per theme prefix, neither repeats the
        // notice nor starts a compiler.
        for (String prefix : new String[]{"", "dark-"}) {
            final List<List<String>> started = new ArrayList<List<String>>();
            CSSWatcher watcher = new CSSWatcher(prefix) {
                @Override
                Process startChild(List<String> command) {
                    started.add(command);
                    return new ExitedProcess(0, "", "");
                }
            };
            try {
                watcher.run();
            } finally {
                watcher.stop();
            }
            assertEquals(0, started.size(), "no compiler without a classpath");
        }

        assertEquals(1, occurrences(stdout(), UNAVAILABLE_NOTICE), stdout());
        assertEquals(UNAVAILABLE_NOTICE, stdout().trim(), "exactly the one line");
    }

    @Test
    void missingCompilerIsNotReportedWhenCssIsNotInUseOrSwitchedOff(@TempDir Path tempDir) throws Exception {
        captureOutput();
        File noCss = Files.createDirectories(tempDir.resolve("no-css")).toFile();
        write(noCss, "codenameone_settings.properties", "codename1.mainName=App\n");
        launchFrom(noCss);
        CSSWatcher.reportIfUnavailable();

        File switchedOff = Files.createDirectories(tempDir.resolve("switched-off")).toFile();
        write(switchedOff, "codenameone_settings.properties", "codename1.cssTheme=true\ncsswatcher.enabled=false\n");
        launchFrom(switchedOff);
        CSSWatcher.reportIfUnavailable();

        File supplied = Files.createDirectories(tempDir.resolve("supplied")).toFile();
        write(supplied, "codenameone_settings.properties", "codename1.cssTheme=true\n");
        launchFrom(supplied);
        System.setProperty("cn1.css.cli.classpath", "css-cli.jar");
        CSSWatcher.reportIfUnavailable();

        assertEquals("", stdout());
    }

    @Test
    void derivesTheMavenBuildsArgumentsFromTheProjectLayout(@TempDir Path tempDir) throws Exception {
        File root = mavenProject(tempDir.toFile());
        write(root, "common/src/main/css/dark-theme.css", "Button { color: blue; }\n");
        launchFrom(new File(root, "javase"));
        File common = new File(root, "common");

        // What PrepareSimulatorClasspathMojo passes for the same project.
        CSSWatcher.CompilerArgs args = new CSSWatcher().resolveCompilerArgs();
        assertNotNull(args);
        assertEquals(new File(common, "src/main/css/theme.css").getCanonicalFile(), canonical(args.input));
        assertEquals(new File(common, "target/classes/theme.res").getCanonicalFile(), canonical(args.output));
        assertEquals(new File(common, "target/css/theme.css").getCanonicalFile(), canonical(args.merge));

        // And what CssCompiler names them for a prefixed theme.
        CSSWatcher.CompilerArgs dark = new CSSWatcher("dark-").resolveCompilerArgs();
        assertNotNull(dark);
        assertEquals(new File(common, "src/main/css/dark-theme.css").getCanonicalFile(), canonical(dark.input));
        assertEquals(new File(common, "target/classes/dark-theme.res").getCanonicalFile(), canonical(dark.output));
        assertEquals(new File(common, "target/css/dark-theme.css").getCanonicalFile(), canonical(dark.merge));
    }

    @Test
    void derivesTheGradleBuildsArgumentsIncludingAMovedBuildDirectory(@TempDir Path tempDir) throws Exception {
        File root = gradleProject(tempDir.resolve("app").toFile());
        launchFrom(root);

        CSSWatcher.CompilerArgs args = new CSSWatcher().resolveCompilerArgs();
        assertNotNull(args);
        assertEquals(new File(root, "src/main/css/theme.css").getCanonicalFile(), canonical(args.input));
        assertEquals(new File(root, "build/resources/main/theme.res").getCanonicalFile(), canonical(args.output));
        assertEquals(new File(root, "build/css/theme.css").getCanonicalFile(), canonical(args.merge));

        File moved = tempDir.resolve("elsewhere").toFile();
        System.setProperty("cn1.buildDir", moved.getAbsolutePath());
        SimulatorProject.reset();
        CSSWatcher.CompilerArgs movedArgs = new CSSWatcher().resolveCompilerArgs();
        assertNotNull(movedArgs);
        assertEquals(new File(moved, "resources/main/theme.res").getCanonicalFile(), canonical(movedArgs.output));
        assertEquals(new File(moved, "css/theme.css").getCanonicalFile(), canonical(movedArgs.merge));
    }

    @Test
    void aThemeTheLayoutDoesNotHaveIsNotWatched(@TempDir Path tempDir) throws Exception {
        File root = mavenProject(tempDir.toFile());
        launchFrom(new File(root, "javase"));
        captureOutput();

        assertNull(new CSSWatcher("dark-").resolveCompilerArgs());
        assertTrue(stdout().contains("dark-theme.css does not exist.  Not activating CSS watcher for prefix dark-"),
                stdout());
    }

    @Test
    void argumentsTheBuildPassedWinOverTheLayout(@TempDir Path tempDir) throws Exception {
        File root = mavenProject(tempDir.toFile());
        launchFrom(new File(root, "javase"));
        String sep = File.separator;
        System.setProperty("codename1.css.compiler.args.input",
                sep + "libs" + sep + "theme.css," + sep + "app" + sep + "theme.css");
        System.setProperty("codename1.css.compiler.args.output", sep + "out" + sep + "theme.res");
        System.setProperty("codename1.css.compiler.args.merge", sep + "work" + sep + "theme.css");

        CSSWatcher.CompilerArgs args = new CSSWatcher("dark-").resolveCompilerArgs();
        assertNotNull(args);
        assertEquals(sep + "libs" + sep + "dark-theme.css," + sep + "app" + sep + "dark-theme.css", args.input);
        assertEquals(sep + "out" + sep + "dark-theme.res", args.output);
        assertEquals(sep + "work" + sep + "dark-theme.css", args.merge);
    }

    @Test
    void aLibraryWithoutThePrefixedThemeIsLeftOutOfItsInputs(@TempDir Path tempDir) throws Exception {
        File root = mavenProject(tempDir.toFile());
        launchFrom(new File(root, "javase"));
        File lib = new File(tempDir.toFile(), "lib/theme.css");
        File app = new File(tempDir.toFile(), "app/theme.css");
        File appDark = new File(tempDir.toFile(), "app/dark-theme.css");
        for (File f : new File[] {lib, app, appDark}) {
            f.getParentFile().mkdirs();
            java.nio.file.Files.write(f.toPath(), new byte[0]);
        }
        System.setProperty("codename1.css.compiler.args.input", lib.getPath() + "," + app.getPath());
        System.setProperty("codename1.css.compiler.args.output", new File(tempDir.toFile(), "theme.res").getPath());
        System.setProperty("codename1.css.compiler.args.merge", new File(tempDir.toFile(), "theme.css").getPath());

        assertEquals(lib.getPath() + "," + app.getPath(), new CSSWatcher().resolveCompilerArgs().input);
        assertEquals(appDark.getPath(), new CSSWatcher("dark-").resolveCompilerArgs().input,
                "only the application has a dark theme");
    }

    @Test
    void anUnexpandedPomPlaceholderIsNotAnArgument(@TempDir Path tempDir) throws Exception {
        File root = mavenProject(tempDir.toFile());
        launchFrom(new File(root, "javase"));
        System.setProperty("codename1.css.compiler.args.input", "${codename1.css.compiler.args.input}");
        System.setProperty("codename1.css.compiler.args.output", "${codename1.css.compiler.args.output}");
        System.setProperty("codename1.css.compiler.args.merge", "${codename1.css.compiler.args.merge}");

        CSSWatcher.CompilerArgs args = new CSSWatcher().resolveCompilerArgs();
        assertNotNull(args);
        assertEquals(new File(root, "common/src/main/css/theme.css").getCanonicalFile(), canonical(args.input));
        assertEquals(new File(root, "common/target/classes/theme.res").getCanonicalFile(), canonical(args.output));
    }

    @Test
    void saysWhyWhenArgumentsCanNeitherBeReadNorDerived(@TempDir Path tempDir) throws Exception {
        File nowhere = Files.createDirectories(tempDir.resolve("nowhere")).toFile();
        launchFrom(nowhere);
        captureOutput();

        assertNull(new CSSWatcher().resolveCompilerArgs());
        assertNull(new CSSWatcher("dark-").resolveCompilerArgs());

        String printed = stdout();
        assertEquals(1, occurrences(printed, "Live CSS reload is unavailable: the launch passed no CSS compiler "
                + "arguments (codename1.css.compiler.args.input, codename1.css.compiler.args.output, "
                + "codename1.css.compiler.args.merge) and no Codename One project was found around "
                + nowhere.getAbsolutePath()), printed);
        assertEquals(1, printed.trim().split("\n").length, printed);
    }

    @Test
    void blankArgumentsFromAProjectWithoutAThemeStartNothing(@TempDir Path tempDir) throws Exception {
        launchFrom(Files.createDirectories(tempDir.resolve("project")).toFile());
        System.setProperty("codename1.css.compiler.args.input", "");
        System.setProperty("codename1.css.compiler.args.output", "");
        System.setProperty("codename1.css.compiler.args.merge", "");
        captureOutput();

        assertNull(new CSSWatcher("dark-").resolveCompilerArgs());
        assertTrue(stdout().contains("Not activating CSS watcher for prefix dark-"), stdout());
    }

    @Test
    void commandLineRunsTheHeadlessCompilerOnTheSuppliedClasspath(@TempDir Path tempDir) throws Exception {
        File project = Files.createDirectories(tempDir.resolve("project")).toFile();
        launchFrom(project);
        String classpath = "css-cli.jar" + File.pathSeparator + "flute.jar";
        CSSWatcher.CompilerArgs compilerArgs = new CSSWatcher.CompilerArgs(
                new File(project, "styles/theme.css").getAbsolutePath(),
                new File(project, "out/theme.res").getAbsolutePath(),
                new File(project, "work/theme.css").getAbsolutePath());

        List<String> command = new CSSWatcher().buildCommand(classpath, 4321, compilerArgs);

        assertEquals(Arrays.asList(
                new File(System.getProperty("java.home"), "bin/java").getAbsolutePath(),
                "-Djava.awt.headless=true",
                "-Dcli=true",
                "-Dparent.port=4321",
                "-cp", classpath,
                "com.codename1.designer.css.CN1CSSCLI",
                "-input", compilerArgs.input,
                "-output", compilerArgs.output,
                "-merge", compilerArgs.merge,
                "-watch"), command);
        assertNoDesignerLaunch(command);
    }

    @Test
    void commandLineCarriesAMovedBuildDirectoryAndTheLocalizationBundles(@TempDir Path tempDir) throws Exception {
        File project = Files.createDirectories(tempDir.resolve("project")).toFile();
        File l10n = Files.createDirectories(project.toPath().resolve("src/main/l10n")).toFile();
        File themeCss = write(project, "src/main/css/theme.css", "Button { color: red; }\n");
        launchFrom(project);
        File moved = tempDir.resolve("elsewhere").toFile();
        System.setProperty("cn1.buildDir", moved.getAbsolutePath());

        List<String> command = new CSSWatcher().buildCommand("css-cli.jar", 7,
                new CSSWatcher.CompilerArgs(themeCss.getAbsolutePath(), "theme.res", "merged.css"));

        int buildDir = command.indexOf("-Dcn1.buildDir=" + moved.getAbsolutePath());
        assertTrue(buildDir > 0 && buildDir < command.indexOf("-cp"), "a JVM option, before the classpath: " + command);
        assertEquals(command.size() - 3, command.indexOf("-l"), command.toString());
        assertEquals(l10n.getAbsolutePath(), command.get(command.indexOf("-l") + 1));
        assertEquals("-watch", command.get(command.size() - 1));
        assertNoDesignerLaunch(command);
    }

    private static void assertNoDesignerLaunch(List<String> command) {
        assertFalse(command.contains("-jar"), command.toString());
        assertFalse(command.contains("-css"), command.toString());
        assertFalse(command.contains("-Dprism.order=sw"), command.toString());
        for (String arg : command) {
            if (!"com.codename1.designer.css.CN1CSSCLI".equals(arg)) {
                assertFalse(arg.contains("designer"), "only the compiler's own class may name the designer: " + arg);
            }
        }
    }

    private static void supplyCompiler(File project) {
        System.setProperty("cn1.css.cli.classpath", "css-cli.jar");
        System.setProperty("codename1.css.compiler.args.input", new File(project, "theme.css").getAbsolutePath());
        System.setProperty("codename1.css.compiler.args.output", new File(project, "theme.res").getAbsolutePath());
        System.setProperty("codename1.css.compiler.args.merge", new File(project, "merged.css").getAbsolutePath());
    }

    @Test
    void aCompilerThatKeepsDyingIsStartedThreeTimesThenLeftAlone(@TempDir Path tempDir) throws Exception {
        File project = Files.createDirectories(tempDir.resolve("project")).toFile();
        launchFrom(project);
        supplyCompiler(project);
        captureOutput();

        // One second per run: three exits inside the ten second window.
        ScriptedCSSWatcher watcher = new ScriptedCSSWatcher(1000L, -1);
        try {
            watcher.run();
        } finally {
            watcher.stop();
        }

        assertEquals(3, watcher.commands.size(), "started three times and no more");
        List<String> command = watcher.commands.get(0);
        assertTrue(command.contains("-Djava.awt.headless=true"), command.toString());
        assertTrue(command.contains("com.codename1.designer.css.CN1CSSCLI"), command.toString());
        assertEquals("-watch", command.get(command.size() - 1));
        assertEquals("css-cli.jar", command.get(command.indexOf("-cp") + 1));
        assertEquals(new File(project, "theme.css").getAbsolutePath(), command.get(command.indexOf("-input") + 1));
        assertEquals(new File(project, "theme.res").getAbsolutePath(), command.get(command.indexOf("-output") + 1));
        assertEquals(new File(project, "merged.css").getAbsolutePath(), command.get(command.indexOf("-merge") + 1));
        assertNoDesignerLaunch(command);

        String stopped = "Live CSS reload stopped for theme.css: the CSS compiler exited 3 times within 10 seconds "
                + "(last exit code 3). Its last error output: Exception in thread \"main\" "
                + "java.lang.IllegalStateException: bad bundle #3";
        assertEquals(1, occurrences(stderr(), stopped), stderr());
        // The compiler's own output is still echoed, every run of it.
        assertEquals(3, occurrences(stdout(), "CSS> Compiling"), stdout());
        assertEquals(3, occurrences(stderr(), "CSS> Exception in thread"), stderr());
    }

    @Test
    void aCompilerThatDiesNowAndThenKeepsBeingRestarted(@TempDir Path tempDir) throws Exception {
        File project = Files.createDirectories(tempDir.resolve("project")).toFile();
        launchFrom(project);
        supplyCompiler(project);
        captureOutput();

        // Six seconds per run: no three exits ever share a ten second window,
        // so only the simulator going away (at the seventh start) ends it.
        ScriptedCSSWatcher watcher = new ScriptedCSSWatcher(6000L, 7);
        watcher.run();

        assertEquals(7, watcher.commands.size());
        assertFalse(stderr().contains("Live CSS reload stopped"), stderr());
    }

    @Test
    void theRestartBoundIsMeasuredOverTheLastThreeExits(@TempDir Path tempDir) throws Exception {
        File project = Files.createDirectories(tempDir.resolve("project")).toFile();
        launchFrom(project);
        supplyCompiler(project);
        captureOutput();

        // Slow at first (two exits twenty seconds apart), then three in two
        // seconds: the early exits must not count against the later ones, and
        // the later ones must still trip the bound.
        final long[] lifetimes = {1000L, 20000L, 1000L, 1000L, 1000L, 1000L};
        final List<List<String>> commands = new ArrayList<List<String>>();
        final long[] clock = {0L};
        CSSWatcher watcher = new CSSWatcher() {
            @Override
            Process startChild(List<String> command) {
                clock[0] += lifetimes[commands.size()];
                commands.add(command);
                return new ExitedProcess(1, "", "");
            }

            @Override
            long nowMillis() {
                return clock[0];
            }
        };
        try {
            watcher.run();
        } finally {
            watcher.stop();
        }

        // Exits at 1s, 21s, 22s, 23s: the fourth is the third inside ten seconds.
        assertEquals(4, commands.size());
        assertTrue(stderr().contains("Live CSS reload stopped for theme.css: the CSS compiler exited 3 times within "
                + "10 seconds (last exit code 1). Its last error output: (none)"), stderr());
    }

    @Test
    void addsLocalizationArgumentForProjectL10nDirectory(@TempDir Path tempDir) throws Exception {
        Path projectDir = tempDir.resolve("project");
        Path cssDir = Files.createDirectories(projectDir.resolve("css"));
        Path l10nDir = Files.createDirectories(projectDir.resolve("l10n"));
        Path cssFile = Files.createFile(cssDir.resolve("theme.css"));

        CSSWatcher watcher = new CSSWatcher();
        List<String> args = new ArrayList<String>();

        watcher.addLocalizationArgument(args, cssFile.toFile(), null);

        assertEquals(2, args.size());
        assertEquals("-l", args.get(0));
        assertEquals(l10nDir.toFile().getAbsolutePath(), args.get(1));
    }

    @Test
    void addsLocalizationArgumentForOverrideInputInCommonModule(@TempDir Path tempDir) throws Exception {
        Path javaseDir = tempDir.resolve("javase");
        Path commonDir = tempDir.resolve("common");
        Path cssDir = Files.createDirectories(commonDir.resolve("src/main/css"));
        Path l10nDir = Files.createDirectories(commonDir.resolve("src/main/l10n"));
        Path cssFile = Files.createFile(cssDir.resolve("theme.css"));
        Files.createDirectories(javaseDir);

        String oldUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", javaseDir.toFile().getAbsolutePath());
        try {
            CSSWatcher watcher = new CSSWatcher();
            List<String> args = new ArrayList<String>();

            watcher.addLocalizationArgument(args, new File("css/theme.css"), cssFile.toFile().getAbsolutePath());

            assertTrue(args.contains("-l"));
            assertEquals(l10nDir.toFile().getAbsolutePath(), args.get(args.indexOf("-l") + 1));
        } finally {
            System.setProperty("user.dir", oldUserDir);
        }
    }

    @Test
    void simulatorReloadStopsEachRegisteredCSSWatcherOnce() {
        TrackingCSSWatcher first = new TrackingCSSWatcher();
        TrackingCSSWatcher second = new TrackingCSSWatcher();
        int simulatorReloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
        assertTrue(Executor.registerCSSWatcher(first, simulatorReloadVersion));
        assertTrue(Executor.registerCSSWatcher(second, simulatorReloadVersion));

        Executor.cleanupForSimulatorReload();
        Executor.cleanupForSimulatorReload();

        assertEquals(1, first.stopCount);
        assertEquals(1, second.stopCount);
    }

    @Test
    void staleSimulatorGenerationCannotRegisterDelayedCSSWatcher() {
        int simulatorReloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
        TrackingCSSWatcher watcher = new TrackingCSSWatcher();
        try {
            assertFalse(Executor.registerCSSWatcher(watcher, simulatorReloadVersion + 1));
        } finally {
            watcher.stop();
        }
    }
}
