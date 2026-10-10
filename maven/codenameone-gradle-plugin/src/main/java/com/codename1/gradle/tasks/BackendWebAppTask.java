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
import com.codename1.build.Log;
import com.codename1.gradle.GradleLog;
import com.codename1.maven.BackendWebApp;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;

/// `backendWebApp`: puts the browser build of the application where its backend
/// serves it from, as `mvn cn1:backend-webapp` does.
///
/// A backend that finds an application in its `webapp` directory answers with
/// it at `/`, so one address is both the API and the app. This task fills that
/// directory, `build/webapp`: it takes the bundle the application's local
/// JavaScript build left in its build directory -- or one named with
/// `-Pcn1.backend.webapp.bundle` -- and unpacks it with a compressed copy beside
/// each text file, through the same [BackendWebApp] the Maven goal runs.
///
/// `runBackend` then serves it without being told to, and `backendPackage`
/// leaves it beside the binary. It is a task of its own, and neither of those
/// depends on it, because the browser build takes about a minute and a server
/// edited and restarted all afternoon should not pay for a translation each
/// time. What it staged stays until `clean`.
@DisableCachingByDefault(because = "Unpacks the result of a build that is never cached")
public abstract class BackendWebAppTask extends DefaultTask {
    /// A browser build to stage instead of the application's own: the archive a
    /// JavaScript build produced, or a directory unpacked from it. A path, and
    /// not a tracked file, because it may be either.
    @Input
    @Optional
    public abstract Property<String> getBundle();

    /// The build directory of the application this backend belongs to, where
    /// its JavaScript build writes the bundle. Absent in a backend that is a
    /// project of its own.
    @Internal
    public abstract DirectoryProperty getAppBuildDirectory();

    /// The backend's directory, which a relative [getBundle()] is resolved in.
    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

    /// Where the application is staged. `runBackend` looks here.
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void stage() {
        String given = getBundle().getOrNull();
        File bundle = null;
        if (given != null && !given.trim().isEmpty()) {
            bundle = new File(given.trim());
            if (!bundle.isAbsolute()) {
                bundle = new File(getWorkingDirectory().get().getAsFile(), given.trim());
            }
        }
        File appBuild = getAppBuildDirectory().isPresent() ? getAppBuildDirectory().get().getAsFile() : null;
        try {
            stage(new GradleLog(getLogger()), bundle, appBuild, getOutputDirectory().get().getAsFile());
        } catch (BuildFailureException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /// Stages `bundle`, or with none the bundle the application's JavaScript
    /// build left in `appBuildDirectory`, into `output`.
    ///
    /// @return the number of files staged
    static int stage(Log log, File bundle, File appBuildDirectory, File output)
            throws BuildFailureException, BuildExecutionException {
        File source = bundle;
        if (source == null) {
            if (appBuildDirectory == null) {
                throw new BuildFailureException("backendWebApp builds the application this backend belongs "
                        + "to, and this backend is a project of its own. Pass a bundle that is already "
                        + "built with -Pcn1.backend.webapp.bundle=<zip or dir>.");
            }
            source = BackendWebApp.locateBundleIn(appBuildDirectory);
            if (source == null) {
                throw new BuildFailureException("The browser build left no bundle in " + appBuildDirectory
                        + ". A project whose browser build is made elsewhere can stage it with "
                        + "-Pcn1.backend.webapp.bundle=<zip or dir>.");
            }
        }
        return new BackendWebApp(log).stage(source, output);
    }
}
