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

import com.codename1.build.BuildFailureException;
import com.codename1.gradle.GradleLog;
import com.codename1.maven.BackendMainClass;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;

/// Runs a backend on this JVM, as `mvn cn1:backend` does: the generated entry
/// point (or the one class with a `main`), the runtime classpath, the backend
/// directory as the working directory so `application.properties` is found.
///
/// It starts in seconds, against the minute and a half a native build takes.
/// Like the Maven goal it does not terminate TLS; `backendPackage` builds the
/// binary for that.
@DisableCachingByDefault(because = "Runs a server")
public abstract class RunBackendTask extends DefaultTask {
    @Inject
    protected abstract ExecOperations getExecOperations();

    /// The runtime classpath, compiled classes first.
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    /// The compiled classes, where the generated entry point is recorded.
    @Internal
    public abstract DirectoryProperty getClassesDirectory();

    /// The class to run, when not the generated or the only one.
    @Input
    @Optional
    public abstract Property<String> getMainClass();

    /// Program arguments.
    @Input
    public abstract ListProperty<String> getArgs();

    /// JVM options.
    @Input
    public abstract ListProperty<String> getJvmArgs();

    /// The backend's directory.
    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

    @TaskAction
    public void run() {
        final String main;
        try {
            main = new BackendMainClass(new GradleLog(getLogger()), "-Pcn1.backend.mainClass")
                    .resolve(getClassesDirectory().get().getAsFile(), getMainClass().getOrNull());
        } catch (BuildFailureException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
        getLogger().lifecycle("Running " + main + " on " + System.getProperty("java.version"));
        getExecOperations().javaexec(spec -> {
            spec.classpath(getClasspath());
            spec.getMainClass().set(main);
            spec.args(getArgs().get());
            spec.jvmArgs(getJvmArgs().get());
            spec.setWorkingDir(getWorkingDirectory().get().getAsFile());
            spec.setStandardInput(System.in);
        });
    }
}
