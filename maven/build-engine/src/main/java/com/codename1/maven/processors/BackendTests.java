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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;

/// The entry to the backend test pass for the goals: the JVM one
/// (`process-test-annotations`) and the compiled one (`backend-test`), which run
/// it over different trees but the same way.
public final class BackendTests {
    /// The entry point a compiled test run is linked with.
    public static final String MAIN_CLASS = BackendTestGenerator.MAIN_CLASS;

    private BackendTests() {
    }

    /// Scans `mainClasses` and `testClasses` into one index and runs
    /// [BackendTestGenerator] over it, writing into `testClasses`.
    ///
    /// @return how many test classes were prepared
    public static int process(File mainClasses, File testClasses, File stubs, File projectDir,
                              List<String> sourceRoots, String encoding, List<String> classpath,
                              boolean compiled, Log log)
            throws BuildExecutionException, BuildFailureException {
        return process(mainClasses, testClasses, stubs, projectDir, sourceRoots, encoding, classpath,
                compiled, null, log);
    }

    /// As above, with a compiled run limited to the test classes, by binary name,
    /// that `selection` accepts -- the ones the build tool's JVM run discovers --
    /// and to the tests of each it accepts as `Class#method`.
    public static int process(File mainClasses, File testClasses, File stubs, File projectDir,
                              List<String> sourceRoots, String encoding, List<String> classpath,
                              boolean compiled, java.util.function.Predicate<String> selection, Log log)
            throws BuildExecutionException, BuildFailureException {
        return process(java.util.Collections.singletonList(mainClasses), testClasses, stubs, projectDir,
                sourceRoots, encoding, classpath, compiled, selection, log);
    }

    /// As above, over every directory the application's classes were compiled into
    /// -- a Gradle backend with Kotlin and Java sources has one per language, and
    /// its beans may be in either.
    public static int process(List<File> mainClassDirs, File testClasses, File stubs, File projectDir,
                              List<String> sourceRoots, String encoding, List<String> classpath,
                              boolean compiled, java.util.function.Predicate<String> selection, Log log)
            throws BuildExecutionException, BuildFailureException {
        return process(mainClassDirs, testClasses, java.util.Collections.<File>emptyList(), stubs,
                projectDir, sourceRoots, encoding, classpath, compiled, selection, log);
    }

    /// As above, with `otherTestClassDirs` -- the other compiler's output of a mixed
    /// Java and Kotlin test set -- read as part of the same tests: a Kotlin test
    /// whose `@BackendTest` base or `@TestConfiguration` is Java, or the reverse,
    /// is one hierarchy. What the pass generates goes into `testClasses`.
    public static int process(List<File> mainClassDirs, File testClasses, List<File> otherTestClassDirs,
                              File stubs, File projectDir,
                              List<String> sourceRoots, String encoding, List<String> classpath,
                              boolean compiled, java.util.function.Predicate<String> selection, Log log)
            throws BuildExecutionException, BuildFailureException {
        Map<String, AnnotatedClass> main = new LinkedHashMap<String, AnnotatedClass>();
        Map<String, AnnotatedClass> tests;
        // The main build's wiring record is in one of them; read from that one.
        File mainClasses = mainClassDirs.isEmpty() ? testClasses : mainClassDirs.get(0);
        try {
            for (File dir : mainClassDirs) {
                if (dir.isDirectory()) {
                    main.putAll(ClassScanner.scan(dir));
                    if (new File(dir, RestControllerAnnotationProcessor.WIRING_RESOURCE).isFile()) {
                        mainClasses = dir;
                    }
                }
            }
            tests = new LinkedHashMap<String, AnnotatedClass>();
            for (File dir : otherTestClassDirs) {
                if (dir != null && dir.isDirectory() && !dir.equals(testClasses)) {
                    tests.putAll(ClassScanner.scan(dir));
                }
            }
            tests.putAll(ClassScanner.scan(testClasses));
        } catch (ProcessingException err) {
            throw new BuildExecutionException("Could not scan the compiled classes: "
                    + err.getMessage(), err);
        }
        boolean standalone = referencesStandalone(testClasses);
        if (!compiled && !standalone && !mentionsAny(tests, BackendTestGenerator.TEST_ANNOTATIONS)) {
            return 0;
        }
        Map<String, AnnotatedClass> index = new LinkedHashMap<String, AnnotatedClass>(main);
        index.putAll(tests);
        Set<String> mainNames = new LinkedHashSet<String>(main.keySet());
        mainNames.removeAll(tests.keySet());
        ProcessorContext ctx = new ProcessorContext(testClasses, stubs, index, log, projectDir,
                new Properties(), null, sourceRoots, encoding, classpath);
        if (standalone) {
            ctx.setAttribute(BackendTestGenerator.STANDALONE_ATTRIBUTE, Boolean.TRUE);
        }
        int count;
        try {
            count = BackendTestGenerator.generate(ctx, mainNames, tests.keySet(), mainClasses,
                    compiled, selection);
        } catch (ProcessingException err) {
            throw new BuildExecutionException(err.getMessage(), err);
        }
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("The backend tests could not be prepared:");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
                sb.append("\n  - ").append(e);
            }
            throw new BuildFailureException(sb.toString());
        }
        return count;
    }

    /// Whether a compiled test names `MockMvcBuilders`, whose `standaloneSetup`
    /// needs the generated router registry: a byte search of the class files,
    /// since a class reference is a constant pool string.
    private static boolean referencesStandalone(File dir) {
        byte[] needle = "com/codename1/backend/test/MockMvcBuilders".getBytes(
                java.nio.charset.StandardCharsets.US_ASCII);
        java.util.ArrayDeque<File> pending = new java.util.ArrayDeque<File>();
        pending.add(dir);
        while (!pending.isEmpty()) {
            File[] children = pending.poll().listFiles();
            if (children == null) {
                continue;
            }
            for (File f : children) {
                if (f.isDirectory()) {
                    pending.add(f);
                } else if (f.getName().endsWith(".class")) {
                    try {
                        if (contains(java.nio.file.Files.readAllBytes(f.toPath()), needle)) {
                            return true;
                        }
                    } catch (java.io.IOException ignored) {
                        // Unreadable: the scan above would have failed on it already.
                    }
                }
            }
        }
        return false;
    }

    private static boolean contains(byte[] hay, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= hay.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean mentionsAny(Map<String, AnnotatedClass> classes, String[] descriptors) {
        for (String d : descriptors) {
            if (mentions(classes, d)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentions(Map<String, AnnotatedClass> classes, String descriptor) {
        for (AnnotatedClass cls : classes.values()) {
            if (cls.getAllAnnotationDescriptors().contains(descriptor)) {
                return true;
            }
        }
        return false;
    }
}
