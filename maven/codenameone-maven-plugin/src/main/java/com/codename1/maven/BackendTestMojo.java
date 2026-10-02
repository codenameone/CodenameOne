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

import com.codename1.maven.processors.BackendTests;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs a backend module's tests as a native binary: `mvn test
 * -Dcn1.backend.compiledTests=true`.
 *
 * The same test classes Surefire runs on the JVM, translated with the application
 * by ParparVM and linked the way {@link BackendPackageMojo} links a server -- so
 * what is tested is the program that ships, generated wiring, woven aspects,
 * natives and all. There is no JUnit and no reflection in that binary: the test
 * pass finds the tests in the bytecode and generates the calls JUnit would make
 * (see `BackendTestGenerator`), and the tests compile against a subset of JUnit 5's
 * API that codenameone-backend-test ships for the purpose.
 *
 * What a compiled run cannot do, it reports rather than fails on: a test class that
 * imports Mockito is left out of the compile (a mocking library builds classes at
 * run time), and a class using {@code @MockitoBean} is reported as skipped.
 * {@code -Dcn1.backend.compiledTests.strict=true} turns either into a failure.
 *
 * Off unless asked for, because it needs the native toolchain
 * {@code cn1:backend-package} needs, and takes as long as a package does. On
 * Windows, which has no native backend, it says so and does nothing; the JVM run is
 * the coverage there.
 *
 * The results land in target/surefire-reports as {@code TEST-<class>-compiled.xml},
 * beside Surefire's own, so a CI report shows both runs.
 */
@Mojo(name = "backend-test", defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class BackendTestMojo extends AbstractBackendNativeMojo {
    @Parameter(property = "cn1.backend.compiledTests", defaultValue = "false")
    private boolean enabled;

    @Parameter(property = "cn1.backend.compiledTests.strict", defaultValue = "false")
    private boolean strict;

    @Parameter(property = "cn1.backend.compiledTests.timeoutSeconds", defaultValue = "900")
    private int timeoutSeconds;

    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;

    @Parameter(property = "maven.test.skip", defaultValue = "false")
    private boolean skip;

    /** Test sources left out of the compile, and why. */
    private final Map<String, String> excluded = new LinkedHashMap<String, String>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!enabled || skip || skipTests) {
            return;
        }
        String os = System.getProperty("os.name", "");
        if (os.regionMatches(true, 0, "windows", 0, 7)) {
            getLog().warn("cn1: compiled backend tests need the native backend, which does "
                    + "not build on Windows; the JVM run is this platform's coverage");
            return;
        }
        if (testSources().isEmpty()) {
            getLog().info("cn1: no test sources, so no compiled backend tests");
            return;
        }
        super.execute();
    }

    @Override
    void prepareSources(File work, String runtimeVersion) throws MojoExecutionException {
        File testRuntime = new File(work, "test-runtime-src");
        emptyDirs(testRuntime);
        mkdirs(testRuntime);
        File jar = resolve("com.codenameone", "codenameone-backend-test", runtimeVersion,
                "parparvm-sources");
        unzip(jar, testRuntime, null);
        addSources(testRuntime);
    }

    @Override
    void afterGenerate(File jdk, File javaApi, File classes, File work)
            throws MojoExecutionException, MojoFailureException {
        File testClasses = new File(work, "test-classes");
        emptyDirs(testClasses);
        mkdirs(testClasses);
        List<String> sources = new ArrayList<String>();
        for (File f : testSources()) {
            String text;
            try {
                text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            } catch (IOException err) {
                throw new MojoExecutionException("Could not read " + f, err);
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
                throw new MojoFailureException("These tests cannot be compiled: " + list);
            }
            getLog().warn("cn1: left out of the compiled test run: " + list);
        }
        if (sources.isEmpty()) {
            throw new MojoFailureException("No test source can be compiled for a native run");
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
        run(command, project.getBasedir(), "compile the tests against the backend's class "
                + "library (a test that uses a JDK class the server runtime lacks fails here)");
        // The tests' resources, as Maven processed them.
        File processed = new File(project.getBuild().getTestOutputDirectory());
        if (processed.isDirectory()) {
            copyResources(processed, testClasses);
        }
        List<String> roots = new ArrayList<String>(project.getCompileSourceRoots());
        roots.addAll(project.getTestCompileSourceRoots());
        List<String> cp = new ArrayList<String>(classpath);
        cp.add(javaApi.getAbsolutePath());
        BackendTests.process(classes, testClasses, new File(work, "test-stubs"),
                project.getBasedir(), roots, sourceEncoding(), cp, true, getLog());
        copyDirectory(testClasses, classes);
        // One main per translation: the app's generated entry point goes, and the
        // test runner's takes its place. No test calls it; a test starts the
        // application through its context instead.
        File marker = new File(classes,
                com.codename1.maven.processors.RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE);
        if (marker.isFile()) {
            try {
                String entry = new String(Files.readAllBytes(marker.toPath()),
                        StandardCharsets.UTF_8).trim();
                File entryClass = new File(classes, entry.replace('.', '/') + ".class");
                if (entryClass.isFile() && !entryClass.delete()) {
                    throw new MojoExecutionException("Could not remove " + entryClass
                            + " from the test translation");
                }
            } catch (IOException err) {
                throw new MojoExecutionException("Could not read " + marker, err);
            }
        }
        mainClass = BackendTests.MAIN_CLASS;
    }

    @Override
    File binaryFile() {
        File dir = new File(project.getBuild().getDirectory(), "cn1-backend-test");
        mkdirs(dir);
        return new File(dir, project.getArtifactId() + "-tests");
    }

    @Override
    void afterLink(File binary) throws MojoExecutionException, MojoFailureException {
        File log = new File(binary.getParentFile(), "test-output.txt");
        int status;
        try {
            ProcessBuilder builder = new ProcessBuilder(binary.getAbsolutePath());
            builder.directory(project.getBasedir());
            builder.redirectErrorStream(true);
            builder.redirectOutput(log);
            Process process = builder.start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new MojoFailureException("The compiled tests did not finish in "
                        + timeoutSeconds + " seconds; their output is in " + log);
            }
            status = process.exitValue();
        } catch (IOException err) {
            throw new MojoExecutionException("Could not run " + binary, err);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while running the compiled tests", err);
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(log.toPath(), StandardCharsets.UTF_8);
        } catch (IOException err) {
            throw new MojoExecutionException("Could not read " + log, err);
        }
        CompiledTestReport report = CompiledTestReport.parse(lines);
        for (String line : lines) {
            if (!line.startsWith("CN1TEST\t")) {
                getLog().info("[compiled] " + line);
            }
        }
        File reports = new File(project.getBuild().getDirectory(), "surefire-reports");
        try {
            report.write(reports);
        } catch (IOException err) {
            throw new MojoExecutionException("Could not write the test reports: "
                    + err.getMessage(), err);
        }
        getLog().info("cn1: compiled tests: " + report.passed + " passed, " + report.failed
                + " failed, " + report.skipped + " skipped");
        if (strict && report.skipped > 0) {
            throw new MojoFailureException(report.skipped + " compiled test(s) were skipped and "
                    + "cn1.backend.compiledTests.strict is set:\n" + report.skips());
        }
        if (!report.done) {
            throw new MojoFailureException("The compiled tests stopped before reporting their "
                    + "totals (exit " + status + "): the binary crashed. Its output is in " + log);
        }
        if (report.failed > 0 || status != 0) {
            throw new MojoFailureException(report.failed + " compiled test(s) failed:\n"
                    + report.failures());
        }
    }

    private List<File> testSources() {
        List<File> out = new ArrayList<File>();
        for (Object root : project.getTestCompileSourceRoots()) {
            List<String> found = new ArrayList<String>();
            collectJava(new File(String.valueOf(root)), found);
            for (String f : found) {
                out.add(new File(f));
            }
        }
        return out;
    }

    private static void copyResources(File from, File to) throws MojoExecutionException {
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
                    Files.copy(child.toPath(), target.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException err) {
                    throw new MojoExecutionException("Could not copy " + child, err);
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
