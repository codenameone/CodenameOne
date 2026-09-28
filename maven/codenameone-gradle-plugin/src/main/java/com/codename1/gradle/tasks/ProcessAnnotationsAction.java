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
        try {
            new AnnotationProcessing(new GradleLog(task.getLogger()), classesDir, stubDir, projectDir, raw,
                    mainClass, sourceRoots, encoding, classpath).run();
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }
}
