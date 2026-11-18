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
import com.codename1.maven.BackendEngines;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/// Judges the database engine a backend declares (`cn1.backend.database`) and
/// writes it where the JVM runtime reads it, as the Maven module's
/// process-annotations execution does.
///
/// A task of its own, with a directory of its own that is an output of the main
/// source set, and not an action of a compiler: a backend written in Kotlin
/// alone never runs `compileJava`'s actions, and one written in both languages
/// would get the file in both classes directories, which is the very thing
/// `SplitOutputCheck` refuses. Being part of the source set's output, it runs
/// before anything that reads the runtime classpath -- `runBackend`, the
/// module's tests, an IDE run -- so none of them starts a server whose engine
/// nobody named.
@DisableCachingByDefault(because = "Writing one line is cheaper than a cache lookup")
public abstract class DeclareBackendDatabaseTask extends DefaultTask {

    /// The value of `cn1.backend.database`. Optional to Gradle so that the
    /// missing one is reported by [BackendEngines#require], which names the
    /// property and the engines, and not as an unset task input.
    @Input
    @Optional
    public abstract Property<String> getDatabase();

    /// Holds the declaration and nothing else.
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void declare() {
        try {
            // Whether SQLite is linked is a question for the native package;
            // the JVM always has it.
            BackendEngines.declare(getOutputDirectory().get().getAsFile(),
                    BackendEngines.require(getDatabase().getOrNull(), true));
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
    }
}
