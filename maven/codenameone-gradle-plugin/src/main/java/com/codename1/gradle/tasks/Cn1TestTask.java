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
import com.codename1.maven.Cn1TestRunner;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Runs the project's Codename One unit tests (the classes in `src/test/java`
/// implementing `com.codename1.testing.UnitTest`) in the JavaSE port's test
/// runner, as `mvn cn1:test` does. JUnit reports go to `build/cn1-reports`.
///
/// Not part of `check`: like the Maven goal, it runs the application's UI, and
/// a headless machine needs a virtual display (`xvfb-run ./gradlew cn1Test`).
@DisableCachingByDefault(because = "Runs the application's UI tests")
public abstract class Cn1TestTask extends Cn1Task {
    /// The compiled tests: every output directory of the test source set, so
    /// Kotlin tests (compiled apart from Java's) are found too. Already on the
    /// runtime classpath, which is the input.
    @Internal
    public abstract ConfigurableFileCollection getTestClassesDirectories();

    /// Everything the tests run against, the JavaSE port included.
    @Classpath
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    /// Where the JUnit XML reports go.
    @OutputDirectory
    public abstract DirectoryProperty getReportsDirectory();

    @TaskAction
    public void runTests() {
        // Reports are this run's or none: with the last test removed, the previous
        // run's TEST-*.xml would otherwise stay for a CI collector to publish as if
        // those tests had just passed.
        File reports = getReportsDirectory().get().getAsFile();
        try {
            if (reports.isDirectory()) {
                org.apache.commons.io.FileUtils.cleanDirectory(reports);
            }
        } catch (java.io.IOException ex) {
            throw new GradleException("Could not clear " + reports, ex);
        }
        List<File> testDirs = new ArrayList<File>(getTestClassesDirectories().getFiles());
        if (testDirs.isEmpty()) {
            getLogger().lifecycle("No tests were found.");
            return;
        }
        List<File> classpath = new ArrayList<File>();
        for (File f : getRuntimeClasspath()) {
            if (!testDirs.contains(f)) {
                classpath.add(f);
            }
        }
        Cn1TestRunner runner = new Cn1TestRunner(log());
        try {
            // tests.dat goes in the first (Java's) directory, which the runner's
            // classpath carries whether or not javac produced anything.
            if (!runner.prepare(testDirs, testDirs.get(0), classpath)) {
                getLogger().lifecycle("No tests were found.");
                return;
            }
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
        List<File> all = new ArrayList<File>();
        all.addAll(testDirs);
        all.addAll(classpath);
        String main = mainClass();
        if (main == null) {
            throw new GradleException("codenameone_settings.properties names no codename1.mainName");
        }
        int result = runner.run(layout().projectDir(), all, main, getReportsDirectory().get().getAsFile());
        if (result != 0) {
            throw new GradleException("Tests failed; see " + getReportsDirectory().get().getAsFile());
        }
    }
}
