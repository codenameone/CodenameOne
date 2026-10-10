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
import com.codename1.build.BuildFailureException;
import com.codename1.gradle.GradleLog;
import com.codename1.maven.BackendMainClass;
import com.codename1.maven.BackendWebApp;
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

    /// The compiled classes, where the generated entry point is recorded: every
    /// classes directory of the main source set, Kotlin's included.
    @Internal
    public abstract ConfigurableFileCollection getClassesDirectories();

    /// The class to run, when not the generated or the only one.
    @Input
    @Optional
    public abstract Property<String> getMainClass();

    /// Set when the class to run is one the build generates only for some modules: what to
    /// say, in place of the JVM's "could not find or load main class", when it is in none of
    /// the classes directories.
    @Internal
    public abstract Property<String> getMissingMainClassMessage();

    /// Program arguments.
    @Input
    public abstract ListProperty<String> getArgs();

    /// JVM options.
    @Input
    public abstract ListProperty<String> getJvmArgs();

    /// The backend's directory.
    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

    /// Where `backendWebApp` stages the application's browser build. Unset for
    /// a task that runs something other than the server.
    @Internal
    public abstract DirectoryProperty getWebAppDirectory();

    /// The JVM option that points the server at the application staged in
    /// `webApp`, or null when it should be given none.
    ///
    /// The server looks for its application in `webapp` in its working
    /// directory, which is the backend's own directory -- and build output does
    /// not belong in a source directory, so it is under the build directory and
    /// the server is told. Not when the project says where its application is:
    /// a system property outranks the backend's own files, and answering for it
    /// would override a setting somebody wrote down.
    static String webAppOption(java.io.File webApp, java.io.File workingDirectory,
                               java.util.Map<String, String> environment, java.util.List<String> jvmArgs)
            throws BuildExecutionException {
        if (webApp == null || !new java.io.File(webApp, BackendWebApp.INDEX).isFile()) {
            return null;
        }
        StringBuilder options = new StringBuilder();
        for (String option : jvmArgs) {
            options.append(option).append(' ');
        }
        if (BackendWebApp.rootIsConfigured(workingDirectory, environment, options.toString())) {
            return null;
        }
        return "-D" + BackendWebApp.ROOT_PROPERTY + "=" + webApp.getAbsolutePath();
    }

    /// The explicit main class, else the generated entry point in whichever
    /// classes directory holds it, else the one class with a main method.
    private String resolveMainClass() throws BuildFailureException {
        BackendMainClass finder = new BackendMainClass(new GradleLog(getLogger()), "-Pcn1.backend.mainClass");
        String explicit = getMainClass().getOrNull();
        if (explicit != null && !explicit.isEmpty()) {
            String absent = getMissingMainClassMessage().getOrNull();
            if (absent != null && !generated(explicit)) {
                throw new BuildFailureException(absent + " (" + explicit + " was not generated)");
            }
            return explicit;
        }
        java.util.List<java.io.File> dirs = new java.util.ArrayList<java.io.File>();
        for (java.io.File dir : getClassesDirectories().getFiles()) {
            if (dir.isDirectory()) {
                dirs.add(dir);
            }
        }
        for (java.io.File dir : dirs) {
            String generated = finder.generatedMainClass(dir);
            if (generated != null && !generated.isEmpty()) {
                return generated;
            }
        }
        // Across every directory at once, so a main in each -- or several in one
        // -- is reported as ambiguous rather than one being picked.
        return finder.findMainClass(dirs);
    }

    private boolean generated(String className) {
        String file = className.replace('.', java.io.File.separatorChar) + ".class";
        for (java.io.File dir : getClassesDirectories().getFiles()) {
            if (new java.io.File(dir, file).isFile()) {
                return true;
            }
        }
        return false;
    }

    @TaskAction
    public void run() {
        final String main;
        try {
            main = resolveMainClass();
        } catch (BuildFailureException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
        final String webApp;
        try {
            webApp = webAppOption(getWebAppDirectory().isPresent() ? getWebAppDirectory().get().getAsFile() : null,
                    getWorkingDirectory().get().getAsFile(), System.getenv(), getJvmArgs().get());
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
        if (webApp != null) {
            getLogger().lifecycle("Serving the web app staged in " + getWebAppDirectory().get().getAsFile());
        }
        getLogger().lifecycle("Running " + main + " on " + System.getProperty("java.version"));
        getExecOperations().javaexec(spec -> {
            spec.classpath(getClasspath());
            spec.getMainClass().set(main);
            spec.args(getArgs().get());
            spec.jvmArgs(getJvmArgs().get());
            if (webApp != null) {
                spec.jvmArgs(webApp);
            }
            spec.setWorkingDir(getWorkingDirectory().get().getAsFile());
            spec.setStandardInput(System.in);
        });
    }
}
