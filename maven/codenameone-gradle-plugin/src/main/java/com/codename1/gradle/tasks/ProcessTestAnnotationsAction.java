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
import com.codename1.maven.processors.BackendTests;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Runs after a backend's test classes compile: makes its `@BackendTest` classes
/// runnable, as the Maven plugin's `process-test-annotations` goal does.
///
/// For each `@BackendTest` class the build engine generates the context and the
/// test wiring the `com.codename1.backend.test` runtime starts, and rewrites the
/// test classes so their injected fields are set without reflection. A project with
/// no `@BackendTest` is left untouched.
public final class ProcessTestAnnotationsAction implements Action<Task> {
    private final FileCollection mainClassDirs;
    private final Provider<File> testClasses;
    private final File stubDir;
    private final File projectDir;
    private final Provider<List<String>> sourceRoots;
    private final Provider<String> encoding;
    private final FileCollection testClasspath;

    public ProcessTestAnnotationsAction(FileCollection mainClassDirs, Provider<File> testClasses, File stubDir,
                                        File projectDir, Provider<List<String>> sourceRoots,
                                        Provider<String> encoding, FileCollection testClasspath) {
        this.mainClassDirs = mainClassDirs;
        this.testClasses = testClasses;
        this.stubDir = stubDir;
        this.projectDir = projectDir;
        this.sourceRoots = sourceRoots;
        this.encoding = encoding;
        this.testClasspath = testClasspath;
    }

    @Override
    public void execute(Task task) {
        File tests = testClasses.get();
        if (!tests.isDirectory()) {
            return;
        }
        List<String> classpath = new ArrayList<String>();
        for (File f : testClasspath) {
            classpath.add(f.getAbsolutePath());
        }
        try {
            int generated = BackendTests.process(new ArrayList<File>(mainClassDirs.getFiles()), tests, stubDir,
                    projectDir,
                    new ArrayList<String>(sourceRoots.get()), encoding.get(), classpath, false, null,
                    new GradleLog(task.getLogger()));
            if (generated > 0) {
                task.getLogger().lifecycle("cn1: prepared " + generated + " backend test class(es)");
            }
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }
}
