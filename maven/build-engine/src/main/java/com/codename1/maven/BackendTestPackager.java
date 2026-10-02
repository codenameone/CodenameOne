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
        return !testSources().isEmpty();
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

    @Override
    protected void afterGenerate(File jdk, File javaApi, File classes, File work)
            throws BuildExecutionException {
        File testClasses = new File(work, "test-classes");
        emptyDirs(testClasses);
        mkdirs(testClasses);
        List<String> sources = new ArrayList<String>();
        for (File f : testSources()) {
            String text;
            try {
                text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            } catch (IOException err) {
                throw new BuildExecutionException("Could not read " + f, err);
            }
            if (text.indexOf("org.mockito") >= 0) {
                excluded.put(f.getName(), "imports Mockito, which runs only on the JVM");
                continue;
            }
            sources.add(f.getAbsolutePath());
        }
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
                host.baseDir(), roots, sourceEncoding(), cp, true, getLog());
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
