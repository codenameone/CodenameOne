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
package com.codename1.gradle.tasks;

import com.codename1.build.BuildExecutionException;
import com.codename1.gradle.GradleLog;
import com.codename1.maven.AnnotationProcessing;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/// Runs the Codename One annotation processors over `compileJava`'s output, as
/// the last step of that task.
///
/// A `doLast` rather than a task of its own because the processors rewrite the
/// compiled classes in place: a separate task writing into `compileJava`'s
/// output directory would be an overlapping output, which defeats up-to-date
/// checks and the build cache for both.
public final class ProcessAnnotationsAction implements Action<Task> {
    private final File classesDir;
    private final File stubDir;
    private final File projectDir;
    private final File settingsFile;
    private final List<String> sourceRoots;
    private final String encoding;
    private final Map<String, String> userProperties;
    private final FileCollection compileClasspath;
    private final List<File> pendingJavaSources = new ArrayList<File>();

    /// @param classesDir `compileJava`'s destination
    /// @param stubDir where processors write generated stub sources
    /// @param userProperties the command-line `codename1.*` overrides
    public ProcessAnnotationsAction(File classesDir, File stubDir, File projectDir, File settingsFile,
                                    List<String> sourceRoots, String encoding, Map<String, String> userProperties,
                                    FileCollection compileClasspath) {
        this.classesDir = classesDir;
        this.stubDir = stubDir;
        this.projectDir = projectDir;
        this.settingsFile = settingsFile;
        this.sourceRoots = new ArrayList<String>(sourceRoots);
        this.encoding = encoding;
        this.userProperties = new java.util.HashMap<String, String>(userProperties);
        this.compileClasspath = compileClasspath;
    }

    /// For the Kotlin pass, which runs before javac: the Java source directories
    /// of the same source set. Kotlin resolves the project's Java types from these
    /// sources, so a Kotlin `@RestClient` returning a Java DTO compiles -- and the
    /// processors, which resolve types through the classpath, must see them too.
    /// They are compiled into a scratch directory put on the processing
    /// classpath only; javac's real pass still produces the shipped classes.
    public ProcessAnnotationsAction withPendingJavaSources(java.util.Collection<File> javaSourceDirs) {
        this.pendingJavaSources.addAll(javaSourceDirs);
        return this;
    }

    /// Compiles [#pendingJavaSources] into `out` against `classpath`, or answers
    /// null when there is nothing to compile or javac refuses. A refusal is left
    /// to compileJava to report with the real diagnostics; this pass then runs
    /// as before, without the Java types.
    private File compilePendingJava(Task task, List<String> classpath) {
        List<File> sources = new ArrayList<File>();
        for (File dir : pendingJavaSources) {
            collectJava(dir, sources);
        }
        if (sources.isEmpty()) {
            return null;
        }
        javax.tools.JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            return null;
        }
        File out = new File(task.getTemporaryDir(), "pending-java");
        try {
            if (out.exists()) {
                org.apache.commons.io.FileUtils.deleteDirectory(out);
            }
        } catch (IOException ex) {
            throw new GradleException("Could not delete " + out, ex);
        }
        if (!out.mkdirs()) {
            throw new GradleException("Could not create " + out);
        }
        StringBuilder cp = new StringBuilder();
        for (String element : classpath) {
            if (cp.length() > 0) {
                cp.append(File.pathSeparatorChar);
            }
            cp.append(element);
        }
        List<String> args = new ArrayList<String>();
        java.util.Collections.addAll(args, "-proc:none", "-nowarn", "-implicit:class", "-encoding", encoding,
                "-cp", cp.toString(), "-d", out.getAbsolutePath());
        for (File f : sources) {
            args.add(f.getAbsolutePath());
        }
        java.io.ByteArrayOutputStream diagnostics = new java.io.ByteArrayOutputStream();
        int result = javac.run(null, diagnostics, diagnostics, args.toArray(new String[0]));
        if (result != 0) {
            task.getLogger().info("cn1: the Java sources did not compile ahead of javac, so Kotlin annotations "
                    + "are processed without the project's Java types:\n" + diagnostics);
            return null;
        }
        return out;
    }

    private static void collectJava(File dir, List<File> out) {
        File[] children = dir == null ? null : dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c.isDirectory()) {
                collectJava(c, out);
            } else if (c.getName().endsWith(".java")) {
                out.add(c);
            }
        }
    }

    @Override
    public void execute(Task task) {
        Properties raw = null;
        if (settingsFile.isFile()) {
            raw = new Properties();
            try (InputStream in = new FileInputStream(settingsFile)) {
                raw.load(in);
            } catch (IOException ex) {
                task.getLogger().warn("cn1: could not read " + settingsFile + ": " + ex.getMessage());
                raw = null;
            }
        }
        // The main class from the EFFECTIVE settings (a -P override wins), the raw
        // file for the duplicate-declaration check: see AnnotationProcessing.
        Properties effective = new Properties();
        if (raw != null) {
            effective.putAll(raw);
        }
        for (Map.Entry<String, String> e : userProperties.entrySet()) {
            effective.setProperty(e.getKey(), e.getValue());
        }
        String main = effective.getProperty("codename1.mainName");
        String pkg = effective.getProperty("codename1.packageName");
        String mainClass = main == null || main.trim().isEmpty() ? null
                : (pkg == null || pkg.trim().isEmpty() ? main.trim() : pkg.trim() + "." + main.trim());
        List<String> classpath = new ArrayList<String>();
        classpath.add(classesDir.getAbsolutePath());
        for (File f : compileClasspath) {
            classpath.add(f.getAbsolutePath());
        }
        File pending = compilePendingJava(task, classpath);
        if (pending != null) {
            classpath.add(pending.getAbsolutePath());
        }
        try {
            new AnnotationProcessing(new GradleLog(task.getLogger()), classesDir, stubDir, projectDir, raw,
                    mainClass, sourceRoots, encoding, classpath).run();
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }
}
