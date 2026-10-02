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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

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
            throws MojoExecutionException, MojoFailureException {
        Map<String, AnnotatedClass> main;
        Map<String, AnnotatedClass> tests;
        try {
            main = mainClasses.isDirectory() ? ClassScanner.scan(mainClasses)
                    : new LinkedHashMap<String, AnnotatedClass>();
            tests = ClassScanner.scan(testClasses);
        } catch (ProcessingException err) {
            throw new MojoExecutionException("Could not scan the compiled classes: "
                    + err.getMessage(), err);
        }
        if (!compiled && !mentions(tests, BackendTestGenerator.BACKEND_TEST)) {
            return 0;
        }
        Map<String, AnnotatedClass> index = new LinkedHashMap<String, AnnotatedClass>(main);
        index.putAll(tests);
        Set<String> mainNames = new LinkedHashSet<String>(main.keySet());
        mainNames.removeAll(tests.keySet());
        ProcessorContext ctx = new ProcessorContext(testClasses, stubs, index, log, projectDir,
                new Properties(), null, sourceRoots, encoding, classpath);
        int count;
        try {
            count = BackendTestGenerator.generate(ctx, mainNames, tests.keySet(), mainClasses,
                    compiled);
        } catch (ProcessingException err) {
            throw new MojoExecutionException(err.getMessage(), err);
        }
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("The backend tests could not be prepared:");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
                sb.append("\n  - ").append(e);
            }
            throw new MojoFailureException(sb.toString());
        }
        return count;
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
