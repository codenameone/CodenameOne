/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.project.BuildSystem;
import com.codename1.project.ProjectLayout;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSSWatcher implements Runnable {
    private int simulatorReloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
    private volatile Thread watchThread;
    private Thread pulseThread;
    private ServerSocket pulseSocket;
    private volatile Process childProcess;
    private volatile boolean closing;
    private final Thread shutdownHook;

    /**
     * Classpath of the headless CSS compiler CLI, published by the Maven and Gradle
     * plugins into the build directory's codenameone/simulator.properties and
     * loaded into the System properties by {@link Simulator}. Kept in sync with
     * SimulatorSupport.CSS_CLI_CLASSPATH_PROPERTY / CssCompiler.CSS_CLI_MAIN_CLASS
     * -- the port cannot see the build engine's constants.
     *
     * <p>This is the only way the compiler is found. There is no fallback to the
     * Resource Editor ("designer") jar: a launch that does not supply the classpath
     * gets no live CSS reload, and says so once.</p>
     */
    static final String CSS_CLI_CLASSPATH_PROPERTY = "cn1.css.cli.classpath";
    static final String CSS_CLI_MAIN_CLASS = "com.codename1.designer.css.CN1CSSCLI";

    /** What the build compiled, as SimulatorSupport names them. */
    static final String CSS_INPUT_PROPERTY = "codename1.css.compiler.args.input";
    static final String CSS_OUTPUT_PROPERTY = "codename1.css.compiler.args.output";
    static final String CSS_MERGE_PROPERTY = "codename1.css.compiler.args.merge";

    static final String UNAVAILABLE_NOTICE = "Live CSS reload is unavailable: this build did not supply "
            + "the CSS compiler (" + CSS_CLI_CLASSPATH_PROPERTY + "). Launch the simulator through the "
            + "Codename One Maven or Gradle plugin.";

    /**
     * Set once the notice above has been printed. A system property rather than a
     * static field because a simulator reload loads this class again in a new
     * class loader, and one watcher is created per theme prefix: the notice is
     * about the launch, so it is printed once per JVM.
     */
    static final String NOTICE_PRINTED_PROPERTY = "cn1.css.reload.noticePrinted";

    /**
     * A compiler that dies is started again, but not for ever: this many exits
     * inside {@link #RAPID_EXIT_WINDOW_MILLIS} means it cannot run at all (a
     * stylesheet or bundle it crashes on, a broken classpath), and restarting it
     * only repeats the same failure. See JavaSEPort.findLocalizationDirectory for
     * the time an unbounded restart turned one bad bundle into an endless loop
     * of JVM launches.
     */
    static final int MAX_RAPID_EXITS = 3;
    static final long RAPID_EXIT_WINDOW_MILLIS = 10000L;

    /** How much of the compiler's error output is kept to explain a failure. */
    private static final int ERROR_TAIL_LINES = 20;

    private final String themePrefix;

    public CSSWatcher() {
        this("");
    }

    public CSSWatcher(String themePrefix) {
        this.themePrefix = themePrefix;
        shutdownHook = new Thread(new Runnable() {

            @Override
            public void run() {
                if (childProcess != null && childProcess.isAlive()) {
                    try {
                        closing = true;
                        childProcess.destroyForcibly();
                    } catch (Throwable t){}
                }
            }

        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public void stop() {
        closing = true;
        if (childProcess != null && childProcess.isAlive()) {
            try {
                childProcess.destroyForcibly();
            } catch (Throwable t){}
        }
        if (pulseSocket != null && !pulseSocket.isClosed()) {
            try {
                pulseSocket.close();
            } catch (Exception ex){}
        }
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (Throwable t) {
            // The JVM is already shutting down, or the hook was already removed.
        }
    }
    
    /**
     * Checks whether the CSS watcher is supported currently: the launch supplied
     * the CSS compiler's classpath ({@literal cn1.css.cli.classpath}), which the
     * Maven and Gradle plugins do and an Ant launch does not.
     *
     * <p>Setting {@literal csswatcher.enabled=false} in the
     * codenameone_settings.properties file switches it off. Any other value leaves
     * the answer to the classpath: the setting cannot conjure a compiler the launch
     * did not supply.</p>
     */
    public static boolean isSupported() {
        return !isDisabledInSettings() && cssCliClasspath() != null;
    }

    /**
     * Says, once per simulator run, that live CSS reload is not available because
     * the launch did not supply the compiler. Silent when the project does not use
     * CSS or switched the watcher off itself: neither is missing anything.
     */
    static void reportIfUnavailable() {
        if (cssCliClasspath() != null || isDisabledInSettings() || !isCssInUse()) {
            return;
        }
        if (System.getProperty(NOTICE_PRINTED_PROPERTY) != null) {
            return;
        }
        System.setProperty(NOTICE_PRINTED_PROPERTY, "true");
        System.out.println(UNAVAILABLE_NOTICE);
    }

    /** The CSS compiler's classpath, or null when the launch supplied none. */
    private static String cssCliClasspath() {
        String classpath = System.getProperty(CSS_CLI_CLASSPATH_PROPERTY, null);
        return classpath == null || classpath.trim().isEmpty() ? null : classpath;
    }

    /** The project's settings, empty when it has none or they cannot be read. */
    private static Properties projectSettings() {
        Properties cn1Properties = new Properties();
        File cn1Props = JavaSEPort.projectSettingsFile();
        if (cn1Props.exists()) {
            try (InputStream input = new FileInputStream(cn1Props)) {
                cn1Properties.load(input);
            } catch (IOException ex) {
                Logger.getLogger(CSSWatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return cn1Properties;
    }

    /** An explicit csswatcher.enabled that is anything but "true" switches the watcher off. */
    private static boolean isDisabledInSettings() {
        String cssWatcherEnabled = projectSettings().getProperty("csswatcher.enabled", null);
        return cssWatcherEnabled != null && !"true".equalsIgnoreCase(cssWatcherEnabled);
    }

    private static boolean isCssInUse() {
        String cssTheme = projectSettings().getProperty("codename1.cssTheme", null);
        return cssTheme != null && "true".equalsIgnoreCase(cssTheme.trim());
    }

    
    /**
     * Resolves the first existing localization directory to pass to the CSS compiler.
     * <p>This accepts both standard project layout and override-input mode so watch mode
     * can produce unified resources in single-module and multi-module projects.</p>
     */
    File findLocalizationDirectory(File srcFile, String overrideInputs) {
        List<File> localizationDirectories = new ArrayList<File>();
        if (overrideInputs != null) {
            for (String input : overrideInputs.split(",")) {
                String trimmed = input.trim();
                if (!trimmed.isEmpty()) {
                    addLocalizationCandidates(new File(trimmed), localizationDirectories);
                }
            }
        } else {
            addLocalizationCandidates(srcFile, localizationDirectories);
        }

        for (File directory : localizationDirectories) {
            if (directory != null && directory.isDirectory()) {
                return directory;
            }
        }
        return null;
    }

    /**
     * Adds likely l10n/i18n locations derived from the CSS file and current working directory.
     * <p>We include relative common-module paths because JavaSE watch is often launched
     * from the `javase` module while CSS/localization resources are in `../common/src/main`.</p>
     */
    void addLocalizationCandidates(File cssFile, List<File> out) {
        if (cssFile == null) {
            return;
        }
        File cssDirectory = cssFile.getParentFile();
        if (cssDirectory != null) {
            File srcMainDirectory = cssDirectory.getParentFile();
            if (srcMainDirectory != null) {
                out.add(new File(srcMainDirectory, "l10n"));
                out.add(new File(srcMainDirectory, "i18n"));
            }
        }

        File workingDirectory = new File(System.getProperty("user.dir"));
        out.add(new File(workingDirectory, "l10n"));
        out.add(new File(workingDirectory, "i18n"));
        out.add(new File(workingDirectory, "src/main/l10n"));
        out.add(new File(workingDirectory, "src/main/i18n"));
        out.add(new File(workingDirectory, "../common/src/main/l10n"));
        out.add(new File(workingDirectory, "../common/src/main/i18n"));
        // Last, the project model's own answer, for a launch directory none of
        // the guesses above fit (a Maven project started at its root).
        ProjectLayout layout = SimulatorProject.current();
        if (layout != null) {
            out.add(layout.l10nDir());
            out.add(new File(layout.l10nDir().getParentFile(), "i18n"));
        }
    }

    /**
     * Appends `-l <dir>` to the CSS compiler arguments when a localization directory exists.
     */
    void addLocalizationArgument(List<String> args, File srcFile, String overrideInputs) {
        File localizationDirectory = findLocalizationDirectory(srcFile, overrideInputs);
        if (localizationDirectory != null) {
            args.add("-l");
            args.add(localizationDirectory.getAbsolutePath());
        }
    }

    public String unescapeXSI(final String s) throws IOException {
        StringBuilder sb = new StringBuilder();


        int segmentStart = 0;
        int searchOffset = 0;
        while (true) {
            final int pos = s.indexOf('\\', searchOffset);
            if (pos == -1) {
                if (segmentStart < s.length()) {
                    sb.append(s.substring(segmentStart));
                }
                break;
            }
            if (pos > segmentStart) {
                sb.append(s.substring(segmentStart, pos));
            }
            segmentStart = pos + 1;
            searchOffset = pos + 2;
        }

        return sb.toString();
    }

    /** The three file arguments of one compiler run. */
    static final class CompilerArgs {
        final String input;
        final String output;
        final String merge;

        CompilerArgs(String input, String output, String merge) {
            this.input = input;
            this.output = output;
            this.merge = merge;
        }
    }

    /**
     * The last lines the compiler wrote to its error stream, so that a compiler
     * that cannot stay up can be reported with its own explanation.
     */
    private static final class ErrorTail {
        private final ArrayDeque<String> lines = new ArrayDeque<String>();

        synchronized void add(String line) {
            if (lines.size() == ERROR_TAIL_LINES) {
                lines.removeFirst();
            }
            lines.addLast(line);
        }

        /**
         * The last line that says something. A crashing JVM ends its output with
         * the stack frames, so the literal last line is "at ...main(...)"; the
         * line above the frames is the exception and its message. Falls back to
         * the literal last line when everything kept is a frame.
         */
        synchronized String lastMeaningfulLine() {
            String last = null;
            Iterator<String> it = lines.descendingIterator();
            while (it.hasNext()) {
                String line = it.next().trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (last == null) {
                    last = line;
                }
                if (!line.startsWith("at ") && !line.startsWith("...")) {
                    return line;
                }
            }
            return last;
        }
    }

    /** Prints a line about the launch once per JVM, however many watchers find it true. */
    private static void printOnce(String key, String message) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, "true");
            System.out.println(message);
        }
    }

    private static String launchProperty(String name) {
        String value = System.getProperty(name, null);
        // A pom that forwards -Dname=${name} while the property is undefined hands
        // over the unexpanded placeholder, which is not a value.
        return value == null || value.contains("${") ? null : value;
    }

    /**
     * Where the build writes theme.res. Maven and Gradle compile it into the
     * resources the simulator runs from (target/classes, build/resources/main);
     * an Ant project keeps it beside its sources.
     */
    static File derivedOutput(ProjectLayout layout) {
        if (layout.buildSystem() == BuildSystem.ANT) {
            return new File(layout.resourcesDir(), "theme.res");
        }
        return new File(layout.resourcesOutputDir(), "theme.res");
    }

    /**
     * Where the build writes the merged stylesheet: css/theme.css under the build
     * directory, which is what CssCompiler, PrepareSimulatorClasspathMojo and the
     * Gradle plugin's run task all name. Deliberately not
     * ProjectLayout.cssMergeFile() for those two: that answers theme.css.merged,
     * and a watcher writing a second merge file would not share the build's
     * up-to-date state. Ant has no such directory and does use it.
     */
    static File derivedMerge(ProjectLayout layout) {
        if (layout.buildSystem() == BuildSystem.ANT) {
            return layout.cssMergeFile(layout.themeCss());
        }
        return new File(new File(layout.buildDir(), "css"), "theme.css");
    }

    /**
     * What to compile for this watcher's theme prefix, or null -- after saying
     * why -- when there is nothing to watch.
     *
     * <p>The build passes the unprefixed theme's files as
     * codename1.css.compiler.args.*. A launch that carries the compiler's
     * classpath but not those (simulator.properties was loaded, the pom's
     * arguments were not: an IDE run configuration) gets the same values from
     * the project layout instead. The one thing the layout cannot supply is the
     * stylesheets of the project's cn1libs, which the build resolves from its
     * dependencies and merges first, so a derived input is the application's
     * own theme.css alone.</p>
     *
     * <p>Every file takes the theme prefix, the merge file included: the build
     * names it [prefix]theme.css too, and two watchers sharing one merge file
     * would overwrite each other's.</p>
     */
    CompilerArgs resolveCompilerArgs() {
        String input = launchProperty(CSS_INPUT_PROPERTY);
        String output = launchProperty(CSS_OUTPUT_PROPERTY);
        String merge = launchProperty(CSS_MERGE_PROPERTY);
        if (input == null || output == null || merge == null) {
            ProjectLayout layout = SimulatorProject.current();
            if (layout == null) {
                printOnce("cn1.css.reload.noArgsNoticePrinted", "Live CSS reload is unavailable: the launch "
                        + "passed no CSS compiler arguments (" + CSS_INPUT_PROPERTY + ", " + CSS_OUTPUT_PROPERTY
                        + ", " + CSS_MERGE_PROPERTY + ") and no Codename One project was found around "
                        + JavaSEPort.getCWD().getAbsolutePath() + " to derive them from.");
                return null;
            }
            if (input == null) {
                File themeCss = layout.themeCss();
                if (!prefixFile(themePrefix, themeCss).exists()) {
                    System.out.println("CSS file " + prefixFile(themePrefix, themeCss)
                            + " does not exist.  Not activating CSS watcher for prefix " + themePrefix);
                    return null;
                }
                input = themeCss.getAbsolutePath();
            }
            if (output == null) {
                output = derivedOutput(layout).getAbsolutePath();
            }
            if (merge == null) {
                merge = derivedMerge(layout).getAbsolutePath();
            }
        }
        if (input.trim().isEmpty() || output.trim().isEmpty() || merge.trim().isEmpty()) {
            // The Maven plugin passes all three blank for a project without a theme.css.
            System.out.println("CSS file " + input + " does not exist.  Not activating CSS watcher for prefix "
                    + themePrefix);
            return null;
        }
        return new CompilerArgs(prefixInputs(themePrefix, input),
                prefixFile(themePrefix, new File(output)).getPath(),
                prefixFile(themePrefix, new File(merge)).getPath());
    }

    /**
     * The command line of the CSS compiler in watch mode.
     *
     * <p>The compiler is headless: it opens no window and needs no display, and
     * java.awt.headless keeps the child from showing up as a second application
     * (a Dock icon on macOS) or failing where there is no display at all.</p>
     */
    List<String> buildCommand(String cssCliClasspath, int parentPort, CompilerArgs compilerArgs) {
        File javaBin = new File(System.getProperty("java.home"), "bin/java");
        List<String> args = new ArrayList<String>();
        args.add(javaBin.getAbsolutePath());
        args.add("-Djava.awt.headless=true");
        args.add("-Dcli=true");
        args.add("-Dparent.port=" + parentPort);
        // A build that moved its build directory told the simulator where; the
        // compiler keeps its checksums there and cannot find it from the
        // directory tree either.
        String buildDir = System.getProperty(SimulatorProject.BUILD_DIR_PROPERTY);
        if (buildDir != null && buildDir.length() > 0) {
            args.add("-D" + SimulatorProject.BUILD_DIR_PROPERTY + "=" + buildDir);
        }
        args.add("-cp");
        args.add(cssCliClasspath);
        args.add(CSS_CLI_MAIN_CLASS);
        args.add("-input");
        args.add(compilerArgs.input);
        args.add("-output");
        args.add(compilerArgs.output);
        args.add("-merge");
        args.add(compilerArgs.merge);
        addLocalizationArgument(args, null, compilerArgs.input);
        args.add("-watch");
        return args;
    }

    /** Starts the compiler. A seam so tests can supply a process without forking a JVM. */
    Process startChild(List<String> command) throws IOException {
        return new ProcessBuilder(command).start();
    }

    /** A monotonic clock, in milliseconds. A seam for the restart bound's tests. */
    long nowMillis() {
        return System.nanoTime() / 1000000L;
    }

    private boolean simulatorReloaded() {
        int reloadVersion = Integer.parseInt(System.getProperty("reload.simulator.count", "0"));
        return reloadVersion != simulatorReloadVersion;
    }

    private void watch() throws IOException {
        if (closing) {
            return;
        }
        String cssCliClasspath = cssCliClasspath();
        if (cssCliClasspath == null) {
            reportIfUnavailable();
            return;
        }
        final CompilerArgs compilerArgs = resolveCompilerArgs();
        if (compilerArgs == null) {
            return;
        }
        System.out.println("Watching CSS files for changes: ["+compilerArgs.input+"]");
        if (pulseSocket == null || pulseSocket.isClosed()) {
            // If the the Simulator is killed then the shutdown hook doesn't run
            // so we need an alternative way for the CSS compiler to know that
            // the parent is dead so that it will close itself.
            // So we create a ServerSocket to serve as a "pulse".
            // We pass the port to the CSS compiler so that it can connect.
            // When the socket is disconnected for any reason, the compiler will exit.
            pulseSocket = new ServerSocket(0);
            pulseThread = new Thread(new Runnable() {
                public void run() {
                    while (!closing) {
                        try {
                            Socket clientSocket = pulseSocket.accept();

                        } catch (IOException ex) {
                            Logger.getLogger(CSSWatcher.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }
            });
            pulseThread.setDaemon(true);

        }
        List<String> args = buildCommand(cssCliClasspath, pulseSocket.getLocalPort(), compilerArgs);
        System.out.println("Running CSS watch with args " + args);

        // When the compiler last exited, as a ring: with MAX_RAPID_EXITS entries
        // recorded, the slot about to be overwritten is the oldest of them.
        long[] exitTimes = new long[MAX_RAPID_EXITS];
        int exits = 0;
        while (!closing) {
            if (simulatorReloaded()) {
                stop();
                return;
            }
            Process previousProcess = childProcess;
            if (previousProcess != null && previousProcess.isAlive()) {
                try {
                    previousProcess.destroyForcibly();
                } catch (Throwable t){}
            }
            Process p = startChild(args);
            childProcess = p;
            if (closing) {
                stop();
                return;
            }
            ErrorTail errors = new ErrorTail();
            Thread errorPump = pumpErrors(p, errors);
            pumpOutput(p, compilerArgs);

            // Output has ended, so the compiler is gone or going. Waiting on the
            // process (rather than polling it) costs nothing while it lives and
            // is ended by stop(), which destroys it.
            int exitCode;
            try {
                exitCode = p.waitFor();
                // Its last words may still be in the pipe; bounded, because a
                // grandchild holding the stream open must not hold this up.
                errorPump.join(2000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                p.destroyForcibly();
                return;
            }
            if (closing) {
                return;
            }
            long now = nowMillis();
            exitTimes[exits % MAX_RAPID_EXITS] = now;
            exits++;
            if (exits >= MAX_RAPID_EXITS && now - exitTimes[exits % MAX_RAPID_EXITS] <= RAPID_EXIT_WINDOW_MILLIS) {
                String lastError = errors.lastMeaningfulLine();
                System.err.println("Live CSS reload stopped for " + themePrefix + "theme.css: the CSS compiler exited "
                        + MAX_RAPID_EXITS + " times within " + (RAPID_EXIT_WINDOW_MILLIS / 1000L)
                        + " seconds (last exit code " + exitCode + "). Its last error output: "
                        + (lastError == null ? "(none)" : lastError));
                return;
            }
        }
    }

    /** Echoes the compiler's error stream and keeps its tail. */
    private Thread pumpErrors(Process p, final ErrorTail errors) {
        final BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        Thread errorPump = new Thread(new Runnable() {
            public void run() {

                while (true) {
                    if (simulatorReloaded()) {
                        stop();
                        break;
                    }
                    try {
                        String l = errorReader.readLine();
                        if (l != null) {
                            errors.add(l);
                            System.err.println("CSS> "+l);
                        } else {
                            break;
                        }
                    } catch (IOException ex) {
                        // The stream is closed under the reader when the process
                        // is destroyed; either way there is nothing more to read.
                        if (!closing) {
                            Logger.getLogger(CSSWatcher.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    }
                }
            }
        });
        errorPump.setDaemon(true);
        errorPump.start();
        return errorPump;
    }

    /**
     * Reads the compiler's output until it ends, reloading the theme on every
     * "::refresh::" the compiler prints after a successful recompile.
     */
    private void pumpOutput(Process p, final CompilerArgs compilerArgs) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        final File fDestFile = new File(compilerArgs.output);
        while (true) {
            if (simulatorReloaded()) {
                stop();
                return;
            }
            String l;
            try {
                l = reader.readLine();
            } catch (IOException ex) {
                if (!closing) {
                    ex.printStackTrace();
                }
                // Nothing more can be read from this process, so a live one is
                // of no further use; ending it hands the decision to restart
                // back to watch(), where it is bounded.
                p.destroyForcibly();
                return;
            }
            if (l == null) {
                return;
            }
            System.out.println("CSS> "+l);
            if ("::refresh::".equals(l)) {
                try {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            if (!shouldRefresh()) {
                                return;
                            }
                            try {
                                System.out.println("CSS File "+compilerArgs.input+" has been updated.  Reloading styles from "+fDestFile);
                                Resources res = Resources.open(new FileInputStream(fDestFile));
                                UIManager.getInstance().addThemeProps(res.getTheme(res.getThemeResourceNames()[0]));
                                Form f = CN.getCurrentForm();
                                if (f != null) {
                                    f.refreshTheme();
                                    f.revalidate();
                                }
                            } catch(Exception err) {
                                err.printStackTrace();
                            }
                        }
                    });
                } catch (Throwable t) {
                    if (closing) {
                        return;
                    }
                    t.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            watch();
        } catch (Throwable t) {
            System.err.println("CSS watching failed");
            t.printStackTrace();
            watchThread = null;
        }
    }
    
    public synchronized void start() {
        if (!closing && watchThread == null) {
            watchThread = new Thread(this);
            watchThread.setDaemon(true);
            watchThread.start(); 
        }
    }

    public static Iterable<String> scanForThemePrefixes() {
        File resourcesDir = getCSSSourceDirectory();
        List<String> prefixes = new ArrayList<>();
        int themeCssLen = "theme.css".length();
        if (resourcesDir != null && resourcesDir.isDirectory()) {
            for (File resourceFile : resourcesDir.listFiles()) {
                String fileName = resourceFile.getName();
                if (fileName.endsWith("theme.css")) {
                    prefixes.add(fileName.substring(0, fileName.length() - themeCssLen));
                }
            }
        }
        return prefixes;
    }

    private static File getCSSSourceDirectory() {
        ProjectLayout layout = SimulatorProject.current();
        if (layout != null && layout.cssDir().isDirectory()) {
            return layout.cssDir();
        }
        File cssDir = new File(JavaSEPort.getCWD(), "src" + File.separator + "main" + File.separator + "css");
        if (cssDir.isDirectory()) return cssDir;
        return  new File(JavaSEPort.getCWD(), "css");
    }


    private static String prefixInputs(String themePrefix, String inputs) {
        StringBuilder all = new StringBuilder();
        StringBuilder present = new StringBuilder();
        for (String part : inputs.split(",")) {
            File prefixed = prefixFile(themePrefix, new File(part));
            if (all.length() > 0) {
                all.append(",");
            }
            all.append(prefixed.getPath());
            if (prefixed.exists()) {
                if (present.length() > 0) {
                    present.append(",");
                }
                present.append(prefixed.getPath());
            }
        }
        // The inputs are the ordinary theme of every library and of the
        // application, renamed. A library need not have a dark theme because
        // the application has one, and the compiler refuses an input that is
        // not there, so the ones that are missing are left out. With none
        // there at all the list is kept whole, for the compiler to say so.
        // A library with a prefixed theme and no ordinary one is not in the
        // list to begin with, and is only compiled by the build.
        return themePrefix.length() > 0 && present.length() > 0 ? present.toString() : all.toString();
    }

    private static File prefixFile(String themePrefix, File file) {
        return new File(file.getParentFile(), themePrefix + file.getName());
    }

    private boolean shouldRefresh() {
        Display displayInstance = Display.getInstance();
        Boolean isDarkMode = displayInstance.isDarkMode();
        boolean isDarkModeBool = isDarkMode != null && isDarkMode;
        String localThemePrefix = themePrefix;
        return !isDarkModeBool && "".equals(localThemePrefix) || isDarkModeBool && "dark-".equals(localThemePrefix);
    }
}
