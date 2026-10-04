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
package com.codename1.maven;

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.ProjectHost;
import com.codename1.maven.processors.BackendTests;
import com.codename1.maven.processors.RestControllerAnnotationProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/// Runs a backend module's tests as a native binary: the packaging
/// [BackendPackager] does, with the tests compiled into the same translation.
///
/// The same test classes the JVM runs, translated with the application by
/// ParparVM and linked the way the server is -- so what is tested is the program
/// that ships, generated wiring, woven aspects, natives and all. There is no JUnit
/// and no reflection in that binary: the test pass finds the tests in the bytecode
/// and generates the calls JUnit would make (see `BackendTestGenerator`), and the
/// tests compile against a subset of JUnit 5's API that codenameone-backend-test
/// ships for the purpose.
///
/// What a compiled run cannot do, it reports rather than fails on: a test class
/// that imports Mockito is left out of the compile (a mocking library builds
/// classes at run time), and a class using `@MockitoBean` is reported as skipped.
/// [#strict] turns either into a failure.
///
/// The results land in the build directory's surefire-reports as
/// `TEST-<class>-compiled.xml`, beside the JVM run's, so a CI report shows both.
public abstract class BackendTestPackager extends BackendPackager {
    /// Fail on a test left out of the run or skipped in it.
    protected boolean strict;
    /// How long the test binary may run.
    protected int timeoutSeconds = 900;

    /// Test sources left out of the compile, and why.
    private final Map<String, String> excluded = new LinkedHashMap<String, String>();

    /// A packager for the backend `host` describes.
    protected BackendTestPackager(ProjectHost host) {
        super(host);
    }

    /// The module's test source roots.
    protected abstract List<String> testSourceRoots();

    /// The test resources as the build tool processed them, or null.
    protected abstract File testOutputDirectory();

    /// Whether the JVM run discovers the test class `binaryName`, so the compiled
    /// run runs it too. Every class by default, as Gradle's JUnit Platform scan
    /// finds them; the Maven goal answers with Surefire's includes and excludes.
    protected boolean selectsTestClass(String binaryName) {
        return true;
    }

    /// The test classpath the JVM run resolved -- test-scoped dependencies
    /// included -- or empty when the build tool does not say. Its entries beyond
    /// the compile classpath are compiled against and translated with the tests.
    protected List<String> testClasspathElements() throws BuildExecutionException {
        return new ArrayList<String>();
    }

    /// Test libraries that run only on the JVM: the compiled run brings its own
    /// JUnit (the shim) and test support as sources, and a mocking library builds
    /// classes while it runs. Recognised by their Maven repository directories,
    /// and by the group directories of Gradle's dependency cache.
    private static final String[] JVM_ONLY_TEST_LIBRARIES = {
        "/org/junit/", "/org/opentest4j/", "/org/apiguardian/", "/org/mockito/",
        "/net/bytebuddy/", "/org/objenesis/", "/codenameone-backend-test/",
        "/org.junit.", "/org.junit/", "/org.opentest4j/", "/org.apiguardian/", "/org.mockito/",
        "/net.bytebuddy/", "/org.objenesis/",
    };

    /// The compile classpath plus the test-scoped dependencies a test imports: a
    /// helper library the JVM run compiles and runs the tests with, and which the
    /// compile classpath alone left out, so javac failed on a missing package.
    /// The JVM-only test libraries above stay out.
    @Override
    protected List<String> compileClasspathWithoutRuntime() throws BuildExecutionException {
        List<String> classpath = super.compileClasspathWithoutRuntime();
        String testOutput = testOutputDirectory() == null ? null : testOutputDirectory().getAbsolutePath();
        String mainOutput = host.outputDirectory().getAbsolutePath();
        for (String element : testClasspathElements()) {
            String absolute = new File(element).getAbsolutePath();
            if (classpath.contains(element) || absolute.equals(testOutput) || absolute.equals(mainOutput)
                    || jvmOnlyTestLibrary(absolute)) {
                continue;
            }
            classpath.add(element);
        }
        // The runtime goes as the parent drops it: by file, wherever it came from --
        // the test classpath carries it too, and its JVM build has a main of its own.
        return withoutRuntime(classpath, runtimeArtifactFile(), host.outputDirectory().getPath());
    }

    static boolean jvmOnlyTestLibrary(String path) {
        String p = path.replace('\\', '/');
        for (String marker : JVM_ONLY_TEST_LIBRARIES) {
            if (p.indexOf(marker) >= 0) {
                return true;
            }
        }
        return false;
    }

    /// Whether the JVM run runs test `method` of the selected class `binaryName`:
    /// all of them by default; the Maven goal narrows them by `-Dtest=Class#method`.
    protected boolean selectsTestMethod(String binaryName, String method) {
        return true;
    }

    /// The selection the test pass applies: a class name, or `Class#method` for one
    /// of its tests.
    private boolean selects(String name) {
        int hash = name.indexOf('#');
        return hash < 0 ? selectsTestClass(name)
                : selectsTestMethod(name.substring(0, hash), name.substring(hash + 1));
    }

    /// Sets [#strict].
    public BackendTestPackager strict(boolean value) {
        this.strict = value;
        return this;
    }

    /// Sets [#timeoutSeconds].
    public BackendTestPackager timeoutSeconds(int value) {
        this.timeoutSeconds = value;
        return this;
    }

    /// Whether the module has any test source to compile.
    public boolean hasTests() {
        // Kotlin sources count: a suite of only Kotlin tests is one the compiled run
        // cannot compile, and must say so -- reported as "no test sources" it was
        // silently left out, strict mode or not.
        return !testSources().isEmpty() || !kotlinTestSources().isEmpty();
    }

    @Override
    protected void prepareSources(File work, String runtimeVersion) throws BuildExecutionException {
        File testRuntime = new File(work, "test-runtime-src");
        emptyDirs(testRuntime);
        mkdirs(testRuntime);
        File jar = resolve("com.codenameone", "codenameone-backend-test", runtimeVersion,
                "parparvm-sources");
        unzip(jar, testRuntime, null);
        addSources(testRuntime);
    }

    private static final java.util.regex.Pattern MOCKITO =
            java.util.regex.Pattern.compile("\\borg\\s*\\.\\s*mockito\\b");

    /// Whether Java `source` uses Mockito in its code -- an import or a qualified
    /// name. Comments and string or character literals are blanked out first: the
    /// words in a comment, a Javadoc or a fixture string left an otherwise
    /// translatable test file out of the compiled run.
    static boolean usesMockito(String source) {
        return MOCKITO.matcher(codeOnly(source)).find();
    }

    @Override
    protected void afterGenerate(File jdk, File javaApi, File classes, File work)
            throws BuildExecutionException {
        File testClasses = new File(work, "test-classes");
        emptyDirs(testClasses);
        mkdirs(testClasses);
        // The tests are compiled here with javac, so Kotlin ones cannot be: refused
        // by name rather than left out, which ran a Kotlin project's compiled tests
        // as "no test source" and a mixed one's without its Kotlin tests at all.
        List<String> kotlin = kotlinTestSources();
        if (!kotlin.isEmpty()) {
            throw new BuildFailureException("The compiled backend test run compiles Java test "
                    + "sources, and these are Kotlin: " + kotlin + ". Run them on the JVM, or "
                    + "write the tests to run compiled in Java.");
        }
        List<String> sources = compilableSources();
        if (!excluded.isEmpty()) {
            String list = excluded.toString();
            if (strict) {
                throw new BuildFailureException("These tests cannot be compiled: " + list);
            }
            getLog().warn("cn1: left out of the compiled test run: " + list);
        }
        if (sources.isEmpty()) {
            throw new BuildFailureException("No test source can be compiled for a native run");
        }
        List<String> classpath = new ArrayList<String>();
        classpath.add(classes.getAbsolutePath());
        classpath.addAll(compileClasspathWithoutRuntime());
        List<String> command = new ArrayList<String>(Arrays.asList(
                new File(jdk, "bin/javac").getAbsolutePath(),
                "-nowarn", "-encoding", sourceEncoding(),
                "-bootclasspath", javaApi.getAbsolutePath(),
                "-source", "1.8", "-target", "1.8",
                "-classpath", join(classpath, File.pathSeparator),
                // The selected tests are named; whatever else of the test sources
                // they use -- a helper, a base class, a configuration -- javac
                // finds here and compiles too. A test the run did not select is
                // not compiled at all, so one using an API the server runtime
                // lacks cannot fail a run that never executes it.
                "-sourcepath", join(testSourceRoots(), File.pathSeparator),
                "-implicit:class",
                "-d", testClasses.getAbsolutePath()));
        command.addAll(sources);
        run(command, host.baseDir(), "compile the tests against the backend's class "
                + "library (a test that uses a JDK class the server runtime lacks fails here)");
        File processed = testOutputDirectory();
        if (processed != null && processed.isDirectory()) {
            copyResources(processed, testClasses);
        }
        List<String> roots = new ArrayList<String>(host.compileSourceRoots());
        roots.addAll(testSourceRoots());
        List<String> cp = new ArrayList<String>(classpath);
        cp.add(javaApi.getAbsolutePath());
        BackendTests.process(classes, testClasses, new File(work, "test-stubs"),
                host.baseDir(), roots, sourceEncoding(), cp, true, this::selects, getLog());
        copyDirectory(testClasses, classes);
        // One main per translation: the app's generated entry point goes, and the
        // test runner's takes its place. No test calls it; a test starts the
        // application through its context instead.
        File marker = new File(classes, RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE);
        if (marker.isFile()) {
            try {
                String entry = new String(Files.readAllBytes(marker.toPath()),
                        StandardCharsets.UTF_8).trim();
                File entryClass = new File(classes, entry.replace('.', '/') + ".class");
                if (entryClass.isFile() && !entryClass.delete()) {
                    throw new BuildExecutionException("Could not remove " + entryClass
                            + " from the test translation");
                }
            } catch (IOException err) {
                throw new BuildExecutionException("Could not read " + marker, err);
            }
        }
        mainClass = BackendTests.MAIN_CLASS;
    }

    @Override
    protected File binaryFile() {
        File dir = new File(host.buildDirectory(), "cn1-backend-test");
        mkdirs(dir);
        return new File(dir, binaryName() + "-tests");
    }

    @Override
    protected void afterLink(File binary) throws BuildExecutionException {
        File log = new File(binary.getParentFile(), "test-output.txt");
        int status;
        try {
            ProcessBuilder builder = new ProcessBuilder(binary.getAbsolutePath());
            builder.directory(host.baseDir());
            builder.redirectErrorStream(true);
            builder.redirectOutput(log);
            Process process = builder.start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new BuildFailureException("The compiled tests did not finish in "
                        + timeoutSeconds + " seconds; their output is in " + log);
            }
            status = process.exitValue();
        } catch (IOException err) {
            throw new BuildExecutionException("Could not run " + binary, err);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new BuildExecutionException("Interrupted while running the compiled tests", err);
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
        } catch (IOException err) {
            throw new BuildExecutionException("Could not read " + log, err);
        }
        CompiledTestReport report = CompiledTestReport.parse(lines);
        for (String line : lines) {
            if (!line.startsWith("CN1TEST\t")) {
                getLog().info("[compiled] " + line);
            }
        }
        File reports = new File(host.buildDirectory(), "surefire-reports");
        try {
            report.write(reports);
        } catch (IOException err) {
            throw new BuildExecutionException("Could not write the test reports: "
                    + err.getMessage(), err);
        }
        getLog().info("cn1: compiled tests: " + report.passed + " passed, " + report.failed
                + " failed, " + report.skipped + " skipped");
        if (strict && report.skipped > 0) {
            throw new BuildFailureException(report.skipped + " compiled test(s) were skipped and "
                    + "strict mode is on:\n" + report.skips());
        }
        if (!report.done) {
            throw new BuildFailureException("The compiled tests stopped before reporting their "
                    + "totals (exit " + status + "): the binary crashed. Its output is in " + log);
        }
        if (report.failed > 0 || status != 0) {
            throw new BuildFailureException(report.failed + " compiled test(s) failed:\n"
                    + report.failures());
        }
    }

    private List<String> kotlinTestSources() {
        List<String> out = new ArrayList<String>();
        for (String root : testSourceRoots()) {
            collectKotlin(new File(root), out);
        }
        return out;
    }

    private static void collectKotlin(File dir, List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectKotlin(child, out);
            } else if (child.getName().endsWith(".kt")) {
                out.add(child.getName());
            }
        }
    }

    /// Runs the compiled tests -- or, when nothing selected can be compiled and
    /// strict is off, says so and returns null. Leaving out a Mockito test is a
    /// warning in that mode, so a module whose tests all use Mockito, or a
    /// selection that names none of the module's tests, is a run with nothing in
    /// it rather than a failed build.
    @Override
    public File execute() throws BuildExecutionException {
        clearCompiledReports();
        if (!hasTests()) {
            // Here, for every build tool: the Maven goal checked before calling,
            // Gradle's backendTest did not, and a module with no tests failed with
            // "No test source can be compiled" instead of having nothing to run.
            getLog().info("cn1: no test sources, so no compiled backend tests");
            return null;
        }
        if (!strict && kotlinTestSources().isEmpty() && !testSources().isEmpty()
                && compilableSources().isEmpty()) {
            getLog().warn("cn1: no compiled backend tests to run"
                    + (excluded.isEmpty() ? ": the selection names none of this module's tests"
                    : "; left out: " + excluded));
            excluded.clear();
            return null;
        }
        excluded.clear();
        return super.execute();
    }

    /// Deletes the compiled-run reports an earlier run left in surefire-reports:
    /// a targeted run (`-Dtest=ApiTest`) writes reports only for what it ran, and
    /// the rest from a previous full run read as if they had run this time.
    private void clearCompiledReports() {
        File[] old = new File(host.buildDirectory(), "surefire-reports").listFiles();
        if (old == null) {
            return;
        }
        for (File f : old) {
            String name = f.getName();
            if (name.startsWith("TEST-") && name.endsWith("-compiled.xml") && !f.delete()) {
                getLog().warn("cn1: could not remove the stale report " + f);
            }
        }
    }

    /// The test sources the compiled run names to javac: the ones whose class the
    /// JVM run selects, less those that use Mockito, which go into [#excluded].
    private List<String> compilableSources() throws BuildExecutionException {
        Map<String, List<String>> declared = declaredClasses(testOutputDirectory());
        // Every test source, selected or not: javac compiles an unselected support
        // source a selected test uses, through -sourcepath.
        Map<File, String> texts = new LinkedHashMap<File, String>();
        Map<File, File> roots = new LinkedHashMap<File, File>();
        for (String root : testSourceRoots()) {
            File base = new File(root);
            List<String> found = new ArrayList<String>();
            collectJava(base, found);
            for (String path : found) {
                File f = new File(path);
                try {
                    texts.put(f, new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
                } catch (IOException err) {
                    throw new BuildExecutionException("Could not read " + f, err);
                }
                roots.put(f, base);
            }
        }
        Map<File, String> mockito = mockitoReach(texts);
        List<String> sources = new ArrayList<String>();
        for (Map.Entry<File, String> e : texts.entrySet()) {
            File f = e.getKey();
            File base = roots.get(f);
            if (!selects(declared.get(sourcePath(base, f)), binaryName(base, f))) {
                continue;
            }
            String why = mockito.get(f);
            if (why != null) {
                excluded.put(f.getName(), why);
                continue;
            }
            sources.add(f.getAbsolutePath());
        }
        return sources;
    }

    /// The test sources that cannot be compiled without Mockito, with why: those
    /// whose code uses it, and -- to a fixed point -- those that name one of them.
    /// A test with no Mockito of its own that extends a MockitoTestBase still
    /// pulls the base in through -sourcepath, and the Mockito jars are not on the
    /// compiled run's classpath, so the compile failed instead of the test being
    /// left out. Matched by simple class name in the code, comments and string
    /// literals stripped; a same-named class in another package still matches,
    /// which is not resolved because a false match only leaves a test out, and
    /// the run's warning names it.
    static Map<File, String> mockitoReach(Map<File, String> texts) {
        Map<File, String> out = new LinkedHashMap<File, String>();
        Map<File, String> names = new LinkedHashMap<File, String>();
        Map<File, String> code = new LinkedHashMap<File, String>();
        for (Map.Entry<File, String> e : texts.entrySet()) {
            code.put(e.getKey(), codeOnly(e.getValue()));
            String file = e.getKey().getName();
            names.put(e.getKey(), file.endsWith(".java") ? file.substring(0, file.length() - 5) : file);
            if (usesMockito(e.getValue())) {
                out.put(e.getKey(), "imports Mockito, which runs only on the JVM");
            }
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Map.Entry<File, String> e : texts.entrySet()) {
                if (out.containsKey(e.getKey())) {
                    continue;
                }
                for (File tainted : new ArrayList<File>(out.keySet())) {
                    String name = names.get(tainted);
                    if (java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(name) + "\\b")
                            .matcher(code.get(e.getKey())).find()) {
                        out.put(e.getKey(), "uses " + name + ", which needs Mockito, which runs only on the JVM");
                        grew = true;
                        break;
                    }
                }
            }
        }
        return out;
    }

    /// `source` with its comments and string, character and text-block literals
    /// blanked, so a name mentioned in prose or a message is not a use.
    static String codeOnly(String source) {
        StringBuilder code = new StringBuilder(source.length());
        int n = source.length();
        int i = 0;
        while (i < n) {
            char c = source.charAt(i);
            char next = i + 1 < n ? source.charAt(i + 1) : 0;
            if (c == '/' && next == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && next == '*') {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? n : end + 2;
                code.append(' ');
            } else if (c == '"' && source.startsWith("\"\"\"", i)) {
                int end = source.indexOf("\"\"\"", i + 3);
                i = end < 0 ? n : end + 3;
                code.append(' ');
            } else if (c == '"' || c == '\'') {
                i++;
                while (i < n && source.charAt(i) != c && source.charAt(i) != '\n') {
                    i += source.charAt(i) == '\\' ? 2 : 1;
                }
                i++;
                code.append(' ');
            } else {
                code.append(c);
                i++;
            }
        }
        return code.toString();
    }

    /// Whether the JVM run selects a source: by the top-level classes compiled
    /// from it, which is what Surefire matches -- `Fixtures.java` may declare a
    /// package-private `class ApiTest` -- or by its file name when those are not
    /// known.
    private boolean selects(List<String> classes, String byFileName) {
        if (classes == null || classes.isEmpty()) {
            return selectsTestClass(byFileName);
        }
        for (String name : classes) {
            if (selectsTestClass(name)) {
                return true;
            }
        }
        return false;
    }

    /// The top-level classes compiled from each test source, keyed by the
    /// source's path under its root (`com/acme/Fixtures.java`), read from the
    /// JVM build's test classes; empty when there are none to read.
    static Map<String, List<String>> declaredClasses(File testClasses) {
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        if (testClasses == null || !testClasses.isDirectory()) {
            return out;
        }
        List<File> files = new ArrayList<File>();
        collectClasses(testClasses, files);
        for (File f : files) {
            final String[] found = new String[2];
            try {
                new org.objectweb.asm.ClassReader(Files.readAllBytes(f.toPath())).accept(
                        new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                            @Override
                            public void visit(int version, int access, String name, String signature,
                                              String superName, String[] interfaces) {
                                found[0] = name;
                            }

                            @Override
                            public void visitSource(String source, String debug) {
                                found[1] = source;
                            }
                        }, org.objectweb.asm.ClassReader.SKIP_CODE);
            } catch (IOException | RuntimeException unreadable) {
                continue;
            }
            if (found[0] == null || found[1] == null || found[0].indexOf('$') >= 0) {
                continue;
            }
            int slash = found[0].lastIndexOf('/');
            String key = (slash < 0 ? "" : found[0].substring(0, slash + 1)) + found[1];
            List<String> classes = out.get(key);
            if (classes == null) {
                classes = new ArrayList<String>();
                out.put(key, classes);
            }
            classes.add(found[0].replace('/', '.'));
        }
        return out;
    }

    private static void collectClasses(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectClasses(child, out);
            } else if (child.getName().endsWith(".class")) {
                out.add(child);
            }
        }
    }

    /// A source file's path under `root`, with `/` separators.
    static String sourcePath(File root, File source) {
        return root.toURI().relativize(source.toURI()).getPath();
    }

    /// The binary name of the class a source file under `root` declares.
    static String binaryName(File root, File source) {
        String relative = root.toURI().relativize(source.toURI()).getPath();
        if (relative.endsWith(".java")) {
            relative = relative.substring(0, relative.length() - ".java".length());
        }
        return relative.replace('/', '.');
    }

    private List<File> testSources() {
        List<File> out = new ArrayList<File>();
        for (String root : testSourceRoots()) {
            List<String> found = new ArrayList<String>();
            collectJava(new File(root), found);
            for (String f : found) {
                out.add(new File(f));
            }
        }
        return out;
    }

    private static void copyResources(File from, File to) throws BuildExecutionException {
        File[] children = from.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            File target = new File(to, child.getName());
            if (child.isDirectory()) {
                copyResources(child, target);
            } else if (!child.getName().endsWith(".class")) {
                try {
                    Files.createDirectories(to.toPath());
                    Files.copy(child.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException err) {
                    throw new BuildExecutionException("Could not copy " + child, err);
                }
            }
        }
    }

    private static String join(List<String> parts, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(p);
        }
        return sb.toString();
    }
}
